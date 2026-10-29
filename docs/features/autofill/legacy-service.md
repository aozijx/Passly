# Legacy Autofill Service

状态：当前实现（API 31+，代码核对：2026-10-24）。

`LegacyAutofillService` 解析 `AssistStructure`、处理取消信号并构造 Legacy `FillResponse`/`Dataset`。它不构造
Credential Manager response。

## Fill 和 SaveInfo

`LegacyResponseFactory` 从已分类角色创建 `SaveInfo`：密码 IDs 为 required，用户名 IDs 为 optional；没有密码
字段时不广告保存。SaveInfo 使用 password data type，有用户名时增加 username type，并设置
`FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE`。即使没有候选，也可以返回只含 SaveInfo 的 FillResponse。

发现结果只在 `LegacyResponseFactory` 映射一次，同时生成 `FillResponse` 和供 Service 记录日志、继承 interaction
trace 的 `LegacyDiscoveryObservation`。Service 不再重复解释 `Candidates`、`RequiresUnlock` 等 domain outcome。
Credential Manager 仍使用自己的 response 合同，不与 Legacy 结果类型合并。

Legacy 服务断连或销毁时会过期其来源的所有活动交互，释放对应的 Autofill 解锁租约。清理在生命周期回调内立即启动，
并以不可取消的短任务完成；服务销毁随后取消普通请求 scope，不会中断这项状态回收。重复触发安全：交互只有在首次终态转换时释放租约。

系统悬浮候选菜单使用 RemoteViews。业务层先生成 `AutofillRemoteItem.SingleLine/DoubleLine`，渲染器只负责选择对应
XML 并绑定文字；XML 不包含匹配类型徽标、自定义卡片背景或业务判断。这样候选、解锁入口和选择器入口共享同一
展示协议，而 domain 候选模型不会泄漏到 Android View 渲染器。

键盘 Inline 模式在服务元数据中声明支持，读取 `FillRequest.inlineSuggestionsRequest` 的最大数量和规格，
仅在键盘规格声明支持 AndroidX v1 模板时，为锁定入口与候选 Dataset 构造 `InlinePresentation`。请求为空、
容量为零、未给规格或模板版本不兼容时只提供菜单呈现；
悬浮模式始终只提供菜单呈现。认证后重建 response 的入口继续携带平台 Inline 请求。选择底部模式时，
认证占位 Dataset 打开应用内 `ModalBottomSheet`，不把 BottomSheet 伪装成 Inline。

Legacy 平台的认证结果按入口显式区分，不能复用一个含隐式布尔默认值的 Intent：

- 锁定发现入口是 `RESPONSE`，认证后返回重新构造的 `FillResponse`；
- 单候选入口和底部候选选择器是 `DATASET`，选择后直接向 Android 返回所选 `Dataset` 并触发填充。

底部选择器因此挂在一个需要认证的占位 Dataset 上，而不是 `FillResponse.setAuthentication()`。后者只适用于认证后
替换整个响应；用于候选选择时会让 Android 只刷新候选而不填充。`AutofillFillRequest` 必须带有效的结果目标，未知或
缺失值直接拒绝，不兼容旧的 `returnDataset` 格式。

Android 的该 flag 表示所有 required views 不可见时可触发保存，不等同于 Passly 自己持久化或排队凭据。
`FLAG_DELAY_SAVE` 只把 FillContext 传递到同一 task 的后续 Autofill 请求，不能让 Passly 在未来解锁时主动取回
一份挂起凭据，因此不是 sealed Inbox 的替代实现。

## 当前 onSaveRequest

服务从最新 `FillContext` 重新解析并分类，不查找或重开已结束的 fill interaction。最低草稿要求是非空包名和至少
一个已分类非空密码；用户名可空，page title 当前固定为 null。

- 已有完整 `Authenticated` 会话：验证草稿后直接写入。
- 否则：将草稿密封写入耐久 Autofill Inbox；写入成功后调用 `SaveCallback.onSuccess()`，不启动认证 Activity。
  Vault 下次完整解锁后由 Inbox drainer 导入；回调表示已耐久排队。

平台依据：[Build autofill services](https://developer.android.com/identity/autofill/autofill-services)、
[FillResponse.Builder](https://developer.android.com/reference/android/service/autofill/FillResponse.Builder.html)、
[Dataset.Builder](https://developer.android.com/reference/android/service/autofill/Dataset.Builder)、
[SaveInfo](https://developer.android.com/reference/android/service/autofill/SaveInfo)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 处理旧版 Autofill 填充与保存请求 | `LegacyAutofillService.onFillRequest()`、`onSaveRequest()` | [`LegacyAutofillService.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt) |
| 准备旧版保存流程 | `LegacySaveFlow.prepare()` | [`LegacySaveFlow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/autofill/legacy/LegacySaveFlow.kt) |
| 密封收件箱的模型与处理流程 | [密封收件箱文档](sealed-inbox.md) | [sealed-inbox.md](sealed-inbox.md) |
