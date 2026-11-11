# 附件 GC 与提交后取消审查

## 当前合同

数据库写事务只将无当前/历史引用的附件资源标为 `PENDING_GC`。授权 Vault 会话恢复后，`AttachmentResourceGarbageCollector` 按最多 64 条稳定分页排空；文件失败持久记录并继续后续资源，worker 在授权持续有效期间退避重试。会话取消结束当前 drain，持久待办供下次完整会话恢复。删除前后均检查引用，防止回收新近重新引用的资源。

条目写入返回后由 `PostCommitCleanup.afterCommit()` 执行派生清理。普通清理异常被记录，不覆盖已提交结果；取消继续传播，fatal `Error` 不转换成普通失败。取消返回不代表事务回滚，调用方必须按 Room 持久事实恢复，不应盲目重放写入。

## 代码与覆盖位置

- [AttachmentResourceGarbageCollector](../../../data/src/main/java/com/aozijx/passly/data/repository/attachment/AttachmentResourceGarbageCollector.kt)：`drain()`、`drainPass()`、`drainAfterUnlock()`、`drainOne()` 管理授权、分页、重试和引用保护。
- [PostCommitCleanup](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/PostCommitCleanup.kt)：`afterCommit()` 与 `run()` 定义提交后清理及异常语义；入口包括 `CreateEntryExecutor.execute()`、`UpdateEntryExecutor.execute()`、`RestoreEntryRevisionExecutor.execute()`。
- [AttachmentGarbageCollectionWorkerTest](../../../data/src/test/java/com/aozijx/passly/data/repository/attachment/AttachmentGarbageCollectionWorkerTest.kt) 覆盖分页、失败继续、授权丢失、退避、fatal 与取消。
- [AttachmentResourceGarbageCollectorTest](../../../data/src/androidTest/java/com/aozijx/passly/data/repository/attachment/AttachmentResourceGarbageCollectorTest.kt) 包含跨批、文件删除重试及解锁恢复用例。
- [PostCommitCleanupTest](../../../data/src/test/java/com/aozijx/passly/data/repository/entry/command/PostCommitCleanupTest.kt) 覆盖清理异常、取消、fatal 传播。

本记录核对当前源码及测试定义；本轮未执行测试。AndroidTest 仍待真实 SQLCipher/文件系统设备运行。

## 未闭合验收

活动计划 [Task 64](../work/plans/2026-10-30-passly-maintainability-todo.md#task-64) 保留两项工作：在设备上执行附件 GC instrumentation；沿 Vault `EditorWriteSession`、Detail revision owner 与 Autofill/Inbox 真实调用链验证提交后取消如何映射结果、是否产生重复重试，以及离页和撤权后的恢复。当前静态链路不能证明 owner 行为已验收，也不构成已复现缺陷。
