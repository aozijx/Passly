# Passly 项目目录结构

本文档用于快速理解项目目录组织与职责边界。

## 导航

- 架构决策记录：`docs/architecture/ARCHITECTURE_DECISIONS.md`
- 改动操作手册：`docs/operations/CHANGE_PLAYBOOK.md`
- 模块文档索引：`docs/modules/README.md`

---

## 项目结构

采用按功能模块划分（Package by Feature）并结合简洁架构（Clean Architecture）的思想。

---

## 目录职责速记

- `core`：跨模块复用能力（加密、错误处理、备份引擎、二维码解析），不直接承载页面业务。
- `data`：数据实现细节（本地存储 Room、映射、仓储实现）。
- `domain`：业务抽象与用例编排，保持纯逻辑。
- `ui/features`：按业务拆分的 UI 与交互入口（vault、settings、backup、detail 等）。
- `ui/navigation`：Compose Navigation 导航图。
- `ui/theme`：全局主题、颜色与样式配置。
- `service`：系统服务能力（Autofill）及其解析器与展示器。