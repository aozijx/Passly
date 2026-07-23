package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.summary.title.isBlank()) return "WiFi 标题不能为空"
        if (entry.summary.username.isBlank()) return "SSID 不能为空"
        if ((entry.secret as? EntrySecret.Wifi)?.data?.password.isNullOrBlank()) return "WiFi 密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (((entry.secret as? EntrySecret.Wifi)?.data?.password?.length
                ?: 0) < 8
        ) return "WiFi 密码长度应在 8 位及以上"
        return null
    }
}
