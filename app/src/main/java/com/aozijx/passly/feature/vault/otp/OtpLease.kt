package com.aozijx.passly.feature.vault.otp

/** Idempotent ownership of one actively displayed OTP entry. */
fun interface OtpLease : AutoCloseable {
    override fun close()
}
