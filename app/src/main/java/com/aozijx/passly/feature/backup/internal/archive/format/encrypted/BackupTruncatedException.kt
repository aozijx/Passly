package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.observability.ReasonCode
import java.io.EOFException

internal class BackupTruncatedException(
    val reasonCode: ReasonCode,
) : EOFException()
