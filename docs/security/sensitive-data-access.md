# 敏感数据读取、显示与剪贴板

状态：部分完成；本文明确当前边界，不把待办写成既有能力。

## 认证语义

“复制敏感数据时验证”位于隐私设置，语义为：

- 直接复制隐藏字段：按全局开关决定是否要求新鲜认证；
- 显示高敏感字段：始终按字段级别验证；
- 字段已在当前详情页会话中验证并显示：随后复制复用这次显示授权，不重复弹窗；
- 字段再次隐藏、离开详情页、应用锁定或敏感状态清理后，显示授权失效。

显示授权只存在于 `DetailUiState.revealedFields`，不进入 SavedState、数据库或 Intent。

## 当前读取边界

secret 数据位于 `entry_secret_fields`：敏感字段（`PASSWORD`、银行卡号、CVV、支付 PIN 等）按
`entryId + fieldKey` 每字段一个独立 AES-GCM 密文行，低敏结构聚合为 `STRUCT_BUNDLE` 行。
普通详情读取通过 `EntryQueryRepository.getById()` 只解密 `STRUCT_BUNDLE`，字段级值在领域模型中为
`null`。显示字段必须走 `SensitiveFieldRepository.revealMany()` 按需读取对应密文行，并且只能由
完成 reveal 认证的流程调用。

字段级密文行的 AAD 绑定 `entryId + fieldKey`，因此解密 `PASSWORD` 不会加载同条目其他字段的密文；
`readField`/`reveal` 只解密请求的字段，不存在"解一个字段带出整条 secret"的捆绑暴露。完整凭据
只在确实需要全部字段的批量流程（Revision 快照、Backup 快照、恢复扫描、自动填充点选后）通过
`readAll` 组装，组装结果在 Repository 内立即交付，不进入 UI 状态。

当前字段级迁移的键是 `PASSWORD` 以及银行卡号、CVV、支付 PIN、身份证号、助记词、恢复码、
SSH 私钥/口令、Passkey 私钥引用、OTP Secret。它们不会在普通详情读取时进入 `VaultEntry.secret`；
详情页 reveal 后只把本次获授权的字段值放入 `DetailUiState.revealedFields`。Domain/Payload 已预留
更多字段结构，但 SSH、助记词、Passkey、OTP 等 UI reveal 链路仍需逐类迁入（当前 UI 已支持上述键）。

## 剪贴板

Passly 复制敏感内容时使用敏感剪贴板标记。应用进入后台或详情页关闭时，只清除仍由 Passly 写入的剪贴板；
前台最长保留 60 秒后自动清除。所有权检查避免误删用户随后从其他应用复制的内容。
