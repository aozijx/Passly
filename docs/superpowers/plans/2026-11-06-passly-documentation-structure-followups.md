# 文档结构后续待办

**范围：** 生成文档在主题目录中的归类和路径合同。

## Task 1：将生成错误码目录归入 observability 主题

- [x] 将生成输出从 `docs/ERROR_CODES.md` 迁入 `docs/architecture/observability/error-codes.md`；同步生成任务、校验任务、stale-file 消息、主题页与 README。运行 `generateErrorCodes verifyErrorCodes verifyArchitecture`，并完成相对链接检查。

**完成记录（2026-11-06）：** 生成任务和校验任务均以 `docs/architecture/observability/error-codes.md` 为目标，stale-file 错误提示也指向新路径。`record-and-trace.md` 使用同目录相对链接，README 入口改指主题路径。`generateErrorCodes`、`verifyErrorCodes`、`verifyArchitecture` 及全仓 Markdown 相对链接检查通过；本次未运行 buildSrc 单元测试。

**相关约定：** [文档维护约定](../../development/documentation.md)。
## Task 2：从维护待办中归档已完成工作

- [ ] 将 `docs/superpowers/plans/2026-10-30-passly-maintainability-todo.md` 中已完成的 task 和子步骤验收证据迁入有日期的 `reviews/` 记录；活动 plan 仅保留未完成的下一步与验收条件。保留仍开放 task 的关键根因证据并让其明确链接已归档记录，不丢失当前未提交新增的开放事项。完成后核对 README 索引、相对链接和计划中的 `[x]` 清单。

**证据（代码/文档核对：2026-11-06）：** 该 593 行活动维护 TODO 含 109 个开放和 93 个已完成 checkbox；多项完整结束的 task（例如设备锁 Task 28、通知快照 Task 30、Task 53–58、60–72、Autofill 域名 Task 76）仍留在活动计划。Task 58 已有独立长函数/大文件审查记录，Task 28 与 Task 76 也已有对应交付记录可作为归档目标。迁移时应保留仍开放任务需要的实现证据及现有用户编辑，不按行数机械截断。