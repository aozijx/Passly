# Passly Backup 文档扫描待办

**状态：** 待实现与验证。条目来自 2026-11-06 对归档调用链的静态核对；本文件记录问题和验收，不代表已修改运行实现。

## Task 1：兼容导入不能把 fatal Error 当成无效条目

- [ ] 检查 `BackupImportPlanner.plan(bundle, strategy)` 兼容分支逐 Entry 校验处的 `runCatching { BackupBundleValidator.validate(...) }`。Kotlin `runCatching` 捕获 `Throwable`，当前 `isSuccess` 将 `OutOfMemoryError`、`LinkageError` 等 fatal `Error` 解释为普通候选校验失败，随后可能把该 Entry 计入 `skippedEntryCount` 并继续导入。
- [ ] 收窄候选校验失败分类：只把预期的结构/业务验证异常转换为“跳过该条目”，让 fatal `Error` 原样逸出；不要掩盖恢复资源清理或主错误传播合同。
- [ ] 增加兼容导入测试，证明候选验证抛出的普通校验异常可跳过该条目，而 fatal `Error` 不会变成成功的兼容导入结果；同时保留未知字段/类型的计数行为。
- [ ] 更新导入工作流文档和兼容导入验证记录，说明何种错误会跳过单个 Entry、何种错误终止整个导入。

**当前证据（2026-11-06）：** `BackupImportPlanner.kt` 的兼容策略对每个候选构造无附件的 `BackupBundle` 并调用 `BackupBundleValidator.validate(..., requireResourceData = false)`；其结果由 `runCatching { ... }.isSuccess` 判定。Kotlin `runCatching` 捕获所有 `Throwable`，因此 fatal `Error` 会被当作 `valid = false`，计入跳过分支。此条是静态确认，尚未运行测试或修改实现。

**验收：** 验证异常按兼容合同计数；fatal `Error` 从规划器传播且不产生部分恢复成功；相关导入测试和质量门禁通过。

## Task 2：为备份导入定义累计内存预算

- [ ] 核对原生 JSON、Bitwarden JSON、加密容器各阶段的输入、文本、JSON tree、ZIP、解压资源及重序列化副本，并确定统一累计预算与格式级上限。
- [ ] 证明超限在完整资源物化前被拒绝；边界测试覆盖单资源、累计资源、畸形 UTF-8/Base64 和刚好达到上限的有效文件。
- [ ] 更新备份导入格式文档，记录预算、验证顺序与峰值副本；保留现有 wire key 与格式兼容合同。

**当前证据（2026-11-06）：** `BackupFileStore.readBytesSafely()` 限制单次输入为 256 MiB，但 Passly JSON/Bitwarden JSON 会构建完整文本或 JSON tree；加密导入会持有原容器、解密 ZIP 字节及提取的 JSON/资源。不同阶段副本和累计内存尚无统一上限证明。
