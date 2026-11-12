# PageEffects owner 审查

**范围：** 复核 Task 84 中 `PageEffects` 候选，以及 Task 51 对通用 collector 与真实 Route/宿主验收的拆分。只审查共享传递机制的 owner 和已存在的测试，不宣称所有消费方的事件有效期已完成审计。

## Owner 与合同

`PageEffects<Effect>` 是单个页面 ViewModel/workflow 的单次事件队列。每次 `enter()` 创建访问 owner；`tryEmit` 和 `send` 只接受当前 owner，队列事件同时携带 owner token，消费时再次过滤旧 owner。旧组合迟到的 `exit(oldOwner)` 不会停用新页面访问。默认缓冲有限；满载时 `tryEmit` 返回失败，`send` 才等待容量。

`collectPageEffects()` 只在 Lifecycle `STARTED` 期间收集。暂停期间当前访问发出的事件留在 Channel，恢复后继续消费；访问退出后，owner token 过滤其排队事件。共享 helper 不决定具体事件的过期时间，也不恢复任何外部请求：Toast/Snackbar、导航、系统 picker、权限和平台 callback 的请求代际、关闭、重建、取消及 callback 异常均由各 Route 与 ViewModel 定义。

多个 Vault、Backup、Recovery 和 Settings ViewModel，以及 Vault 命令 workflow，创建 `PageEffects` 并由相应 Route 收集。Detail 与编辑页也使用通用 `collectPageEffects()`，但传入的是各自的 Flow，不因此成为 `PageEffects` 的共享状态 owner。Queue 与 lifecycle collector 不与业务 reducer、effect handler 或单个 Route 重复；它们边界不同，保留。共享契约见[状态管理规范](../../engineering/development/architecture-and-state.md#页面一次性事件的可见性与-owner)与[Compose UI 规范](../../engineering/development/compose-ui.md)。

## 已有验证与未闭合范围

`PageEffectsTest` 覆盖退出后旧 owner 队列丢弃、迟到旧 exit 不关闭新 owner，以及有限队列溢出拒绝；`PageEffectLifecycleTest` 覆盖 STOPPED→STARTED 的一次交付与访问退出丢弃缓冲事件。它们闭合通用 helper 的 owner/lifecycle 合同。它们不验证具体 Route 在锁定、动画转场、请求代际、callback 失败或 Activity recreation 时应重放、撤销或恢复什么。主计划 Task 51 保留这些产品/宿主级合同；Task 84 的 PageEffects owner 候选可以移除。

本轮只查阅生产代码、消费方搜索和已有测试，没有运行测试。

## 代码位置

| 职责 | 类/函数 | 源码或测试 |
| --- | --- | --- |
| 按页面访问 owner 缓冲/过滤单次事件 | `PageEffects.enter()`、`tryEmit()`、`send()`、`exit()` | [`PageEffects.kt`](../../../app/src/main/java/com/aozijx/passly/presentation/shared/event/PageEffects.kt) |
| 在宿主 STARTED 期间收集事件 | `collectPageEffects()` | [`PageEffects.kt`](../../../app/src/main/java/com/aozijx/passly/presentation/shared/event/PageEffects.kt) |
| 验证 owner 替换与有限队列 | `PageEffectsTest` | [`PageEffectsTest.kt`](../../../app/src/test/java/com/aozijx/passly/presentation/shared/event/PageEffectsTest.kt) |
| 验证 STOPPED 缓冲交付与旧 owner 丢弃 | `PageEffectLifecycleTest` | [`PageEffectLifecycleTest.kt`](../../../app/src/test/java/com/aozijx/passly/presentation/shared/event/PageEffectLifecycleTest.kt) |
