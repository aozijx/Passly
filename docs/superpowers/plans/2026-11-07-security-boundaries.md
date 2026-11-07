# Security 边界收敛计划

## 目标

保留 `:security` 单一 Gradle 模块；将认证、密钥生命周期与会话协调按职责归包；让业务调用依赖 Domain 端口，Core 仅持有通用密码学能力合同；迁移完成后立即删除旧路径与旧测试支架。

## 边界决定

- `:security` 不拆子模块。认证流程、认证方式、平台宿主、会话、授权、锁定、DEK 和信封分别归入职责包。
- App/Feature 的认证和会话业务调用使用 Domain 端口。App Hilt 组合根在 Gradle 图中依赖 Security 以装配实现。
- 只允许 `PasslyApp` 与 `AutofillAuthActivity` 组合 Security 的 `AuthenticationHost`。它是 Activity/Compose/BiometricPrompt 平台宿主，不把 Android API 塞进纯 Kotlin Domain。
- Data 生产代码通过 Core 的 `SensitiveFieldCryptor` 和 `AttachmentContentProtector` 能力合同，不依赖 Security。Data AndroidTest 用本地最小 fake，不增加 Security 测试依赖。
- Security 内的 DEK 安装/撤销等密钥所有权过程继续留在 Security；不为同模块实现互调新增一一转发层或把 `OwnedBytes` 等密钥实现类型塞入 Domain。
- 不保留内部兼容双轨。每个调用点完成切换后，立即移除旧路由、状态、桥接及只服务旧路的测试/文档。

## 实施记录

### 1. 移动密钥生命周期实现

- [x] 将 Core 下的 `security.dek` 与 `security.envelope` 实现及行为测试迁入 Security。
- [x] Core 保留通用 AES/KDF/内存工具与 Data 能力合同；Security 绑定附件与敏感字段加密实现。
- [x] Data 使用 Core 能力接口；移除 `decryptProvisioned`；Data AndroidTest 改为局部 fake。
- [x] 清除 Core 主源码中的 Security 实现命名空间，保持密文、信封和持久化格式不变。

### 2. 整理认证与会话职责

- [x] 认证请求与流程放入 `authentication.flow`，凭据和生物识别执行放入 `authentication.method.*`。
- [x] Android 认证宿主保留在 `authentication.host`；会话协调归 `session`；授权与锁定保持分开。
- [x] 更新 Security 内部调用及测试路径，将实现类型收窄为 `internal`；唯一公开的 Security 源码入口是平台 `AuthenticationHost`。
- [x] 将 `KdfOperation` 收窄为 `internal`，不让 Security 的 KDF 执行细节成为跨模块 API。

### 3. 通过 Domain 和 Core 合同使用能力

- [x] Unlock、设置、恢复流程使用 Domain 的认证/凭据端口；App 主源码仅两个平台宿主组合点导入 `AuthenticationHost`。
- [x] 扩展 SourceBoundaryPolicy：App 主源码禁止导入 Security 实现，仅允许两个平台宿主文件导入认证宿主包；Core/Data 主源码禁止依赖 Security 实现。
- [x] 保持技术能力语义在 Core：敏感字段和附件密码学合同留在 Core，不把存储加密细节移入 Domain。
- [x] 扫描确认没有 Security 内部兼容入口、`decryptProvisioned` 调用点或 Data 到 Security 的生产引用。

### 4. 同步文档和验证

- [x] 更新包边界、架构总览、认证与密钥管理文档，并记录为何 Android 宿主不属于 Domain。
- [x] 更新冗余清理 skill：每个独立迁移点结束即扫描调用方并清理旧路径，不积攒到任务末尾。
- [x] `buildSrc test` 通过：128 项、8 个 XML 测试类，无失败或错误。
- [x] Security 单测通过：174 项、33 个 XML 测试类；Core 单测通过：71 项、18 个 XML 测试类。
- [x] Data 单测与 AndroidTest Kotlin 源集编译、App Kotlin 编译、`verifyArchitecture` 通过；部分编译任务为 `UP-TO-DATE`。
- [x] `git diff --check` 通过。未运行设备端仪器测试。

## 工作区和提交

工作区同时包含其他功能与文档的既有未提交改动，部分文件与本次边界规则共享同一文件。未暂存或提交；后续若提交，需只包含确认属于本次 Security 迁移的精确改动，并排除 `AGENTS.md` 与 `.agents/skills/`。
