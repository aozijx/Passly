# Passly

一款基于 **Jetpack Compose** 构建的现代化、极简且高安全的隐私保险库 Android 应用。

## 核心特性

- **生物识别解锁**：支持指纹及面部识别，结合系统级 KeyStore 安全存储，确保只有你能访问。
- **高强度本地加密**：采用 AES-256 GCM 算法对每一条账号密码进行独立加密，不依赖任何第三方云服务，数据完全私有化。
- **加密备份与恢复**：支持将保险库内容导出为高度加密的备份文件，并支持在不同设备间安全迁移。
- **类型策略引擎**：内置 9 类条目策略（密码、TOTP、Passkey、恢复码、WiFi、银行卡、助记词、证件、SSH），统一校验与展示规则。
- **自动填充优化**：自动填充匹配从全量加载优化为定向候选查询，并内置慢操作日志监控。
- **隐私防截屏**：全应用开启安全窗口模式，防止恶意应用截屏或系统多任务预览泄露敏感信息。
- **智能分类管理**：支持自定义分类，并能根据名称智能匹配图标，让你的资产井井有条。
- **现代 UI 设计**：遵循 Material 3 规范，支持动态颜色 (Android 12+) 和深色模式。

## 技术架构

- **UI 框架**：Jetpack Compose (声明式 UI)
- **数据库**：Room + SQLCipher (数据库级全磁盘加密)
- **渲染器**：Multiplatform Markdown Renderer (Material 3)
- **依赖注入**：AppContainer（手写依赖注入容器）
- **架构分层**：Entity + Domain Model + Mapper + Repository + UseCase
- **异步处理**：Kotlin Coroutines & Flow
- **安全组件**：AndroidX Biometric + AndroidX Security
- **扫码能力**：CameraX + Google ML Kit Barcode Scanning

## 项目结构

采用按功能模块划分（Package by Feature）并结合简洁架构（Clean Architecture）的思想。

```tree
com.aozijx.passly
├── core                        // 跨模块基础能力
│   ├── backup                  // 备份导入导出
│   ├── common                  // 公共类型与常量
│   ├── crypto                  // 加密/解密能力
│   ├── designsystem            // 可复用 UI 组件
│   ├── di                      // AppContainer 依赖注入
│   ├── logging                 // 日志记录
│   ├── media                   // 媒体与图像能力
│   ├── platform                // 平台适配封装
│   ├── qr                      // 二维码能力
│   ├── security/otp            // TOTP 相关安全逻辑
│   └── storage                 // 存储辅助
│
├── data                        // 数据层
│   ├── entity                  // Room 实体
│   ├── local                   // Room/DataStore 本地源
│   ├── mapper                  // Entity <-> Domain 映射
│   └── repository              // Repository 实现
│
├── domain                      // 领域层
│   ├── mapper                  // 领域映射
│   ├── model                   // 纯业务模型
│   ├── policy                  // 业务策略
│   ├── repository              // 仓库接口
│   ├── strategy                // 条目类型策略
│   └── usecase                 // 用例编排
│
├── features                    // 功能层（按业务拆分）
│   ├── detail
│   ├── scanner
│   ├── settings
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── components/         // 设置子组件
│   │   └── internal/           // 设置内部支持逻辑
│   └── vault
│       ├── VaultScreen.kt
│       ├── VaultViewModel.kt
│       ├── components/         // vault 组件与卡片渲染
│       ├── dialogs/            // vault 对话框
│       └── internal/           // vault 内部逻辑
│
├── service
│   └── autofill
│       ├── engine/             // 匹配与填充引擎
│       └── presentation/       // RemoteViews/展示层
│
└── ui
    └── theme                   // 全局主题
```

## 快速开始

### 开发环境要求

- Android Studio **Otter (2024.2.2)** 或更高版本（需支持 AGP 9.0+）
- **JDK 21** (项目采用 jvmToolchain 21)
- Gradle 8.13+
- Android 12.0+ (API 31+) 设备

### 构建步骤

1. 克隆项目：`git clone https://github.com/aozijx/Passly.git`
2. 使用 Android Studio 打开项目。
3. 等待 Gradle 同步完成。
4. 点击 `Run` 即可在真机或模拟器上运行。

## 持续集成 (CI)

项目配置了 **GitHub Actions + CodeQL** 自动化安全分析，确保代码符合安全规范：

- **静态扫描 (Static Analysis)**：
  - **技术方案**：采用 CodeQL 的 `build-mode: none` 模式。
  - **原因注释**：由于项目使用了最新的 **Kotlin 2.3.20 (Alpha)** 和 **KSP**，传统的编译器拦截模式会产生版本不兼容报错（KotlinVersionTooRecent）。使用 `none` 模式可绕过编译器环境直接扫描源码，确保 CI 稳定性。
- **漏洞防护**：
  - 自动检测 Android 常见安全风险，如 **Implicit PendingIntent**（隐式意图重定向）等。
  - 自动扫描泄露的硬编码密钥和敏感凭据。

## 隐私与安全

Passly 设计初衷即为“零信任”架构：

- **无联网请求**：应用不包含任何上传数据的网络代码（仅支持手动获取更新日志）。
- **零痕迹**：所有敏感数据在内存中均以加密形式存在，并在使用后立即清除。
- **完全离线**：你的密码只属于你的手机，没有任何云端备份。

## 开源协议

本项目采用 [Apache-2.0](LICENSE) 协议开源。
