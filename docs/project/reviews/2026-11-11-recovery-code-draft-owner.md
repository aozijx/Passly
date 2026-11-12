# Recovery Code 草稿 owner 审查

审查日期：2026-11-11。

## 当前实现

- `RecoveryCodeDraftInteractor.generate()` 创建并持有 `RecoveryCredentialDraft`；`copy()` 在临时字符数组 finally 清理后交付剪贴板；`dismiss()` / `close()` 关闭候选。
- `commit()` 执行草稿提交；`reconcileCommit()` 读取草稿 durable marker，已提交时释放草稿并返回事实结果。
- `RecoveryCodeSettingsViewModel.confirmAndEnable()` 同步进入 Committing 状态并防止重复确认，启动提交协程。取消和 fatal `Error` 路径先进入 `reconcileInterruptedCommit()`，在 `NonCancellable` 中调用 `reconcileCommit()`，再按当前 page owner 更新可用状态、disclosure 与结果状态，最后传播原取消/fatal。
- 因此 durable 写入与 marker 核对留在 Interactor；当前页面 owner 负责根据持久事实收尾 disclosure、`SavedStateHandle` 标记和页面状态。该协调保留在 `RecoveryCodeSettingsViewModel`：把它移入 Security 操作 owner 需要将 PageEffects owner、敏感 disclosure 和 UI 状态回调重新传回 ViewModel，不能形成独立状态/生命周期 owner，且增加转发层。真实 Route/Activity 生命周期验收仍开放。

## 证据与限制

现有测试源码包括：

- `RecoveryCodeDraftInteractorTest`：草稿持有、复制/关闭、挂起剪贴板写入时撤销草稿、durable commit 后释放草稿。
- `RecoveryCodeSettingsViewModelTest`：离页后已确认提交继续、离页取消不恢复 disclosure、durable save 后取消核对状态、fatal 后核对状态、确认单飞与提交期间不可关闭。

本轮只核对生产代码和测试源码，未执行测试，也未验证真实 Route、Activity recreation 或设备行为。因此 Task 84 保留该 owner 审查与宿主验收，不把源码测试覆盖当作运行结果。

## 位置

- [认证专题：恢复码草稿](../../platform/security/authentication.md#恢复码草稿)
- [RecoveryCodeDraftInteractor.kt](../../../app/src/main/java/com/aozijx/passly/feature/settings/security/RecoveryCodeDraftInteractor.kt)
- [RecoveryCodeSettingsViewModel.kt](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt)
- [RecoveryCodeDraftInteractorTest.kt](../../../app/src/test/java/com/aozijx/passly/feature/settings/security/RecoveryCodeDraftInteractorTest.kt)
- [RecoveryCodeSettingsViewModelTest.kt](../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModelTest.kt)
- [主维护待办 Task 84](../work/plans/2026-10-30-passly-maintainability-todo.md#task-84)
