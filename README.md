# Passly

Passly 是一款离线优先的 Android 隐私保险库，使用 Jetpack Compose 构建。敏感数据由 SQLCipher 与字段级
AES-256-GCM 共同保护，认证凭据通过信封解密同一份 Vault DEK。

## 能力概览

- 生物识别、应用密码和一次性恢复码解锁
- 密码、TOTP、银行卡等保险库条目
- Android Autofill 与 Credential Manager 集成
- 带附件的版本化加密备份
- 自动锁定、剪贴板清理与集中消息通知

## 技术基线

- Android API 31–36、JDK 21
- Kotlin、Coroutines、Flow、Hilt
- Jetpack Compose、Material 3、Navigation、Paging 3
- Room、SQLCipher、Proto DataStore
- AES-256-GCM、Android Keystore、Argon2id

## 本地构建

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

Release 构建必须提供完整签名配置；缺少凭据时不会回退到 Debug
签名。详细环境、验收命令和已知问题见[开发指南](docs/getting-started/development.md)。

## 文档

从[文档中心](docs/README.md)开始。架构约束、安全模型、数据格式、功能设计和 ADR 均以该目录为索引。

## 许可证

本项目采用 [Apache-2.0](LICENSE) 许可证。
