package com.aozijx.passly.feature.autofill.internal.matcher

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.AutofillHintProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutofillHintProviderImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AutofillHintProvider {

    private val usernamePatternLazy by lazy { buildRegex(R.array.autofill_username_keywords) }
    private val passwordPatternLazy by lazy { buildRegex(R.array.autofill_password_keywords) }
    private val otpPatternLazy by lazy { buildRegex(R.array.autofill_otp_keywords) }
    private val submitPatternLazy by lazy { buildRegex(R.array.autofill_submit_keywords) }
    private val searchPatternLazy by lazy { buildRegex(R.array.autofill_search_keywords) }
    private val confirmationPatternLazy by lazy { buildRegex(R.array.autofill_confirmation_keywords) }

    private val hintRoleMapLazy by lazy {
        val mappings = context.resources.getStringArray(R.array.autofill_hint_role_mappings)
        mappings.associate { mapping ->
            val parts = mapping.split(":")
            val hint = parts[0]
            val roleName = parts[1]
            hint to FieldRole.valueOf(roleName)
        }
    }

    override fun getUsernamePattern(): Regex = usernamePatternLazy
    override fun getPasswordPattern(): Regex = passwordPatternLazy
    override fun getOtpPattern(): Regex = otpPatternLazy
    override fun getSubmitPattern(): Regex = submitPatternLazy
    override fun getSearchPattern(): Regex = searchPatternLazy
    override fun getConfirmationPattern(): Regex = confirmationPatternLazy
    override fun getHintRoleMap(): Map<String, FieldRole> = hintRoleMapLazy

    private fun buildRegex(arrayResId: Int): Regex {
        val keywords = context.resources.getStringArray(arrayResId)
        val pattern = keywords.joinToString("|") { "(?:$it)" }
        return Regex(pattern, RegexOption.IGNORE_CASE)
    }
}
