package com.aozijx.passly.domain.settings.port

import java.io.IOException

/** Signals that persisted settings cannot be decoded and must not be replaced with defaults. */
class CorruptSettingsDataException(
    val original: Throwable,
) : IOException("Persisted settings data is corrupt", original)
