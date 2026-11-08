package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.crypto.CryptoConfig
import com.aozijx.passly.domain.autofill.model.AutofillInboxLimits
import java.math.BigInteger

internal object AutofillInboxProtocol {
    const val KEY_ID_BYTES = 16
    const val GCM_TAG_BYTES = 16

    object KeyMaterial {
        const val MAGIC = "PASSLYINBOXKEY"
        const val WRAP_LABEL = "passly-autofill-inbox-private-wrap-v1"
        const val RSA_BITS = 3072
        val RSA_PUBLIC_EXPONENT = BigInteger.valueOf(65_537L)
        const val DEK_BYTES = 32
        const val FINGERPRINT_BYTES = 32
        val ENVELOPE_HEADER_BYTES =
            MAGIC.length + 1 + KEY_ID_BYTES + FINGERPRINT_BYTES + CryptoConfig.IV_LENGTH + 4
        const val MIN_RSA_CIPHERTEXT_BYTES = 1
        const val MAX_RSA_CIPHERTEXT_BYTES = 16 * 1024
        const val MAX_PUBLIC_KEY_BYTES = 2 * 1024
        const val ENVELOPE_VERSION: Byte = 1
    }

    object Record {
        const val RECORD_MAGIC = "PASSLYINBOX"
        const val PAYLOAD_MAGIC = "PASSLYDRAFT"
        const val RECORD_VERSION: Byte = 1
        const val PAYLOAD_VERSION: Byte = 1
        const val RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val MGF1 = "MGF1"
        const val U16_BYTES = 2
        const val U32_BYTES = 4
        const val DRAFT_ID_BYTES = 16
        const val RSA_CIPHERTEXT_BYTES = 384
        const val CONTENT_KEY_BYTES = 32
        val RECORD_AAD_HEADER_BYTES =
            RECORD_MAGIC.length + 1 + KEY_ID_BYTES + DRAFT_ID_BYTES + 8
        val MIN_RECORD_BYTES =
            RECORD_AAD_HEADER_BYTES + U16_BYTES + RSA_CIPHERTEXT_BYTES +
                CryptoConfig.IV_LENGTH + U32_BYTES + GCM_TAG_BYTES + 1
        const val MAX_PAYLOAD_BYTES = AutofillInboxLimits.MAX_RECORD_BYTES - 512
        const val MAX_PACKAGE_BYTES = 512
        const val MAX_DOMAIN_BYTES = 4 * 1024
        const val MAX_PAGE_TITLE_BYTES = 8 * 1024
        const val MAX_USERNAME_BYTES = 8 * 1024
        const val MAX_PASSWORD_BYTES = 32 * 1024
        const val NULL_STRING_LENGTH = -1
    }
}
