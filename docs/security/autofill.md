# Autofill 安全策略

状态：当前实现；锁定保存通过 Sealed Inbox 持久接受，并在完整 Vault 解锁后幂等导入。

平台流程见 [Autofill 共享填充流程](../features/autofill/fill-flow.md)。本页只定义跨入口安全不变量。

## 信任与授权

- Android 调用方身份只来自当前 framework request；PendingIntent extras 不能重建调用方授权。
- interaction 绑定来源、规范化 scope、实际展示的 Entry IDs 和 process-local 生命周期。
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
