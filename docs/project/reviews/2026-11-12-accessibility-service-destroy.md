# Accessibility Service teardown 审查

## 当前实现

`ManualAccessibilityFillService.onDestroy()` 先注销系统 Accessibility 按钮回调并移除浮动 View；`removeView()` 的普通异常会通过 `runBlocking` 写失败日志。随后清空浮动 View 引用、调用 `AccessibilityFillSessionStore.unbind()`，再通过 `runBlocking { finishInteraction.expireAll(ACCESSIBILITY) }` 逐个结束该来源的活动 interaction；最后才调用 `scope.cancel()` 和 `super.onDestroy()`。这两处同步桥接都没有 timeout。若失败日志写入本身抛出异常，也会从首个 `runBlocking` 逸出并跳过后续清理。

`FinishAutofillInteraction.expireAll(source)` 读取该来源的 active IDs 并顺序调用 `expire(id)`。每次状态先由 Store 转为终态；只有发生终态转换时才调用 `AutofillUnlockLease.release(id)`。最后一个 borrower 释放时，Lease 在 `NonCancellable` 中持有 Mutex 并尝试锁定临时 session；锁失败会安排进程级 retry 后重新抛出普通异常。因此异常会从当前 `expire()` 传播并中断后续 ID 的循环，也会越过 Service 的 `runBlocking`，使后续 `scope.cancel()` 和 `super.onDestroy()` 未执行。

## 待验证边界

源码确认了同步等待和异常传播路径，但没有证明 Android 回调线程、可接受的最大等待时间、系统是否会遇到日志/锁失败，以及 teardown 被中断时的实际资源后果。当前不据此断言生产缺陷，也不改变实现。

后续验收应以可控 Logger 与 `SessionLockController` 覆盖失败日志、多个活动 interaction 的逐项收尾、retry obligation、Service teardown 与 fatal `Error` 传播；设备侧还需确认 `onDestroy()` 的返回与系统回调行为。对应未完成工作由[并发计划 Task 4](../work/plans/2026-11-06-app-architecture-concurrency-todo.md)跟踪。

## 代码位置

| 职责 | 函数 | 源码 |
| --- | --- | --- |
| 系统按钮、View、Store 与 Service 退出顺序 | `ManualAccessibilityFillService.onDestroy()` | [`ManualAccessibilityFillService.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/accessibility/ManualAccessibilityFillService.kt) |
| 按来源取得 ID 并逐个终结 | `FinishAutofillInteraction.expireAll(source)`、`finish()` | [`FinishAutofillInteraction.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/FinishAutofillInteraction.kt) |
| borrower 归零、同步锁定及失败重试 | `AutofillUnlockLease.release()`、`lockOwnedSessionIfPendingLocked()` | [`AutofillUnlockLease.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillUnlockLease.kt) |
| 无障碍页面 Activity 销毁后的异步取消 | `AccessibilityAutofillEntry` observer、`AccessibilityFillViewModel.cancel()` | [`AccessibilityAutofillEntry.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityAutofillEntry.kt)、[`AccessibilityFillViewModel.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityFillViewModel.kt) |

本轮只核对源码与文档，没有运行测试、构建或设备验收。
