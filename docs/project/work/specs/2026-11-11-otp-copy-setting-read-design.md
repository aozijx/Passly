# OTP 复制设置读取设计

## 意图

让 `OtpCodeRuntime` 在需要复制 TOTP 时读取当前策略快照，而不是长期收集设置 Flow。OTP 策略有独立的 Domain 模型和 Repository 端口；同一数据源可为设置 UI 提供观察流，也可为 Runtime 提供一次性快照。

## 目标与边界

- 保留当前码/下一窗码预生成，以及最后 3 秒按设置决定复制哪一个的行为。
- 让验证码展示状态只保存 UI 必需的码值和进度，复制阶段在用户操作时计算。
- 不改 HOTP 消费语义；HOTP 复制不需要读取 TOTP 临期偏好。
- Runtime 只在一次授权复制流程进入 TOTP 取码时读取设置，不在 ticker 中每秒访问设置仓库。
- 普通读取异常返回不可用，下一次显式复制重新读取；传播取消和 fatal `Error`。

## 方案

1. **复制时读取 OTP 策略快照（采用）**：Runtime 依赖 `OtpRuntimePolicySnapshotReader.readOtpRuntimePolicySnapshot()`，在 `codeForCopy` 的 TOTP 分支读取快照；从 `LibraryViewSettings`、Runtime 信号、Runtime 缓存和验证码状态中移除复制阶段。设置 UI 观察 `OtpRuntimePolicyRepository.otpRuntimePolicy`。这样设置只保留一个事实源，同时消费者只依赖所需读取方式。
2. **保留 Runtime 缓存并通知失效**：设置页面持久化成功后主动通知 Runtime 更新缓存。它减少复制时一次 Flow 读取，但增加设置写入方到 OTP Runtime 的同步合同；遗漏写入入口会导致行为陈旧。

选择方案 1，因为偏好只影响一次复制选择，验证码展示不需要维护复制阶段。专用 OTP 策略模型/端口避免 Runtime 依赖同时承载排序、卡片外观等无关字段的 `LibraryViewSettings`；Repository 默认从策略 Flow 读取当前快照，Data 层可按其持久化机制覆写读取行为。Runtime 不接触 Flow，只依赖快照读取合同。

## 验收

- 设置开/关的值在每次 TOTP 复制时重新读取，分别选择当前码和下一窗码。
- 设置读取失败不抛出普通异常到授权/页面协程；再次复制会重新读取。
- 取消与 fatal `Error` 保持传播，HOTP 不读取该设置。
- `OtpRuntimeSignals` 只包含需要持续响应的会话信号。
- `LibraryViewSettings` 不承载 OTP 运行策略；Data 层策略映射与设置 ViewModel 使用 `OtpRuntimePolicy`。
