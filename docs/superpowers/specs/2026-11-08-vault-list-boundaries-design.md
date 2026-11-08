# Vault 页面内部边界与旧路径清理设计

## 意图

整理 Vault 页面相关代码的内部职责，降低列表、详情、编辑、回收站以及页面交接之间的耦合，让后续整理条目和多选操作能在明确 owner 上扩展。

本轮只重构现有行为；不新增用户可见功能。尤其不提前实现选择模式、批量移动/删除或整理规则。

## 当前证据

Vault Presentation 目前包含 list、detail、editor、trash、navigation 等独立子树。Detail ViewModel 保留条目状态和多条详情工作流的协调，同时由 `DetailPasswordRelationsWorkflow`、`DetailWorkSession`、reveal stores、`DetailFaviconSession`、OTP runtime 与 `DetailQrSession` 分担关系绑定、会话有效性、敏感值、favicon、OTP 和 QR 生命周期。修订历史的订阅代次、操作取消、历史敏感 reveal、比较与恢复由 `DetailRevisionWorkflow` 持有；Detail ViewModel 只保留修订状态投影及恢复提交后的当前条目快照/OTP 刷新。Vault ViewModel 拥有列表状态、查询恢复读取、Paging 与 OTP runtime；复制/删除命令、删除互斥及命令结果 effect 由页面作用域的 `VaultEntryCommandWorkflow` 持有。四种编辑 ViewModel 保留各自表单不变量，同时共用 `EditEntryUseCase`、`EditorWriteSession`、加载失败 effect handler 和一次性 seed access 检查。回收站有独立订阅和写操作状态机。Route/Graph 承接导航、页面生命周期、一次性 seed 消费与平台 effect。

目标是沿真实状态、协程、生命周期、授权与失败合同减少页面 owner 之间的耦合，保持 Route 的宿主职责、Content 的被动渲染职责，以及类型专属表单/详情合同。执行中核对所有 Vault 页面和交接点；只迁移有清晰独立 owner 且能实际减少原 owner 职责/依赖的工作流。每次替换都迁移当前所有调用方并删除已无消费者的旧路径、兼容分支、专属测试和过期文档。

## 范围

- Vault 页面 Presentation 全面审查：主列表及其查询/显示配置、详情及敏感值/修订/favicon/QR/关联密码工作流、四种编辑器、回收站、导航/seed/扫码导入交接，以及 Route 到 Screen/Content 的职责。
- 对相关 ViewModel、Reducer、Session/Workflow、UI model/action、Route/Graph、DI、测试、正式 Vault 文档和维护计划做 caller 闭包审计；跨 Domain/Data 仅在真实业务 owner 错置或失效兼容路径时触及。
- 未来的多选/整理操作只作为可扩展边界要求，不实现选择状态、批量命令或产品规则。
- 清理范围仅限本次切换中证明确认没有独立合同或调用方的旧逻辑；不因名称或行数删除代码。

不新增产品能力，不改变已发布或持久化合同；不为整齐而将整个 Vault 强行抽成新的 Gradle module。Scanner 内部实现和其他非 Vault 页面不做普遍重构；只审查它们与 Vault 的真实交接。若遇到需要改变外部/持久化合同的方案，保留具体合同并单独说明，不套用内部调用的兼容默认。

## 成功标准

- Vault list、detail、editor、trash 和 navigation/seed 交接的状态、任务、资源与 effect 各有可说明的 owner；没有仅转发参数的一一包装层。
- Route 只保留宿主生命周期、导航/平台 effect 和必要的映射；被动 UI 不获取 ViewModel。类型专属编辑规则仍由对应表单 owner/Domain 合同负责。
- 删除/恢复/永久清除、复制/剪贴板交付、敏感 reveal 与擦除、OTP/QR、favicon 资源、页面订阅/重试、seed 消费和锁定取消仍遵守当前合同；旧结果不能在 entry/auth/session/page owner 失效后产生副作用。
- 内部调用只有一条有效路径；没有为内部调用方保留未经证实的兼容 API。
- 行为级验证覆盖异步 owner、失败/取消、迟到结果和资源收尾；相关正式文档与实际代码一致。

## 不可破坏的合同

- `EntryListQuery` 和 Data/Paging 仍是列表查询事实来源。当前 `buildRecoverableVaultQueryStates()` 与 `switchRecoverableQueryGenerations()` 是已清理后的唯一有效查询路径；不得恢复旧查询 helper 或全量刷新桥。收藏/排序/筛选/层级参数、250ms 搜索防抖、按实际 `EntryListQuery` 去重、设置读取 Loading/Ready/Failed 与显式重试保持不变。
- 编辑仍使用 `EntryDraft`/`EntryDraftMaterializer`、`EntryEdit`/预期版本和一次性 seed 合同；各类型表单校验和字段差异不得被通用化抹平。
- 删除仍使用 `TrashCommandUseCase` 并要求完整安全会话。复制、历史 reveal/restore、QR、favicon promote/delete、标签更新及关联密码 binding 保留各自现有授权、版本/commit、owner 和资源清理合同。
- `Job.cancel()` 不能替代任务退出；取消和 fatal `Error` 不映射成普通失败。状态 owner、剪贴板写入、敏感值擦除、Room/附件/icon 资源清理及 durable commit 结果的合同不得弱化。
- 当前用户本地修改和 index 状态均保留；不暂存、不提交。

## 方案比较与选择

1. **完整审查并分批调整 Vault 页面边界（采用）**：覆盖 list/detail/editor/trash/navigation 与页面交接；执行中用真实 caller 和 task/state/resource owner 选择迁移，逐批完成旧路由清理。它保留完整目标，同时每批可独立验证。
2. **只深改列表和详情**：实现了两个复杂 owner 的迁移，但会留下四种编辑器、回收站与导航交接的耦合，不能满足用户要求的 Vault 页面整体可维护性目标。
3. **全量新建 Gradle feature/module 并迁移 Vault**：形式边界更强，但当前 package policy 已将 app 内业务 Feature 放在 `app/feature/vault`，实际证据并未证明编译隔离可覆盖配置/依赖成本；不作为默认方案。

## 实现决策留给计划

获批后按独立流程的依赖关系制定多阶段计划：先确认全量页面/业务动作清单及当前消费者，再选择最能降低共享耦合且可独立验证的切片。不得预设一定新增 ViewModel/handler/UseCase 或机械拆文件。若拆分不能减少原 owner 的职责或依赖，则保留原结构并记录 retain/defer 证据。每个迁移切片都完成新旧调用方迁移、主源码/测试/DI/导航/资源引用闭包扫描、旧路径删除与行为验证，再继续下一切片。对证明正确且有独立合同的结构记录保留理由；对暂缓项保留原始目标并说明依赖或阻塞，不能把它们重新定义成完成。

## 验证

依据具体切片运行相关 Vault ViewModel/Reducer/Session/UseCase/Route/Compose 行为验证；验收完整修改时覆盖 list、detail、all editors、trash、navigation/seed、DI、正式文档和残余生产调用引用。检查需要的 AndroidTest 编译与适用 UI 行为，运行 `verifyArchitecture`，最后运行 `git diff --check`。Gradle 串行运行并限制为 `--no-parallel --max-workers=2`。仪器测试无法运行时明确报告缺口。
