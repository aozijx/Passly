package com.aozijx.passly.feature.autofill.legacy.service.parser

import com.aozijx.passly.domain.autofill.AutofillDomainNormalization
import com.aozijx.passly.domain.autofill.AutofillScope

internal data class AutofillWebOriginResolution(
    val domain: String?,
    val ambiguous: Boolean,
)

/** Canonicalizes all web-domain claims in one AssistStructure instead of trusting the first field. */
internal object AutofillWebOriginResolver {
    fun resolve(
        claims: Iterable<String?>,
        structureTruncated: Boolean = false,
    ): AutofillWebOriginResolution {
        val parsedClaims =
            claims.mapNotNull { claim ->
                val raw = claim?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                raw to AutofillScope.normalizeDomain(raw)
            }
        if (parsedClaims.isEmpty()) return AutofillWebOriginResolution(domain = null, ambiguous = false)

        val identities =
            parsedClaims.mapTo(linkedSetOf()) { (raw, normalized) ->
                when (normalized) {
                    AutofillDomainNormalization.Missing -> "missing"
                    AutofillDomainNormalization.Invalid -> "invalid:${raw.lowercase()}"
                    is AutofillDomainNormalization.Valid -> "host:${normalized.host}"
                }
            }
        val first = parsedClaims.first()
        val domain =
            when (val normalized = first.second) {
                AutofillDomainNormalization.Missing -> null
                AutofillDomainNormalization.Invalid -> first.first
                is AutofillDomainNormalization.Valid -> normalized.host
            }
        return AutofillWebOriginResolution(
            domain = domain,
            ambiguous = identities.size > 1 || structureTruncated,
        )
    }
}
