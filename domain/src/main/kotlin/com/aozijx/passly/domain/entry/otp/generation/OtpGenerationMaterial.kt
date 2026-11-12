package com.aozijx.passly.domain.entry.otp.generation

import java.io.Closeable

/** Owns an encoded OTP seed buffer; callers must close it when its scoped use ends. */
class OtpGenerationMaterial(
    val config: OtpGenerationConfig,
    val secretBytes: ByteArray,
) : Closeable {
    private val wipeMonitor = Any()
    private var closed = false

    /** Creates a short-lived, independently wipeable copy for one generation operation. */
    fun copyForGeneration(): OtpGenerationMaterial =
        synchronized(wipeMonitor) {
            check(!closed) { "OTP generation material is closed" }
            OtpGenerationMaterial(config = config, secretBytes = secretBytes.clone())
        }

    override fun close() {
        synchronized(wipeMonitor) {
            if (!closed) {
                closed = true
                secretBytes.fill(0)
            }
        }
    }
}
