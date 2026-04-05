# Passly

Passly 是一款基于 **Jetpack Compose** 构建的离线优先隐私保险库 Android 应用。

## 核心特性

- 生物识别解锁（系统 KeyStore）
- 本地强加密存储（AES-256 GCM）
- 加密备份与恢复
- 多条目类型策略引擎（密码、TOTP、Passkey 等）
- Autofill 候选优化与慢操作监控
- Material 3 UI（动态色 / 深色模式）

## 技术栈

- Jetpack Compose + Material 3
- Room + SQLCipher
- Kotlin Coroutines + Flow
- AppContainer（手写依赖注入）
- CameraX + ML Kit

## 快速开始

### 环境要求

- Android Studio Otter (2024.2.2)+
- JDK 21
- Gradle 8.13+
- Android 12+ (API 31+)

### 本地构建

```powershell
Set-Location "D:\MyApplication\Passly"
.\gradlew.bat :app:compileFullDebugKotlin
.\gradlew.bat :app:assembleDebug
```

## 文档导航

- 统一入口：`docs/INDEX.md`
- 项目目录结构：`docs/PROJECT_STRUCTURE.md`
- 开发者项目文档：`docs/DEVELOPER_GUIDE.md`
- 改动操作手册：`docs/CHANGE_PLAYBOOK.md`
- 架构决策记录：`docs/ARCHITECTURE_DECISIONS.md`

> 推荐阅读顺序：先看 `README.md`，再看 `docs/INDEX.md`，随后按场景进入对应文档。
> 如需完整开发与改动流程，请以 `docs/INDEX.md` 为准，不再以 README 承载详细规则。

## 项目原则

- 安全优先
- 离线优先
- 分层清晰（Clean Architecture + Package by Feature）
- 可维护性优先（策略、配置、样式 token 集中管理）

## 开源协议

本项目采用 [Apache-2.0](LICENSE) 协议开源。
