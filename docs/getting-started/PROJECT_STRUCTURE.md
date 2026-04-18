# Passly 项目目录结构

本文档用于快速理解项目目录组织与职责边界。

## 导航

- 文档索引：`INDEX.md`
- 开发者指南：`DEVELOPER_GUIDE.md`
- 改动操作手册：`CHANGE_PLAYBOOK.md`
- 架构决策记录：`ARCHITECTURE_DECISIONS.md`

---

## 项目结构

采用按功能模块划分（Package by Feature）并结合简洁架构（Clean Architecture）的思想。

---

## 目录职责速记

- `core`：跨模块复用能力，不直接承载页面业务。
- `data`：数据实现细节（本地存储、映射、仓储实现）。
- `domain`：业务抽象与用例编排，尽量保持纯逻辑。
- `features`：按业务拆分的 UI 与交互入口。
- `service`：系统服务能力（如 Autofill）及其引擎。
- `ui`：全局主题与 UI 基础配置。