# 完整表单编辑 owner 与失败交接审查

## 当前合同

完整编辑的普通预加载异常由 `DetailViewModel.prepareEntryForEdit()` 映射为仅带条目 ID 的 effect；当前条目、认证对象和完整访问仍匹配时才发出，Route 对应 ID 显示本地化提示，用户可重新点击编辑。取消和 fatal `Error` 继续传播。编辑器 seed 导航普通异常按 token 清理并返回 `NavigationFailed`；取消和 fatal `Error` 清理后继续传播。Route 生命周期内关闭/切换页面会取消预加载任务，迟到 owner 结果不交付。保存 Session 遇 fatal `Error` 时会终结当前会话、清理表单并重抛错误。当前实现合同见[完整表单编辑](../../product/vault/full-form-editing.md)和[编辑与写入](../../product/vault/entry-editor-and-persistence.md)。

完整编辑只通过一次性 token 交接条目；`Entry` 和其不可变 `String` 值不进入导航参数、SavedState 或 Intent。受控引用清除和临时 `CharArray` 覆盖不保证 JVM 中不可变 String 副本的物理擦除；要增强该保证需整体迁移 Domain、Presentation 和持久化写入类型。

## 已完成的 JVM 验收

- 2026-11-07：`DetailViewModelTest` 27 项通过，包含普通预加载异常重试、取消与 `LinkageError` 传播；六个编辑能力/页面测试合计 98 项通过；`verifyArchitecture` 与 `git diff --check` 通过。
- 2026-11-07：`EditorWriteSessionTest` 13 项通过，覆盖 fatal Error 原样传播、表单清理、Saving 释放和 session 不可重开；`verifyArchitecture` 通过。随后一条无关增量断言运行被当时的 `BackupChunkedContainerCodecTest.kt` 编译错误阻断，该断言已撤回；前述通过结果来自保留的测试版本。
- 2026-11-08：`EntryEditorSeedCoordinatorTest` 与 `VaultNavigationTest` 共 12 项通过，覆盖导航普通异常、token 补偿、旧请求失败不清理新 token、取消/fatal 清理后传播；相关 Kotlin 编译、`verifyArchitecture` 与 `git diff --check` 通过。
- 2026-11-08：完整编辑相关 UseCase、Detail ViewModel/Reducer、四类编辑 ViewModel、写入 Session、effect handler 与 Vault Navigation 共 151 项通过，`verifyArchitecture` 通过。

以上为原维护计划中的历史运行结果，本次迁移到审查记录时没有重跑。真实 Detail Route Toast/重复点击、编辑器 Route Snackbar/返回重试、Activity STOPPED/重建和平台 callback 交错仍未由宿主行为测试证明，继续由主维护 TODO Task 42 跟踪。

## 代码位置

| 职责 | 类/测试 | 源码文件 |
| --- | --- | --- |
| Detail 预加载异常映射和 owner 核对 | `DetailViewModel.prepareEntryForEdit()` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt) |
| 生命周期内显示 Detail 预加载与编辑路由失败反馈 | `DetailRoute`、`collectPageEffects()` | [`DetailRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailRoute.kt) |
| seed 导航异常补偿 | `EntryEditorSeedCoordinator.navigateWithSeed()` | [`EntryEditorSeedCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinator.kt) |
| fatal Error 清理与 session 终态 | `EditorWriteSession.save()`、`close()` | [`EditorWriteSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorWriteSession.kt) |
| 预加载异常重试和取消/fatal 传播 | `DetailViewModelTest` | [`DetailViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModelTest.kt) |
| seed 导航失败补偿 | `EntryEditorSeedCoordinatorTest` | [`EntryEditorSeedCoordinatorTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinatorTest.kt) |
| fatal Error 后清理与会话终态 | `EditorWriteSessionTest.fatalFailureClearsTheFormClosesTheSessionAndPropagatesTheError` | [`EditorWriteSessionTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/editor/EditorWriteSessionTest.kt) |
