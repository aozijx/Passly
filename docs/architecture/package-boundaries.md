# 包边界

## 职责表

| 包          | 可以包含                                      | 不应包含                                  |
|------------|-------------------------------------------|---------------------------------------|
| `core`     | Android 平台适配、权限、共享 Compose UI、密码学与遥测能力          | DAO、Repository 实现、业务页面                |
| `domain`   | 领域模型、端口、策略、验证                           | `android.*`、资源 ID、Entity、Feature/设置类型 |
| `data`     | Room、Proto DataStore、Mapper、Repository 实现 | Compose 页面、业务流程、备份协议与格式              |
| `core.crypto` | 无状态密码学原语、算法实现、加密格式                    | 认证流程、持久化、Android Keystore          |
| `security` | DEK/派生密钥生命周期、信封、Keystore、锁与认证编排            | Room Entity、DataStore 具体实现、Feature UI |
| `feature`  | app 内的导航、业务流程、ViewModel、UI state/effect、UI | 无边界地访问 DAO、Entity、具体 Repository 实现 |
| `service`  | Android Service 入口与系统生命周期桥接               | 独立业务真相源                               |

真实模块还要求源码命名空间与职责一致：`:data` 只包含 `com.aozijx.passly.data.*`。`:core:common` 保持为
Domain 可依赖的纯 Kotlin 错误与通用能力；密码学、遥测、共享 Android 与 Compose 能力合并在 `:core`。
Android Keystore、锁状态和认证编排归 `:app/security`，应用消息调度与全局框架桥归 `:app`。

## 强制规则

```text
domain -X-> data | feature | android.*
core   -X-> data repository implementation
feature-X-> entity | dao
security-X-> data implementation
app feature -X-> 任意散落的 data implementation import
```

App 通过 Gradle 依赖 `:data` 让 Hilt 聚合实现。普通 feature 只消费 Domain/Core 契约；Backup 因为需要生成和
恢复全量数据库快照，允许在 `feature/backup/internal/archive/snapshot` 内集中访问 Data 类型。该例外必须保持
在一个目录中，不能扩散到 UI、ViewModel 或其他 feature。

分页应通过纯 Kotlin 契约传递；Android Paging 可留在 Data/UI 适配边界。Feature 需要条目数据时依赖
Entry Domain 端口，不直接构造 Entity 或具体 Repository。Autofill/Credential Manager 的平台流程和候选编排
留在 App；只有可复用的条目匹配语义进入 Entry 领域。

## 模型归属

- Entity：数据库结构，只在 Data 层。
- Domain model：业务语义与跨层契约。
- UI model/state：展示派生状态，只在 Feature presentation/UI。
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

