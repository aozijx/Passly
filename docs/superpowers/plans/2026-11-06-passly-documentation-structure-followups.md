# 文档结构后续待办

**范围：** 生成文档在主题目录中的归类和路径合同。

## Task 1：将生成错误码目录归入 observability 主题

- [ ] 将生成输出从 `docs/ERROR_CODES.md` 迁入小写 kebab-case 的 observability 主题路径；同步更新 `ModuleBoundariesPlugin` 的 `markdownOutputFile` / `markdownFile`、`VerifyErrorCatalogTask` 的 stale-file 消息、生成器校验、主题页与 README 索引。验收：运行生成任务后输出与校验任务指向同一文件，且全仓 Markdown 相对链接检查通过。

**代码证据：** `buildSrc/src/main/kotlin/passly/ModuleBoundariesPlugin.kt` 当前在配置生成任务和校验任务时都固定设置 `docs/ERROR_CODES.md`；`buildSrc/src/main/kotlin/passly/VerifyErrorCatalogTask.kt` 的错误提示也写死该路径。`docs/architecture/observability/record-and-trace.md` 及 `docs/README.md` 引用生成文件。它由 `config/observability/error-catalog.json` 生成，不能只移动输出文件或手工改名。

**相关约定：** [文档维护约定](../../development/documentation.md)。