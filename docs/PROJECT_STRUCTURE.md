# Passly 项目目录结构

本文档用于快速理解项目目录组织与职责边界。

## 导航

- 文档索引：`docs/INDEX.md`
- 开发者指南：`docs/DEVELOPER_GUIDE.md`
- 改动操作手册：`docs/CHANGE_PLAYBOOK.md`
- 架构决策记录：`docs/ARCHITECTURE_DECISIONS.md`

---

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

---

## 目录职责速记

- `core`：跨模块复用能力，不直接承载页面业务。
- `data`：数据实现细节（本地存储、映射、仓储实现）。
- `domain`：业务抽象与用例编排，尽量保持纯逻辑。
- `features`：按业务拆分的 UI 与交互入口。
- `service`：系统服务能力（如 Autofill）及其引擎。
- `ui`：全局主题与 UI 基础配置。

