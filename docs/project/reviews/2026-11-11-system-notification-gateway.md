# Android 系统通知网关平台边界审查

## 结果

`AndroidSystemNotificationGateway` 通过 `SystemNotificationPlatform` 访问系统通知状态、Channel 和 `notify()`；Android API 与资源解析由 `AndroidSystemNotificationPlatform` 封装。网关继续解析受控通知文案，将普通平台/文案异常映射为 `SinkResult.Failed`，并传播取消与 fatal `Error`。通知不可用时不创建 Channel、不解析文案，也不发出通知。

## 验收

`AndroidSystemNotificationGatewayTest` 覆盖 Channel 参数、资源 ID、稳定通知 ID、解析文案、通知发送、普通异常、取消、fatal `Error` 和系统通知不可用路径。`DefaultAppNoticeDispatcherTest` 覆盖普通 sink 异常和 fatal 路由异常后的 dedup claim 释放及同 event ID 重试。同批维护记录确认两个定向测试通过；本次文档清理未重跑这些命令。

消息分发、目标回退和事件 claim 合同见[消息中心](../../platform/architecture/message-center.md)。

## 代码位置

| 职责 | 类/测试 | 源码文件 |
| --- | --- | --- |
| 通知路由、授权状态与失败映射 | `AndroidSystemNotificationGateway.current()`、`deliver()` | [`AndroidSystemNotificationGateway.kt`](../../../app/src/main/java/com/aozijx/passly/app/message/system/AndroidSystemNotificationGateway.kt) |
| 可注入的系统通知能力合同 | `SystemNotificationPlatform` | [`SystemNotificationPlatform.kt`](../../../app/src/main/java/com/aozijx/passly/app/message/system/SystemNotificationPlatform.kt) |
| Android 通知与 Channel API 实现 | `AndroidSystemNotificationPlatform` | [`AndroidSystemNotificationPlatform.kt`](../../../app/src/main/java/com/aozijx/passly/app/message/system/AndroidSystemNotificationPlatform.kt) |
| 平台适配器错误与通知行为 | `AndroidSystemNotificationGatewayTest` | [`AndroidSystemNotificationGatewayTest.kt`](../../../app/src/test/java/com/aozijx/passly/app/message/system/AndroidSystemNotificationGatewayTest.kt) |
| 去重 claim 的异常恢复与重试 | `thrownSinkExceptionReleasesClaimAndReturnsRetryableFailure()`、`fatalRouterErrorPropagatesAfterReleasingClaimForRetry()` | [`DefaultAppNoticeDispatcherTest.kt`](../../../app/src/test/java/com/aozijx/passly/app/message/runtime/DefaultAppNoticeDispatcherTest.kt) |
