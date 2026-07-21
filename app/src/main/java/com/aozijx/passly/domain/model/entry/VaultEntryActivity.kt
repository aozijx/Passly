package com.aozijx.passly.domain.model.entry

import com.aozijx.passly.domain.model.activity.VaultActivity

data class VaultEntryActivity(
    val entry: VaultEntry, // 包含基础信息和凭据
    val activities: List<VaultActivity> // 从 act 表查出的记录
)