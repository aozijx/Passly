package com.aozijx.passly.security.dek

import java.security.SecureRandom

internal class CapturingSecureRandom(
    private val failingCall: Int? = null,
    private val failure: Throwable? = null,
) : SecureRandom() {
    private var calls = 0
    val buffers = mutableListOf<ByteArray>()

    override fun nextBytes(bytes: ByteArray) {
        calls++
        buffers += bytes
        bytes.fill(calls.toByte())
        if (calls == failingCall) throw requireNotNull(failure)
    }
}
