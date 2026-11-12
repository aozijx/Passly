# AppNotice host owner 审查

## 结论

删除 `AppNoticeHostViewModel` 与 `RenderedNotice`。此前唯一消费者 `AppNoticeEffects` 只订阅 `InAppNoticeStream.notices`、调用 `NoticeTextResolver.resolve()` 并把受控文案映射为 Toast，没有独立 UI 状态、任务组、重试、授权或资源清理合同。该映射留在唯一展示者 `AppNoticeEffects`，不再经过仅供转发的 ViewModel。

## 当前调用链

`MainActivity` 从 Hilt 注入 `AppNoticePublisher`、`InAppNoticeStream` 和 `NoticeTextResolver`，传给 `PasslyApp`。`PasslyApp` 在应用 Compose 树中调用 `AppNoticeEffects`；该 Composable 使用 `LaunchedEffect` 收集应用内消息、解析本地化文本并显示 Toast。订阅仍随 Composable 进入/离开组合而开始/取消；队列、通知路由和系统通知交付合同未改。

`InAppNoticeStream` 与 `NoticeTextResolver` 仍通过消息中心 DI 绑定；通知发布者、Router/Sink 和 Android 系统通知网关不受本次变更影响。Autofill Activity 继续使用自己的发布入口，不负责消费主应用的 Toast 流。

## 证据与范围

| 项目 | 证据 |
| --- | --- |
| 原唯一消费者 | `AppNoticeEffects` 原先通过 `hiltViewModel<AppNoticeHostViewModel>()` 收集 `toastMessages`；全仓生产引用只有此处。 |
| 原有映射 | ViewModel 仅执行 `inAppNoticeStream.notices.map { resolver.resolve(it) }` 并转为 `RenderedNotice`。 |
| 新所有者 | [`MainActivity`](../../../app/src/main/java/com/aozijx/passly/presentation/MainActivity.kt) 注入合同；[`PasslyApp`](../../../app/src/main/java/com/aozijx/passly/presentation/PasslyApp.kt) 接线；[`AppNoticeEffects`](../../../app/src/main/java/com/aozijx/passly/app/message/presentation/AppNoticeEffects.kt) 收集、解析与呈现。 |
| 正式合同 | [消息中心](../../platform/architecture/message-center.md) 记录队列、路由、受控文案与应用内提示调用链。 |

未发现 `AppNoticeHostViewModel` 专属测试；本次未新增或运行测试。已做调用引用核对与 `git diff --check`，未进行 Android 运行时/设备验收。
