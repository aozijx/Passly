package com.aozijx.passly.domain.model

data class VaultSummary(
    val id: Int,
    val title: String,
    override val category: String,
    val entryType: Int = 0,
    val username: String,
    val email: String? = null,
    override val iconName: String? = null,
    override val iconCustomPath: String? = null,
    override val associatedAppPackage: String? = null,
    override val associatedDomain: String? = null,
    val totpSecret: String? = null,
    val totpPeriod: Int = 30,
    val totpDigits: Int = 6,
    val totpAlgorithm: String = "SHA1",
    val favorite: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) : VaultIconable