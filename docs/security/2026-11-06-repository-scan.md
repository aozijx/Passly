# Passly 全仓安全扫描报告

**扫描 ID：** `5a9b077c-d16d-4ead-b5c5-25b0bc1dd29a`

**审查修订：** `651ae33f552a02601ed5745e7d79aaba2a994586`

**快照摘要：** `codex-security-snapshot/v1:sha256:b8d22a1eaa0e8728193646419eac711ce37db2b81aa41c178f50e9216948e5cd`

**扫描时间：** 2026-11-06

**范围：** Passly 全仓；扫描清单包含 1,632 个文件。

## 结论

扫描报告记录三项低严重度安全问题，涉及 Argon2 导入资源消耗、备份导入内存放大和会话 lease 关闭顺序。Wi-Fi 密码内联显示候选不构成已验证漏洞：当前 Domain 策略和测试明确将其作为解锁会话内的 `SECRET` 读取；显示是否应与复制/二维码采用相同的新鲜认证要求仍是产品/安全策略决策。

人工语义审查覆盖为 partial。该结果不代表全仓逐行人工审查，也没有运行构建、测试、设备、运行时或性能验证。原始扫描报告保存在运行扫描的本机 Codex Security 归档中，不随仓库发布。

## 发现

### 1. 未认证备份头可触发高成本 Argon2 派生

**严重度：低（2026-11-06 扫描快照）。** 当时的单块容器 decoder 在验证 AES-GCM tag 前读取未认证 header 参数并调用 `deriveKey(...)`。当前分块实现仍需在认证首条记录前依据未认证 header 派生密钥；导入范围仍允许 memory `256 * 1024 KiB`、iterations 10、parallelism 8，可能消耗显著设备资源。旧实现类已删除；当前 KDF 资源预算状态见下方后续跟踪记录。

**整改方向：** 在 KDF 执行前应用有设备资源依据的导入专属上限；区分导出强度与导入防滥用限制，并评估文件及重复缓冲造成的峰值内存。详见[安全整改跟踪](../superpowers/plans/2026-11-06-passly-security-scan-followups.md)中的 Task 3。

### 2. 备份导入存在跨格式内存放大

**严重度：低。** 通用文件读取上限为 256 MiB。原生 JSON 会将完整输入转为 String、解析对象并解码 Base64 资源；Bitwarden JSON 会构建完整 JSON tree 并再次序列化/反序列化；加密 archive 会在 ZIP 检查前缓冲、复制和解密 payload。现有单文件/部分 ZIP 限制尚未证明所有格式路径均受统一累计内存预算约束。

**整改方向：** 为各格式设定读取前的字节、记录和累计资源预算，尽可能在完整物化前拒绝超限输入，并验证失败清理。详见[安全整改跟踪](../superpowers/plans/2026-11-06-passly-security-scan-followups.md)中的 Task 4。

**后续状态（2026-11-07）：** Task 4 的代码整改已完成：FileStore、格式 parser 和加密容器采用受限流，JSON/Bitwarden/ZIP 有记录及资源预算；全量 App JVM 单测和架构检查通过。当前仍保留用户选择的 256 MiB 文件上限；该发现描述的是 2026-11-06 扫描修订的实现，不代表当前代码状态。KDF 参数的低设备资源上限仍由 Task 3 单独跟踪。

### 3. lease 取消宽限期后仍可能关闭正在使用的数据库

**严重度：低。** `SessionLeaseGate.seal()` 在 drain 超时后请求取消 lease owner，等待 `POST_CANCEL_GRACE_MS = 100` 后仍会调用通用 `close()`；`Job.cancel()` 本身不能证明操作已退出。当前测试覆盖合作式取消，没有覆盖忽略取消且 lease 仍活动时的关闭竞态；尚未确认生产中存在具体非合作 caller。

**整改结果：** Runtime 等待活动 lease 归零后才关闭资源；忽略取消的操作会让锁定继续等待。屏障回归与当前合同见[Session 关闭与 DEK 擦除审查](../reviews/2026-11-07-session-close-and-dek-wipe.md)。

## 已复核但未判定为漏洞的候选

Wi-Fi `PASSWORD` 当前属于解锁会话可读取的 `SECRET`，不是字段级 `HIGH_SENSITIVITY`；已有测试验证无字段授权时可内联读取。复制和二维码有额外授权，但这一差异本身不能证明内联显示违反现行合同。仍需明确两种披露路径是否应有不同策略，跟踪于[安全整改计划](../superpowers/plans/2026-11-06-passly-security-scan-followups.md)的 Task 1。

诊断堆栈帧的第三方 package/class/method 名称会通过 sanitizer 并出现在用户主动授权的诊断导出中；实现与现行隐私文档一致，扫描未发现帧中包含用户输入或绝对路径的证据。这是进一步数据最小化策略选择，不列为漏洞。
