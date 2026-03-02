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
- **依赖注入**：原生 ViewModel + 状态管理
- **异步处理**：Kotlin Coroutines & Flow
- **安全组件**：AndroidX Biometric + AndroidX Security
- **扫码能力**：CameraX + Google ML Kit Barcode Scanning

## 快速开始

### 开发环境要求
- Android Studio Ladybug (或更高版本)
- JDK 17
- Gradle 8.13+
- Android 12.0+ (API 31+) 设备

### 构建步骤
1. 克隆项目：`git clone https://github.com/your-username/poop.git`
2. 使用 Android Studio 打开项目。
3. 在 `local.properties` 中配置你的签名信息（如果是为了发布）。
4. 点击 `Run` 即可运行。

## 隐私与安全

Poop 设计初衷即为“零信任”架构：
- **无联网请求**：应用不包含任何上传数据的网络代码（仅支持手动获取更新日志）。
- **零痕迹**：所有敏感数据在内存中均以加密形式存在，并在使用后立即清除。
- **完全离线**：你的密码只属于你的手机，没有任何云端备份。

## 开源协议

本项目采用 [Apache-2.0](LICENSE) 协议开源。
