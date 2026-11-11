# Vault 功能总览

状态：当前实现。

Vault 管理本地凭据条目、搜索与排序、创建/编辑、敏感字段访问、动态口令、修订历史和条目关系。本文按用户工作流组织入口；Room、Domain、Security 与 Runtime 的共享合同仍由平台文档拥有。

## 功能路径

| 工作流 | 权威专题 |
| --- | --- |
| 浏览、分页、检索、筛选、排序 | [列表、搜索与排序](list-and-search.md) |
| 创建、逐字段编辑、完整表单替换和保存 | [条目编辑器与写入架构](entry-editor-and-persistence.md)、[完整表单编辑](full-form-editing.md) |
| 修订快照、活动记录、差异与恢复 | [条目修订历史](entry-revision-history.md) |
| OTP 创建、URI 导入导出、运行时验证码与复制 | [OTP 创建](otp-entry.md)、[OTP URI](otp-uri.md)、[OTP 运行时](otp-runtime.md) |
| 卡片资料、卡片样式 | [银行卡资料](card-entry.md)、[卡片样式](card-styles.md) |
| 条目二维码展示 | [条目二维码](qr-display.md) |

## 跨层协作

- Route/Activity 持有宿主生命周期、导航、权限、Activity Result 和系统 effect；ViewModel 持有页面状态与异步任务，向 UI 暴露只读可观察状态；Content 渲染状态并上送用户动作。
- ViewModel 经 Domain Repository/能力 port 读取或写入。完整创建、授权或多 port 编排由对应 App UseCase 持有；简单操作不机械增加转发层。
- Domain 拥有 Entry、Credential、查询与命令合同，以及跨入口验证/物化规则；Data 实现持久化 port、Room 事务和映射。页面草稿及 UI 投影不取代 Repository 持有的持久真相。
- Security 签发/验证敏感字段访问许可并管理密钥生命周期；Data 在授权与数据库 session/lease 边界内读取数据，敏感值只在获准路径中揭示。
- 长期观察 Flow 的失败、重新订阅和锁定撤权语义由其消费 owner 处理。写事务的 durable commit 与提交后清理分别遵守数据合同。

层间通信、错误/取消、Session 与 lease 的通用要求见[层间契约与通信](../../platform/architecture/layer-interactions.md)；ViewModel/Route owner 规则见[ViewModel 依赖与 owner 边界](../../platform/architecture/viewmodel-ownership.md)，状态管理见[Presentation 状态管理](../../engineering/development/architecture-and-state.md)。

## 代码位置

| 职责 | 代表类型 | 源码 |
| --- | --- | --- |
| Vault 页面状态与列表查询 | `VaultViewModel`、`VaultRoute` | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt)、[`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt) |
| 领域条目读取/分页/创建合同 | `EntryQueryRepository`、`EntryPagingRepository`、`EntryCreation` | [`EntryQueryRepository.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryQueryRepository.kt)、[`EntryPagingRepository.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryPagingRepository.kt)、[`EntryCreation.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryCreation.kt) |
| 加密数据库 session 边界 | `AppDatabaseSession` | [`AppDatabaseSession.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt) |
