package com.aozijx.passly.core.crypto

/** Protects attachment bytes while keeping key ownership inside the security boundary. */
interface AttachmentContentProtector {
    suspend fun contentId(content: ByteArray): String

    suspend fun encrypt(content: ByteArray, resourceId: String): ByteArray

    suspend fun decrypt(encrypted: ByteArray, resourceId: String): ByteArray

    suspend fun verifyContentId(content: ByteArray, resourceId: String): Boolean
}