package com.aozijx.passly.domain.authentication

object AppPasswordPolicy {
    const val MIN_LENGTH: Int = 6

    fun acceptsLength(length: Int): Boolean = length >= MIN_LENGTH
}
