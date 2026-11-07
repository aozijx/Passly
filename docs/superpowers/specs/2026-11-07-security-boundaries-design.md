# Security 模块边界与重构设计

## 意图

收窄 `:security` 的公开接口，减少调用方接触认证实现、密钥状态和内部参数的机会；整理认证相关代码与通用密码学能力的归属，降低 Security、Core、Domain、Data 和 App 之间的耦合。内部重构按一次性切换迁移当前调用方，并在每项迁移后删除已失效的旧入口、状态和测试支架；只有查明外部消费者或受保护合同后才保留旧行为，并记录证据。

## 目标

- `:security` 拥有认证执行、Vault/DEK/敏感字段/附件密钥生命周期、Android Keystore、授权、锁定与安全会话生命周期编排。
- 认证实现继续作为 `:security` 内的 `authentication` 子域，不另拆 Gradle 模块；认证合同/模型可以留在 Domain，具体验证和编排由 Security 实现。
- `:core` 拥有无状态 AES、Argon2、随机数与敏感内存处理实现，以及供 Data 消费的加密能力合同；Security 实现受认证和 Vault 会话控制的密钥使用策略。
- `:domain` 拥有纯 Kotlin 认证合同、认证模型及应用密码业务规则。通用强度评估保留为纯规则；UI 展示状态和资源映射不进入 Security。
- Unlock 保留为主 App 的 `app/feature/unlock` 页面入口，本轮不创建 `:feature:unlock` Gradle 模块，也不删除 Unlock 页面。它是用户进入已锁应用时看到的认证界面，不是独立安全引擎或第二套数据库解锁流程。
- 业务 Feature 只依赖 Domain 端口；Core/Security 是这些合同的技术实现，绑定由 App 组合根发现，不把实现类型注入业务代码。
- App 负责导航、主题、Activity/应用组合和 DI 连接。`AuthenticationHost` 是承载 FragmentActivity、Compose 生命周期和 Android BiometricPrompt 的平台 UI 适配入口，只供 App 根组合与 Autofill Activity 使用；Domain 保持纯 Kotlin，不复制这类 Android API。
- 除上述平台宿主适配外，App 与业务 Feature 不直接引用 Security 实现；Security 内部实现按需使用 `internal`，不为内部调用方保留兼容路由。

## 非目标

- 不把 Backup 改造、Autofill 收口或 `:runtime:session` 状态机迁移混入本轮 Security 改造。
- 不把 Vault 业务规则或 Vault UI 搬进 Security。
- 不因包名含 `security` 或 `crypto` 而机械搬迁代码；以模块依赖和实际职责为准。
- 不在本轮拆分 Unlock Gradle 模块。

## 当前状态与约束

- `:security` 依赖 `:core`、`:domain`、`:runtime:session`，没有依赖 App 或 Vault。
- Android Keystore、认证执行器、认证会话管理器、授权实现和 `AuthenticationHost` 位于 `:security`。
- `VaultSessionController` 由 Security 在认证成功后调用 `DatabaseSessionLifecycle`；该抽象属于 `:runtime:session`，实际 `AppDatabaseSession` 在 Data 中实现并调用 `RuntimeSessionManager` 管理资源开关。
- `:core` 当前包含 `KeyDerivation`、AES 原语，也物理包含了 `security.dek` 与 `security.envelope` 实现。扫描调用链确认这些密钥/信封类型只由 Security 编排和少量 Data 集成测试直接构造；生产 Data 只通过敏感字段加密能力工作。
- Data 生产代码直接依赖 `SensitiveFieldEncryptor` 实现。将其替换为 `:core` 中仅含 `encrypt`/`decrypt` 的 `SensitiveFieldCryptor` 合同，由 Security 提供实现和 Hilt 绑定；不将密文格式或密钥管理模型放入 Domain。
- `AttachmentContentProtector` 是 Data 使用的能力合同，留在 Core；其 `AttachmentContentCrypto` 与 `AttachmentDataKeyManager` 依赖 Vault DEK，迁入 Security，并将绑定移至 Security。
- 日志加密密钥（Data 的 `EncryptedLogFileManager`）和遥测标识散列密钥（App 的 `AndroidKeystoreIdentifierHasher`）拥有独立于 Vault/认证会话的生命周期，继续由各自的日志/隐私 owner 管理；本次不按 `AndroidKeyStore` 字样搬迁。
- Unlock 当前位于 App Feature 目录，拥有 ViewModel、Reducer、Route 与认证页面；流程还包括生物识别、恢复码及设置应用密码，因此不删除其业务入口。
- 工作区有大量其他模块的未提交修改。本轮只编辑明确纳入 Security 设计的文件，保留其余现有改动。

## 备选方案

1. **将 Unlock 保持为 `app/feature/unlock`，不拆 Gradle 模块（选定）**：改动范围较小，保留现有 Feature 所有权；接口和源码边界由公开 API、可见性及边界检查约束。以后只有在需要编译隔离或独立复用时再升为模块。
2. **新建 `:feature:unlock`**：编译期隔离更强，但需要迁移 UI 依赖、资源、Hilt 和导航；当前 Unlock 只由主 App 使用，收益尚不足以覆盖迁移成本。
3. **删除 Unlock Feature 并将认证并入根导航/通用弹窗**：减少一个目录，但会把恢复码、凭据设置和解锁状态挤入根壳层或通用认证宿主，职责更难维护，不采用。

认证能力另有两种模块边界：继续作为 `:security` 内的 `authentication` 子域，或新建 `:security:authentication` Gradle 模块。采用前者：认证还需要协调 Keystore、DEK、授权、锁定与 Runtime Session，没有证据表明它能独立形成依赖更少、由多方单独复用的模块；包级整理和窄 API 已能先解决当前耦合。

另一个边界选择是所有 Feature 直接依赖 `:security` 的门面，或让业务 Feature 依赖 Domain 合同并由应用组合层绑定 Security 实现。采用后者：Unlock 通过 Domain 的 `AuthenticationManager` 与 `AuthenticationMethodProvisioner` 请求认证能力；其他业务仍通过领域授权/会话合同表达访问需求；Vault 不导入 Security 具体实现。App 组合根持有 Android 平台宿主适配，因其所需的 FragmentActivity、Compose 与 BiometricPrompt 类型不属于 Domain 合同。

## 设计边界

- Domain 合同表达认证请求、认证结果、授权能力、密码规则和访问状态，不携带 Android 类型或算法实现细节。
- Core 不包含 `com.aozijx.passly.security.*` 的实现源码。无状态密码学原语、通用加密器与 `SensitiveFieldCryptor`、`AttachmentContentProtector` 能力合同留在 Core；`FieldKeyProvider` 仅作为通用 field crypto 的取钥接缝。
- Security 内部按职责组织 `authentication.flow`、`authentication.method.*`、`authentication.host`、`session`、`authorization`、`lock`、`dek` 与 `envelope`。认证执行、Keystore/凭据材料、授权、锁定、会话协调和 DEK 生命周期的具体实现只对 Security 内部可见；App 继续只访问认证宿主及 Domain 合同。
- `SensitiveDataKeyManager`、`DekManager`、`FieldKeyManager`、`AttachmentDataKeyManager`、`SecuritySensitiveFieldCryptor`、`AttachmentContentCrypto`、`VerificationTag` 和 `EnvelopeManager` 均归 Security。Core 继续拥有 `KeyDerivation`、AES 通用引擎、`MemoryCleaner`、`FieldEncryptor` 与能力合同。
- `SensitiveFieldCryptor` 只表达 Data 所需的加密/解密动作，不暴露 provisioned key、DEK、freshness 状态或解锁/清除入口；废弃且无调用方的 `decryptProvisioned` 旁路一并删除。Data 不直接依赖 `:security`，Security 在组合图中绑定合同。
- 一次 `UNLOCK_VAULT` 认证由 Security 统一完成凭据验证、DEK 准备和会话转换，并通过 `DatabaseSessionLifecycle` 请求打开/恢复数据库会话。数据库对象和实际文件/连接资源的打开关闭仍由 Data 实现；`:runtime:session` 管理资源无关的 lease 与生命周期状态。App 不直接操作 Data 会话。
- Unlock 页面负责输入展示和导航反馈；认证方式编排、认证策略、凭据设置/恢复的安全操作及数据库会话切换通过 Security 的窄入口执行。App 不自行实现认证、KDF、KeyStore 操作、授权签发或数据库会话切换。
- `UNLOCK_VAULT` 是 Security 内唯一进入主 Vault 完整会话的认证操作；认证成功与数据库会话开启由同一 Security 编排负责，避免 App 再执行第二次认证/解锁。
- Data 生产代码只依赖 Core 的加密能力合同，不依赖 Security 内部具体加密类；Hilt 由 Security 提供实现绑定。Data 的 AndroidTest 对这些合同使用本地最小 fake，不增加 Security 测试依赖；真实实现行为由 Security 自身测试覆盖。
- 不新增内部兼容适配层。发现旧调用时先迁移全部当前调用点，再移除旧路径及只服务旧路径的模型、测试和文档；如确认受保护合同，则单独记录消费者、合同与保留原因。

## 迁移顺序

1. 清点 Core 中 `security.*` 实现、跨模块引用、测试 fixture、Hilt 绑定与外部 Android Keystore 使用点；按运行时 owner 逐类归属。
2. 在 Core 建立窄 `SensitiveFieldCryptor` 合同，迁移 DEK/Envelope/附件密钥实现与对应测试到 Security，保留数据格式和持久化合同不变。
3. 迁移 Data 生产调用至 Core 合同，将绑定移入 Security；Data AndroidTest 用最小 Core 合同 fake，不依赖 Security 实现；删除 `decryptProvisioned` 未使用旁路。
4. 将认证编排、认证方式、认证宿主与会话协调分别整理到职责包；迁移所有 Security 内部调用和测试，并将实现类收窄为模块内可见。
5. 更新架构规则与正式文档，确认 Core 不再含 Security 实现命名空间，Data 主源码无 Security 实现 import，外部日志密钥仍由原 owner 持有。
6. 运行 Security/Core/Data 相关测试与 `verifyArchitecture`，核验依赖图、DI、所有调用点和 `git diff --check`；保留工作区其他已有改动，不暂存、不提交。

## 验收标准

- App 和业务 Feature 不引用 Security 私有实现；Vault 只经由领域合同使用授权/会话能力。
- `:security` 不依赖 App、Vault、Data 实现或 Backup 协议。
- Core 算法 API 不依赖认证流程或 Android Keystore。
- Core 主源码不再包含 `com.aozijx.passly.security.*` 实现；Data 主源码不导入 `com.aozijx.passly.security.*`。
- DEK、敏感字段密钥、附件密钥、信封包装/校验和认证实现均由 Security 持有；`KeyDerivation`、AES 通用原语及安全内存工具仍由 Core 持有。
- 日志加密及遥测标识散列仍由其独立的 Data/App owner 管理，不与 Vault DEK 生命周期耦合。
- Unlock 继续可通过 App 路由访问，且其认证行为由 Security 实现、密码业务规则来自 Domain、算法来自 Core。
- 本仓库内部调用在迁移点完成一次性切换；本轮无已确认的外部消费者需要兼容。
- 正式边界文档与源码、Gradle 依赖及边界检查保持一致。

## 已完成的归属判断

- `SensitiveFieldCryptor` 留在 Core 的加密能力合同，不放入 Domain：它不暴露密钥/状态，Data 是唯一生产消费者，返回值是不透明密文字节，避免 Domain 承担存储密文语义。
- `AttachmentContentProtector` 留在 Core 作为 Data 能力合同；具体实现与 attachment-only key manager 迁入 Security，因为其密钥由 Vault DEK 包装。
- Core 的 `FieldEncryptor`、`CryptoEngine`、AES 实现、`KeyDerivation` 和 `MemoryCleaner` 属于通用无状态加密/内存能力，不随 `security.*` 源码搬迁。
- Data/App 中日志用途的独立 Keystore key 不属于 Vault/认证生命周期，本轮保留。
- 当前没有需要兼容的外部消费者：Security 实现路径只由本仓库调用，内部调用一次性迁移并删除旧路径；`AttachmentContentProtector` 与新 `SensitiveFieldCryptor` 能力合同保留。
