package com.aozijx.passly.domain.access.model

data class BiometricBinding(
    val activeAlias: String,
    val invalidateOnEnrollment: Boolean,
)

enum class BiometricRotationPhase { NONE, PREPARED, COMMITTED }

data class BiometricRotationJournal(
    val phase: BiometricRotationPhase,
    val oldAlias: String?,
    val candidateAlias: String,
)

data class BiometricBootstrapState(
    val binding: BiometricBinding?,
    val rotation: BiometricRotationJournal?,
    val cleanupAliases: Set<String>,
)
