# Autofill 安全策略

状态：当前实现；锁定保存通过 Sealed Inbox 持久接受，并在完整 Vault 解锁后幂等导入。

平台流程见 [Autofill 共享填充流程](../features/autofill/fill-flow.md)。本页只定义跨入口安全不变量。

## 信任与授权

- Android 调用方身份只来自当前 framework request；PendingIntent extras 不能重建调用方授权。
- interaction 绑定来源、规范化 scope、实际展示的 Entry IDs 和 process-local 生命周期。
- scope 规范化由 Domain 的 `AutofillScope` 统一拥有：application ID 去首尾空白并按 `Locale.ROOT` 转小写，空值无效；缺失域名与畸形域名分开处理，畸形 scope 在取得解锁 lease 或查询候选前拒绝。
- 网页身份按精确 host 匹配：scheme、port、path 不参与身份；`www.example.com` 与 `example.com` 保持隔离，单个 DNS 根尾点会规范化移除。只接受 ASCII DNS host 和 bracketed IPv6 literal；Unicode IDN 拒绝，IPv6 转小写并保留文字格式，不做 DNS 查询或不同压缩写法合并，拒绝带 zone 的地址。
- 有有效网页域名时，候选查询与保存关联只使用该域名，不把浏览器 application ID 作为网站凭据关联。无网页域名时才按 application ID 查询/保存。DAO 的子串结果仍须通过精确 Domain 规范化匹配；允许未匹配建议时，只在没有精确候选后按设置回退到未关联登录项。
- 候选阶段只读元数据；密码和 OTP 只在最终选择验证后按单条读取。
- `AutofillAuthenticated` 是窄权限会话，不授权普通 Vault 列表、编辑、备份或设置。
- 结果提交边界必须包含 `setResult + finish`；提交开始后禁止 lease 新借用，`finish` 返回后才能关闭临时会话。
- 最后一个已提交结果完成时必须过期未提交结果的遗留 interaction，关闭期间的新请求等待锁定完成。
- 进程死亡、token 过期、scope 改变或重复 continuation 都拒绝，不能从 Intent/SavedState 恢复权限。
- 手动无障碍回退只在用户点击浮动按钮后扫描当前输入窗口；候选选择前仍走 Autofill 认证与 scope 校验。
- 无障碍选择页的启动 Intent 不携带查找 token；页面从同进程的单次会话存储读取请求。
  选择页关闭、原窗口再次活动后，写入前确认原窗口 ID、包名、节点可编辑和 30 秒有效期。待交付明文只在
  进程内保留至多 10 秒，超时即丢弃并取消 interaction。无法确认目标时拒绝填充，不使用剪贴板兜底。

## 保存

完整 Vault 会话可直接保存；锁定时只用 Inbox public key 密封耐久排队，不取得 DEK、SQLCipher session 或读权限，
也不弹出认证。只有 Inbox 入队成功才向 Android 确认接收；完整解锁后按序导入。Queued 与 Imported 是不同阶段，
详见 [Sealed Inbox](../features/autofill/sealed-inbox.md)。

## 数据和日志

密码、用户名、域名、明文包名、interaction ID 和 Trace token 不进入日志。目标包只允许经过 HMAC。异常不读取
message，只保存安全类别和受限快照。PendingIntent、SavedState 和文件名不得携带 secret；Legacy 内部 Intent
携带目标包名和域名仅用于回调后的 scope 再校验，不能凭这些 extras 授权。

Android 平台可以持有 Autofill/Credential 请求上下文，但 Passly 不把这种平台状态描述成应用自己的 durable save。
唯一例外是 Android `AccessibilityNodeInfo.ACTION_SET_TEXT` 强制要求的瞬时 `Bundle` 参数：只在最终
`performAction` 调用现场写入所选字段值，并在 `finally` 中清空该容器；不得放入 Intent、SavedState、日志或持久化。
无障碍入口只保留短时节点引用，不读取输入框当前明文，不从浏览器包名推断网页 origin。
`FLAG_DELAY_SAVE` 用于同一 task 的多屏 Autofill context，不提供应用解锁后的主动消费能力。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 维护 Autofill 授权与交互生命周期 | `AutofillInteractionStore`、`AuthorizeAutofillInteraction` | [`AutofillInteractionStore.kt`](../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillInteractionStore.kt)、[`AuthorizeAutofillInteraction.kt`](../../app/src/main/java/com/aozijx/passly/feature/autofill/core/AuthorizeAutofillInteraction.kt) |
| 在平台交付前后管理结果提交 | `AutofillResultDelivery.commitResult()` | [`AutofillResultDelivery.kt`](../../app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt) |
| 在 Legacy Autofill Activity 重建间 claim 并确认一次性交付 | `AutofillFillViewModel.commitPlatformResult()`、`ActivityResultClaim.commit()` | [`AutofillFillViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillViewModel.kt)、[`ActivityResultClaim.kt`](../../app/src/main/java/com/aozijx/passly/feature/autofill/presentation/ActivityResultClaim.kt) |
| 在敏感数据读取处执行受限字段读取 | `RoomSensitiveFieldRepository.revealMany()` | [`RoomSensitiveFieldRepository.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/entry/RoomSensitiveFieldRepository.kt) |

Legacy Fill 的平台结果 claim 由 Activity-retained `AutofillFillViewModel` 持有，避免配置重建后新 Activity 将同一 StateFlow 终态再次交付。ViewModel 只暂存 discovery/selection 语义结果；Activity 在平台交付时通过 `LegacyResponseFactory` 构造 `FillResponse`/`Dataset`，ViewModel 不持有 Context 或 framework result。选中结果中的凭据 payload 只保留到一次性 callback 成功，之后 `Result`/`Error` 清回 `Initial`；callback 失败时 `ActivityResultClaim` 回滚 claim，Activity 可以取消 interaction 并交付取消结果。Autofill 凭据交付仍先完成平台 callback，再由 `AutofillResultDelivery` 收尾 selection 与 lease。
