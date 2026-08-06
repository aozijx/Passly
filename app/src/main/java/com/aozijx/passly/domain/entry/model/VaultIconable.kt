package com.aozijx.passly.domain.entry.model

interface VaultIconable {
    val iconName: String?
    val iconCustomPath: String?
    val associatedAppPackage: String?
    val associatedDomain: String?
}
