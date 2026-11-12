package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.observability.ReasonCode

internal class BackupIntegrityException(val reasonCode: ReasonCode) : IllegalArgumentException()
