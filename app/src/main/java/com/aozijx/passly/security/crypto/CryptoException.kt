package com.aozijx.passly.security.crypto

/**
 * 加密相关异常。
 */
sealed class CryptoException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** GCM 认证标签验证失败：密文可能损坏或密钥不匹配 */
    class TagVerificationFailed(message: String, cause: Throwable? = null) :
        CryptoException(message, cause)
}