package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.VaultViewPreferences
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy

internal fun VaultViewPreferences.toOtpRuntimePolicy(): OtpRuntimePolicy = OtpRuntimePolicy(copyNextTotpNearExpiry = copyNextTotpNearExpiry)
