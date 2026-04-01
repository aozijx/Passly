# Poop Vault

一款基于 **Jetpack Compose** 构建的现代化、极简且高安全的隐私保险库 Android 应用。

## 核心特性

- **生物识别解锁**：支持指纹及面部识别，结合系统级 KeyStore 安全存储，确保只有你能访问。
- **高强度本地加密**：采用 AES-256 GCM 算法对每一条账号密码进行独立加密，不依赖任何第三方云服务，数据完全私有化。
- **加密备份与恢复**：支持将保险库内容导出为高度加密的备份文件，并支持在不同设备间安全迁移。
- **隐私防截屏**：全应用开启安全窗口模式，防止恶意应用截屏或系统多任务预览泄露敏感信息。
- **智能分类管理**：支持自定义分类，并能根据名称智能匹配图标，让你的资产井井有条。
- **现代 UI 设计**：遵循 Material 3 规范，支持动态颜色 (Android 12+) 和深色模式。

## 技术架构

- **UI 框架**：Jetpack Compose (声明式 UI)
- **数据库**：Room + SQLCipher (数据库级全磁盘加密)
- **渲染器**：Multiplatform Markdown Renderer (Material 3)
- **依赖注入**：原生 ViewModel + 状态管理
- **异步处理**：Kotlin Coroutines & Flow
- **安全组件**：AndroidX Biometric + AndroidX Security
- **扫码能力**：CameraX + Google ML Kit Barcode Scanning

## 项目结构

采用按功能模块划分（Package by Feature）并结合简洁架构（Clean Architecture）的思想。
```tree
com.example.poop
├── core                // 核心底层能力（跨模块通用）
│   ├── crypto          // 加密解密核心逻辑
│   ├── common          // 基础基类、常量
│   ├── designsystem    // 自定义 UI 组件库 (Material 3)
│   └── util            // 纯工具类 (Backup, Notification)
│
├── data                // 数据层（不含 UI 逻辑）
│   ├── local           // Room 数据库, DataStore
│   ├── repository      // 聚合数据源
│   └── model           // 数据库实体 (Entity)
│
├── domain              // 领域层（业务模型和规则）
│   └── model           // 业务模型
│
├── features            // 功能模块层（按业务划分）
│   ├── vault           // 保险箱主界面
│   ├── detail          // 详情页
│   ├── settings        // 设置页
│   └── scanner         // 扫码功能
│
├── service             // 系统级服务
│   └── autofill        // 自动填充功能
│
└── ui                  // 全局 UI 相关
    ├── theme           // 主题配置 (Color, Type, Theme)
    └── NavGraph.kt     // 导航路由
```

## 快速开始

### 开发环境要求
- Android Studio **Otter (2024.2.2)** 或更高版本（需支持 AGP 9.0+）
- **JDK 21** (项目采用 jvmToolchain 21)
- Gradle 8.13+
- Android 12.0+ (API 31+) 设备

### 构建步骤
1. 克隆项目：`git clone https://github.com/aozijx/poop.git`
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

Poop 设计初衷即为“零信任”架构：
- **无联网请求**：应用不包含任何上传数据的网络代码（仅支持手动获取更新日志）。
- **零痕迹**：所有敏感数据在内存中均以加密形式存在，并在使用后立即清除。
- **完全离线**：你的密码只属于你的手机，没有任何云端备份。

## 开源协议

本项目采用 [Apache-2.0](LICENSE) 协议开源。
