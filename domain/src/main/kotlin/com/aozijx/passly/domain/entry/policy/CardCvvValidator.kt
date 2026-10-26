package com.aozijx.passly.domain.entry.policy

/** Optional CVV: absent/blank means clear; supplied values contain exactly 3 or 4 ASCII digits. */
object CardCvvValidator {
    private val pattern = Regex("^[0-9]{3,4}$")

    fun isValid(value: String?): Boolean = value.isNullOrBlank() || pattern.matches(value)
}
