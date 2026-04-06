package com.aozijx.passly.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.config.DatabaseConfig
import java.io.Serializable

/**
 * 主表：保险库条目 (v5 深度扩展版 - 移除 BLOB 存储)
 * 核心设计：支持结构化存储多种安全凭据，所有敏感字段均预留加密存储空间。
 * 备注：图标等大文件不再存储在数据库 BLOB 中，统一通过 iconCustomPath 引用本地文件系统。
 */
@Entity(
    tableName = DatabaseConfig.TABLE_ENTRIES,
    indices = [
        Index(value = ["favorite", "usageCount", "createdAt"]),
        Index(value = ["category"]),
        Index(value = ["createdAt"]),
        Index(value = ["usageCount"])
    ]
)
data class VaultEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,           // 标题（如：谷歌、招行、公司WiFi）
    val username: String,        // 加密后的用户名或 WiFi SSID
    val password: String,        // 加密后的主密码
    val category: String,        // 分类名称（用于 UI 分组）
    val notes: String? = null,   // 加密后的备注/详细笔记

    // --- 视觉识别 ---
    val iconName: String? = null,       // 预设图标标识符
    val iconCustomPath: String? = null, // 自定义图标/背景图的本地文件路径

    // --- 动态验证 (TOTP) ---
    val totpSecret: String? = null,     // 加密后的 Base32 密钥
    val totpPeriod: Int = 30,           // 步长 (秒)
    val totpDigits: Int = 6,             // 验证码位数
    val totpAlgorithm: String = "SHA1", // 散列算法

    // --- Passkey & 硬件安全 ---
    val passkeyDataJson: String? = null, // 加密后的 WebAuthn 凭据数据 (JSON)
    val recoveryCodes: String? = null,    // 加密后的离线恢复码 (JSON 数组格式)
    val hardwareKeyInfo: String? = null,  // 关联的硬件密钥备注 (如 "YubiKey 5C")

    // --- WiFi 特化配置 ---
    val wifiSecurityType: String? = "WPA", // WiFi 加密协议 (WPA, WEP, nopass)
    val wifiIsHidden: Boolean = false,       // 是否为隐藏 SSID

    // --- 金融与证件加固 ---
    val cardCvv: String? = null,         // 加密后的信用卡三位安全码
    val cardExpiration: String? = null,  // 加密后的有效期 (MM/YY)
    val idNumber: String? = null,        // 加密后的证件号码 (身份证/护照)

    // --- 支付与金融 ---
    val paymentPin: String? = null,         // 加密后的支付专用 PIN / 6位数字
    val paymentPlatform: String? = null,    // 支付渠道标识 (如 "Alipay", "WeChat", "PayPal"，用于 UI 图标匹配)

    // --- 密保与恢复 ---
    val securityQuestion: String? = null,   // 安全问题 (如：您母亲的姓名)
    val securityAnswer: String? = null,     // 加密后的安全问题答案

    // --- 技术凭据管理 ---
    val sshPrivateKey: String? = null,   // 加密后的 SSH 私钥内容
    val cryptoSeedPhrase: String? = null,// 加密后的区块链助记词 (12/24词)

    // --- 条目元数据 ---
    /**
     * 明确条目类型，驱动 UI 模板切换：
     * 0:密码, 1:TOTP, 2:Passkey, 3:WiFi, 4:恢复码/备注, 5:银行卡, 6:助记词, 7:证件, 8:SSH密钥
     */
    val entryType: Int = 0,

    // --- Autofill 自动填充引擎数据 ---
    val associatedAppPackage: String? = null, // 关联的 Android 包名
    val associatedDomain: String? = null,     // 关联的 Web 域名
    val uriList: List<String>? = null,        // 多 URI 匹配列表 (自动应用 Converters)
    val matchType: Int = 0,                   // 匹配规则 (0:精确, 1:主机名, 2:根域名)
    val customFieldsJson: String? = null,     // 加密后的自定义键值对 (JSON)
    val autoSubmit: Boolean = false,          // 填充后是否尝试自动提交

    // --- 安全审计与多媒体 ---
    val strengthScore: Float? = null,          // 密码强度评分 (0.0 - 1.0)
    val lastUsedAt: Long? = null,              // 最后一次填充/查看的时间戳
    val usageCount: Int = 0,                   // 累计使用次数

    // --- 通用管理 ---
    val favorite: Boolean = false,             // 是否标记为收藏
    val tags: List<String>? = null,            // 标签列表 (自动应用 Converters)
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val expiresAt: Long? = null                // 凭据到期时间 (提醒更换密码)
) : Serializable
