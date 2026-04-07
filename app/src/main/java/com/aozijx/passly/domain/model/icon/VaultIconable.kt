package com.aozijx.passly.domain.model.icon

/**
 * 具有图标显示能力的条目接口
 */
interface VaultIconable {
    val iconName: String?
    val iconCustomPath: String?
    val associatedAppPackage: String?
    val associatedDomain: String?
    val category: String
}
