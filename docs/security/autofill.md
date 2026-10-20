# Autofill 安全策略

状态：过渡中；当前读取/保存安全边界已实现，sealed Inbox 尚未实现。

平台流程见 [Autofill 功能文档](../features/autofill/README.md)。本页只定义跨入口安全不变量。

## 信任与授权

- Android 调用方身份只来自当前 framework request；PendingIntent extras 不能重建调用方授权。
- interaction 绑定来源、规范化 scope、实际展示的 Entry IDs 和 process-local 生命周期。
- 候选阶段只读元数据；密码和 OTP 只在最终选择验证后按单条读取。
- `AutofillAuthenticated` 是窄权限会话，不授权普通 Vault 列表、编辑、备份或设置。
- 结果提交边界必须包含 `setResult + finish`；提交开始后禁止 lease 新借用，`finish` 返回后才能关闭临时会话。
- 最后一个已提交结果完成时必须过期未提交结果的遗留 interaction，关闭期间的新请求等待锁定完成。
- 进程死亡、token 过期、scope 改变或重复 continuation 都拒绝，不能从 Intent/SavedState 恢复权限。

## 保存

当前锁定保存会认证；保存 interaction 与 fill interaction 相互独立。保存草稿不能因为 Android 已显示保存 UI 就被
视为已获 Passly 授权。当前 Legacy 内存草稿不落盘，进程死亡后失败。

批准的 Inbox 目标只提供 public-key 密封写入：锁定状态不取得 DEK、SQLCipher session 或读权限。Queued 与
Imported 必须区分，详见 [Sealed Inbox](../features/autofill/sealed-inbox.md)。

## 数据和日志

密码、用户名、域名、明文包名、interaction ID 和 Trace token 不进入日志。目标包只允许经过 HMAC。异常不读取
message，只保存安全类别和受限快照。PendingIntent、Bundle、SavedState 和文件名均不得携带 secret/scope 明文。

Android 平台可以持有 Autofill/Credential 请求上下文，但 Passly 不把这种平台状态描述成应用自己的 durable save。
`FLAG_DELAY_SAVE` 用于同一 task 的多屏 Autofill context，不提供应用解锁后的主动消费能力。
