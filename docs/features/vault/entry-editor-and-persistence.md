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
只接受 `EntryDraftTarget.New`。它先检查当前权限，再检查必填字段和 `invalidProvidedFields`，生成身份后物化、
执行完整领域校验并写入 Repository。Autofill 创建复用同一纯规则，但使用自己的会话访问检查和写入端口。

[`EntryDraftMaterializer`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryDraftMaterializer.kt)
位于 Domain，目前显式转换四种创建类型。它再次检查草稿字段及身份类型；不能假定草稿一定经 `withValue()` 构造。
无效草稿以字段键表达失败，不能把用户输入拼入异常。`EntryValidation` 仍负责最终完整凭据校验。

`EntryDraftTarget.Existing` 目前只有模型定义，没有通用更新消费者；不能据此宣称已有完整表单编辑能力。
详情编辑不经过该目标模型。

## 四类创建表单

| 类型 | 状态与映射 | 专属规则 |
| --- | --- | --- |
| Login | `AddPasswordFormState` / `AddPasswordUiState`、`PasswordFormMapper.kt` | 网站写入 `PRIMARY_URL`，应用写入 `APPLICATION_IDS`；密码保留原值 |
| OTP | `OtpFormState` / `AddOtpCodeState`、`OtpFormMapper.kt` | `OtpConfigInput` 与扫码配置共用 Domain 校验 |
| Bank Card | `AddBankCardFormState` / `AddBankCardUiState`、`BankCardFormMapper.kt` | 卡号、有效期和可选银行资料使用各自规则 |
| Wi-Fi | `AddWifiFormState` / `AddWifiUiState`、`WifiFormMapper.kt` | 网络名、安全类型、密码及隐藏标记一起转换；扫码种子只初始化一次 |

每类 ViewModel 暴露不可变 `uiState`，拥有自己的 `Idle / Saving` 阶段与保存逻辑。
保存取点击时的表单快照，期间拒绝表单更新和重复保存；成功清空表单并发送 `EditorSaveEffect.Saved`，
失败回到可重试状态。当前成功 Effect 不携带条目 ID，不能按尚不存在的 `Saved(entryId)` 合同调用。

Route 按生命周期收集状态；Screen 接收状态和 Action。`EditorSaveEffectHandler` 消费保存结果并调用宿主回调或显示失败提示。
焦点、动画和展开状态留在 Compose；字段值、扫码结果和保存状态回到表单状态。敏感输入不进入导航参数或 SavedState。

四个 ViewModel 都观察认证状态并清空表单，但尚未使用统一的创建会话状态机。保存任务的持有、取消与迟到结果隔离，
以及创建页特有异步任务的生命周期，仍需逐条核对，不能把“表单已清空”当作“所有任务已取消”。

专属细节见[银行卡资料与有效期](card-entry.md)、[OTP 创建](otp-entry.md)及[Wi-Fi / OTP 二维码](qr-display.md)。

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

- 创建页仍重复保存、失败恢复和认证清理逻辑；没有 `EntryEditorSession`、`EntryFormAdapter`、`EntryEditorRegistry` 或 `EntryMaterializerRegistry` 实现。
- 集中式物化器目前只有四个创建分支；需要扩展类型时按实际语义调整，不把每个分支强制包装成空转发类。
- 完整表单编辑与 `Existing` 草稿更新尚未实现。

验证以现有测试为准：`CreateEntryUseCaseTest`、四类表单映射及 ViewModel 测试覆盖创建入口；Domain 的
`EntryDraftMaterializerTest`、`EntryEditTest`、`EntryFieldEditorTest` 覆盖纯规则；`EditDetailEntryUseCaseTest` 和
详情状态测试覆盖逐项更新与发布边界。它们不证明未实现的统一编辑器、所有类型能力或真实设备交互已经完成。
`VaultListUiMapperTest` 与 `VaultNavigationTest` 检查创建选项均有目的地、筛选能力独立保留；
`AddEntryBottomSheetTest` 提供按传入选项渲染和转发选择的仪器用例，编译通过不等于设备运行通过。
修改实现时按[测试要求](../../development/testing.md)运行相关模块、App 门禁及所需仪器测试。
