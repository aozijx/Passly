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

`entry_secrets` 已拆成普通 `secretBlob` 与高敏 `highSensitivityBlob` 两个 AES-GCM blob。普通详情读取通过
`EntryQueryRepository.getByIdWithoutHighSensitivity()`，不会解密高敏 blob。高敏字段必须走
`EntryHighSensitivityRepository.getHighSensitivitySecretForReveal()`，并且只能由完成 reveal 认证的流程调用。

当前实际迁出的高敏字段是银行卡卡号、CVV 和支付 PIN。它们不会在普通详情读取时进入 `VaultEntry.secret`；
详情页 reveal 后只把本次获授权的字段值放入 `DetailUiState.revealedFields`。Domain/Payload 已预留更多高敏
字段结构，但 SSH、助记词、Passkey、OTP 等 UI reveal 链路仍需逐类迁入。

两 blob 方案提供的是“普通敏感 / 高敏感”分层，不是字段级独立 blob。解密 CVV 时会解开同一条目的
`highSensitivityBlob`，因此同 blob 内的其他高敏字段会短暂存在于 Repository 层内存中。若未来需要
“只解密单个字段”，再演进到字段级密文记录。

后续更理想的方向是增加 `EntryDetailMetadata` 与 `readSecretField(entryId, fieldKey)`：

1. 初次进入只加载 header、summary 和 capability flags；
2. 认证成功后按 entry ID 和字段 key 读取对应密文；
3. 在 Repository 内提取请求字段，立即丢弃聚合对象；
4. UI 只保存已获授权的单字段值，并在隐藏/离开/锁定时清除。

自动填充已经使用更窄的两阶段路径：候选阶段不读取 `entry_secrets`；用户选择并完成所需认证后，才按 ID
解密所选的单条凭据。未关联候选可能暴露标题、用户名、域名或包名，但不会在候选阶段提前解密多条密码。

## 剪贴板

Passly 复制敏感内容时使用敏感剪贴板标记。应用进入后台或详情页关闭时，只清除仍由 Passly 写入的剪贴板；
前台最长保留 60 秒后自动清除。所有权检查避免误删用户随后从其他应用复制的内容。
