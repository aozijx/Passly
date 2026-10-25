# 包边界

## 职责表

| 包          | 可以包含                                      | 不应包含                                  |
|------------|-------------------------------------------|---------------------------------------|
| `core`     | Android 平台适配、权限、共享 Compose UI、密码学与遥测能力          | DAO、Repository 实现、业务页面                |
| `domain`   | 领域模型、端口、策略、验证及设置契约                    | `android.*`、资源 ID、Entity、Feature UI 状态 |
| `data`     | Room、Proto DataStore、Mapper、Repository 实现 | Compose 页面、业务流程、备份协议与格式              |
| `core.crypto` | 无状态密码学原语、算法实现、加密格式                    | 认证流程、持久化、Android Keystore          |
| `security` | Android Keystore、认证方式、授权与安全会话编排                 | Room Entity、DataStore 具体实现、Feature UI |
| `feature`  | app 内的业务流程、平台入口、能力合同与策略 | Presentation 页面、ViewModel、Data 实现 |
| `app` 适配器 | Android 入口、DI 与跨模块实现连接；Backup 快照可读取 Data | 页面状态、领域规则真相源 |
| `presentation.feature` | 对应业务的 Route、ViewModel、UI state/effect、Compose UI | DAO、Entity、Data Repository 实现 |
| `service`  | Android Service 入口与系统生命周期桥接               | 独立业务真相源                               |

真实模块还要求源码命名空间与职责一致：`:data` 只包含 `com.aozijx.passly.data.*`。`:core:common` 保持为
Domain 可依赖的纯 Kotlin 错误与通用能力；密码学、遥测、共享 Android 与 Compose 能力合并在 `:core`。
DEK、字段派生密钥和信封原语当前位于 `:core` 的 `security.dek/security.envelope` 包；Android Keystore、
锁状态和认证编排归独立的 `:security`。应用消息调度与全局框架桥归 `:app`。

## 强制规则

```text
domain -X-> data | feature | android.*
core   -X-> data repository implementation
feature-X-> presentation | data implementation
security-X-> data implementation
app feature -X-> 任意散落的 data implementation import
```

`:security` 可以依赖 Domain 的认证/会话端口、`:core` 的密码学能力和 `:runtime:session` 的资源无关状态机，
但不能依赖 Room、Data Repository 实现、Compose 页面或 Feature 状态。Feature 只能通过 Domain 端口请求认证、
授权和锁定，不直接导入 `security.authentication`、Keystore 或密钥实现。

App 通过 Gradle 依赖 `:data` 让 Hilt 聚合实现。Backup 的 `BackupSnapshotGateway` 是能力侧合同；
`app/database/backup` 的 Room Reader、Restorer、恢复文件事务、Gateway 实现和 Hilt binding 负责与 Data 集成。
稳定键映射器位于 `feature/backup/internal/archive/mapper`，只依赖 Domain 与归档模型。
`SourceBoundaryPolicy` 对全部 `feature` 禁止 Data 实现 import，不为 Backup 另开例外。
Autofill 等 Android Service 需要打开页面时，由 Feature 中的启动契约与 `app` 适配器衔接，Feature 不引用 Activity 类。

分页应通过纯 Kotlin 契约传递；Android Paging 可留在 Data/UI 适配边界。Feature 需要条目数据时依赖
Entry Domain 端口，不直接构造 Entity 或具体 Repository。Autofill/Credential Manager 的平台流程和候选编排
留在 App；只有可复用的条目匹配语义进入 Entry 领域。

## 模型归属

- Entity：数据库结构，只在 Data 层。
- Domain model：业务语义与跨层契约。
- UI model/state：展示派生状态，只在 Feature presentation/UI。
- 设置策略枚举若展示语义与 Domain 完全相同，Feature UI 可直接使用 Domain 类型；只有展示结构或语义确实不同才增加 UI 映射。
- 应用偏好：设置契约模型与端口（`domain/settings`）在 Domain 统一表达；Proto 存储 DTO 与
  Repository 实现在 Data（`data/local/datastore`、`data/repository/settings`）。通知事件模型
  （`AppNotice`）属于 App 层，通知设置值属于 Domain 设置模型。
- 外部格式 model：备份模型归 `app/feature/backup/internal/archive`；Proto 或系统 API 模型放在各自适配边界。

Mapper 应显式处理缺失字段、版本和错误，不用强制类型转换掩盖边界问题。

## 自动检查建议

架构门禁应扫描 import，并至少禁止：

```text
domain -> com.aozijx.passly.data
domain -> com.aozijx.passly.feature
feature -> *.data.local.database.entity / *.data.local.database.dao
security -> *.data.repository / *.data.local
```

应用内备份的数据库快照适配器是已知例外；允许清单必须精确到文件或接口并附理由，不能通过全局排除关闭检查。

