package com.aozijx.passly.domain.model.envelope

@JvmInline
value class KdfAlgorithm(val value: String) {
    companion object {
        val NONE = KdfAlgorithm("none")
        val ARGON2ID = KdfAlgorithm("argon2id")
        val PBKDF2 = KdfAlgorithm("pbkdf2")
    }
}