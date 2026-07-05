# Passly

Passly 是一款基于 **Jetpack Compose** 构建的离线优先隐私保险库 Android 应用。

## 核心特性

- 生物识别解锁（系统 KeyStore + BiometricPrompt）
- AES-256 GCM 本地强加密存储
- 加密备份与恢复
- 多条目类型策略引擎（密码、TOTP、Passkey、银行卡等）
- Autofill 自动填充支持
- Material 3 UI（动态色 / 深色模式）
- 应用密码（App Password）独立于系统锁屏

## 技术栈

- Jetpack Compose + Material 3
- Room + SQLCipher
- Kotlin Coroutines + Flow
- Hilt（依赖注入）
- CameraX + ML Kit（二维码扫描）

## 快速开始

### 环境要求

- Android Studio Otter (2024.2.2)+
- JDK 21
- Gradle 9.4.1+
- Android 12+ (API 31+)

### 构建命令

```powershell
# 编译
.\gradlew.bat :app:compileDebugKotlin --no-daemon

# 打包
.\gradlew.bat :app:assembleDebug --no-daemon
```

## 项目原则

- 安全优先
- 离线优先
- 分层清晰（Clean Architecture + Package by Feature）
- 可维护性优先（策略、配置、样式 token 集中管理）

## 开源协议

本项目采用 [Apache-2.0](LICENSE) 协议开源。