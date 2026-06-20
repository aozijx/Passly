package com.aozijx.passly.domain.model

interface VaultIconable {
    val category: String
    val iconName: String?
    val iconCustomPath: String?
    val associatedAppPackage: String?
    val associatedDomain: String?
}