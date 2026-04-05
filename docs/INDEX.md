# Passly 文档索引

这是 Passly 的统一文档入口页。

## 0. 文档体系总览

- `入口`：`README.md`
- `导航`：`docs/INDEX.md`（当前文档）
- `认知`：`docs/DEVELOPER_GUIDE.md`
- `执行`：`docs/CHANGE_PLAYBOOK.md`
- `决策`：`docs/ARCHITECTURE_DECISIONS.md`

当你不确定先看哪份时，优先从本页按角色/任务分流。

## 1. 快速入口

- 项目概览：`README.md`
- 项目目录结构：`docs/PROJECT_STRUCTURE.md`
- 开发者指南：`docs/DEVELOPER_GUIDE.md`
- 改动操作手册：`docs/CHANGE_PLAYBOOK.md`
- 架构决策记录：`docs/ARCHITECTURE_DECISIONS.md`

## 2. 按角色阅读

- 新加入开发者：先看 `docs/DEVELOPER_GUIDE.md`
- 功能改造/重构执行者：先看 `docs/CHANGE_PLAYBOOK.md`
- 方案设计/评审者：先看 `docs/ARCHITECTURE_DECISIONS.md`

## 3. 按任务查找

- 想快速定位目录与模块职责：`docs/PROJECT_STRUCTURE.md`
- 想了解项目结构与关键文件：`docs/DEVELOPER_GUIDE.md`
- 想执行数据库/备份/卡片样式/Autofill 改动：`docs/CHANGE_PLAYBOOK.md`
- 想确认“为什么采用当前技术方案”：`docs/ARCHITECTURE_DECISIONS.md`

## 4. 推荐阅读顺序

`README.md` -> `docs/INDEX.md` -> `docs/DEVELOPER_GUIDE.md` -> `docs/ARCHITECTURE_DECISIONS.md` -> `docs/CHANGE_PLAYBOOK.md`

## 5. 文档维护约定

1. 新增文档优先放在 `docs/`，文件名使用全大写蛇形（如 `XXX_YYY.md`）。
2. 涉及高风险改动的文档更新顺序：先改 `ARCHITECTURE_DECISIONS`，再改 `CHANGE_PLAYBOOK`。
3. 新增条目时尽量补充“适用范围 + 风险点 + 验证方式”，避免只写结论。

