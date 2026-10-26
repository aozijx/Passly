# 条目编辑器与写入架构

状态：当前实现。本文记录生产代码的职责与已知缺口，不把未实现的接口当作依赖边界。

## 创建与详情编辑

当前独立创建页支持 Login、OTP、Bank Card、Wi-Fi。已有条目的编辑由详情页按一个逻辑字段提交；
没有统一的完整条目编辑表单，也没有把创建表单直接用于整条更新。

| 层 | 当前职责 | 主要入口 |
| --- | --- | --- |
| Domain | 字段定义、草稿类型、纯校验、物化和单次编辑规则 | `EntryTypeDefinitions`、`EntryDraft`、`EntryDraftMaterializer`、`EntryEdit` / `EntryEditResult` |
| App feature | 会话检查、ID/时间生成、冲突处理、日志和 Repository 编排 | `CreateEntryUseCase`、`EditDetailEntryUseCase` |
| App presentation | 类型专属表单、状态、Action、ViewModel、Route 和 Compose 内容 | `feature/vault/presentation/editor/<类型>`、`presentation/detail` |
| Data | 事务、乐观锁、高敏字段保留/替换/清除、加密、修订和活动写入 | `UpdateEntryExecutor`、`SecretFieldStore`、`EntryRevisionWriter` |

Domain 不依赖 Android、导航或表单组件；物化器不生成身份、不访问 Repository。
Presentation 经用例或 Domain 端口访问数据，不直接使用 DAO。完整模块边界见[包与模块职责](../../architecture/package-boundaries.md)。

## 新建链路

```text
类型 Action -> ViewModel 的表单快照 -> toEntryDraft()
  -> CreateEntryUseCase -> EntryDraftMaterializer -> EntryValidation
  -> EntryCommandRepository.createEntry -> Data 事务
```

[`CreateEntryUseCase`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/CreateEntryUseCase.kt)
接收只含 `type` 和类型化字段值的创建草稿。它先检查当前权限，再检查必填字段和 `invalidProvidedFields`，生成身份后物化、
执行完整领域校验并写入 Repository。Autofill 创建复用同一纯规则，但使用自己的会话访问检查和写入端口。

[`EntryDraftMaterializer`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryDraftMaterializer.kt)
位于 Domain，目前显式转换四种创建类型。它再次检查草稿字段及身份类型；不能假定草稿一定经 `withValue()` 构造。
无效草稿以字段键表达失败，不能把用户输入拼入异常。`EntryValidation` 仍负责最终完整凭据校验。

草稿只描述待创建内容；身份与初始版本由创建用例生成。
已有条目通过 `EntryEdit` 与带预期版本的 Repository 更新合同修改，条目 ID 与版本由更新用例和端口参数表达。

## 四类创建表单

| 类型 | 状态与映射 | 专属规则 |
| --- | --- | --- |
| Login | `AddPasswordFormState` / `AddPasswordUiState`、`PasswordFormMapper.kt` | 网站写入 `PRIMARY_URL`，应用写入 `APPLICATION_IDS`；密码保留原值 |
| OTP | `OtpFormState` / `AddOtpCodeState`、`OtpFormMapper.kt` | `OtpConfigInput` 与扫码配置共用 Domain 校验 |
| Bank Card | `AddBankCardFormState` / `AddBankCardUiState`、`BankCardFormMapper.kt` | 卡号、有效期和可选银行资料使用各自规则 |
| Wi-Fi | `AddWifiFormState` / `AddWifiUiState`、`WifiFormMapper.kt` | 网络名原样保留并同时作为初始标题，安全类型、密码及隐藏标记一起转换；扫码种子只初始化一次 |

每类 ViewModel 暴露不可变 `uiState`，保留类型专属表单和 `Idle / Saving` 展示阶段。
共享的 `EditorSaveSession` 持有保存任务，驱动保存、失败恢复和清空回调；保存取点击时的表单快照，期间拒绝表单更新和重复保存。
成功清空表单并发送 `EditorSaveEffect.Saved`，失败回到可重试状态。当前成功 Effect 不携带条目 ID，不能按尚不存在的 `Saved(entryId)` 合同调用。

Route 按生命周期收集状态；Screen 接收状态和 Action。`EditorSaveEffectHandler` 消费保存结果并调用宿主回调或显示失败提示。
焦点、动画和展开状态留在 Compose；字段值、扫码结果和保存状态回到表单状态。敏感输入不进入导航参数或 SavedState。

`EditorSaveSession` 观察认证状态，将表单和保存尝试绑定到当前完整认证对象；锁定或认证归属变化先使尝试失效，再取消保存并清空表单。
迟到结果不能清空新表单或发出新保存效果。缓冲效果在消费时再次核对归属，新的编辑或重试会淘汰旧效果；只保留最近一次结果。
关闭 ViewModel 后协调器不能重新激活。取消继续传播，不生成失败效果；独立取消且认证仍有效时恢复可编辑状态。
取消或丢弃 UI 结果不撤销已经提交的数据库事务。类型专属的应用列表加载、扫码初始化与事件仍由对应 ViewModel 管理，不能把保存协调器当作整个表单的生命周期实现。

Login 的应用列表加载先经过编辑会话守卫，并在查询前后核对同一完整认证对象；查询返回后继续检查协程取消状态。
清空表单会取消持有的目录请求，迟到结果不能重建已清空缓存或覆盖新请求。正常加载失败仍保持可重试状态。

专属细节见[银行卡资料与有效期](card-entry.md)、[OTP 创建](otp-entry.md)及[Wi-Fi / OTP 二维码](qr-display.md)。

密码、Wi-Fi 密码及银行卡的卡号、付款 PIN、CVV 显隐使用同一
`SensitiveFieldVisibilityButton`。控件采用 M3 `IconToggleButton`，以中英文资源表达“显示/隐藏某字段”、
当前可见状态和选中状态；只接收字段标签资源，不接收凭据值。可见性仍由各自表单及 Action 管理，
控件不缓存密码或另存一份可见状态。文本框的遮罩和输入内容继续由页面状态驱动。

## 已有条目的逐项编辑

[`EditDetailEntryUseCase`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/EditDetailEntryUseCase.kt)
在互斥区内读取最新条目，经 Domain `EntryEdit.applyTo()` 得到类型化结果，再提交 `EntryUpdate` 和预期版本。
无变化不写入；版本冲突最多重新读取并重试一次。失败返回明确错误，不把缺失字段补成默认凭据。

`EntryFieldEditor` 只校验本次替换字段，保留其他未揭示字段；不能对删去高敏值的详情模型执行整条创建校验。
Data 根据真实高敏行键维护能力标记，按 Preserve / Clear / Replace 合同处理敏感字段。
映射与存储边界见[数据映射](../../data/mapping.md)。

详情异步结果通过 `DetailWorkSession` 的独立访问凭据发布。内联草稿在提交后发生修改或重开时，旧完成保留当前编辑；
标签提交期间冻结草稿。图标资源另由 `DetailFaviconSession` 与引用检查回收端口管理，不能因页面关闭直接删除已提交文件。
这些都是现有详情机制，不代表四类创建页已经共享统一 Session。

## 创建入口与能力边界

[`VaultEditorDestinationRegistry`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultEditorDestinationRegistry.kt)
目前只映射 Password、TOTP、Bank Card、Wi-Fi 四个导航目的地。不支持的请求返回 `UnsupportedDestination`，不回退到 Login。
注册表从目的地映射派生 `creatableTypes`，由列表 UI model 传入 FAB 和创建弹层；快捷项也与该列表取交集。
组件只渲染传入的创建选项，不自行遍历类型枚举。筛选继续使用独立的全量选项，读取、导入支持的类型不等于可创建类型。
不支持目的地的请求不会进入备用表单；旧的未渲染 `addType` 状态与分支已移除。

新增可创建类型应同时核对字段定义、强类型表单与映射、Domain 物化和校验、事务存储、详情读取及备份往返；
声明 Autofill、扫码等能力时，再验证对应集成。增加注册表只能解决实际的路由或组合需求，不能替代能力闭环。

## 当前缺口与验证

- 创建页已共享保存协调器，表单更新、校验和类型专属异步能力仍由类型 ViewModel 持有；没有泛型 `EntryEditorSession`、`EntryFormAdapter`、`EntryEditorRegistry` 或 `EntryMaterializerRegistry` 实现。
- 集中式物化器目前只有四个创建分支；需要扩展类型时按实际语义调整，不把每个分支强制包装成空转发类。
- 完整条目表单编辑尚未实现；当前更新入口仍是详情逐项编辑，不提供另一套草稿更新分支。

验证以现有测试为准：`CreateEntryUseCaseTest`、四类表单映射及 ViewModel 测试覆盖创建入口；Domain 的
`EntryDraftMaterializerTest`、`EntryEditTest`、`EntryFieldEditorTest` 覆盖纯规则；`EditDetailEntryUseCaseTest` 和
详情状态测试覆盖逐项更新与发布边界。它们不证明未实现的统一编辑器、所有类型能力或真实设备交互已经完成。
`EditorSaveSessionTest` 覆盖保存去重、锁定与重新认证、迟到结果、缓冲效果淘汰、取消和销毁边界；Wi-Fi 创建测试通过实际用例复现锁定期间的保存窗口。
`VaultListUiMapperTest` 与 `VaultNavigationTest` 检查创建选项均有目的地、筛选能力独立保留；
`AddEntryBottomSheetTest` 提供按传入选项渲染和转发选择的仪器用例，编译通过不等于设备运行通过。
修改实现时按[测试要求](../../development/testing.md)运行相关模块、App 门禁及所需仪器测试。
