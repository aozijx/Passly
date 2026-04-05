# Passly 备份加密机制升级指南 (V2)

市面上的主流密码管理器（如 Bitwarden, 1Password, KeePassXC）在处理备份加密时，早已不再使用简单的对称加密，而是采用 **“强 KDF（密钥派生函数）+ 认证加密（AEAD）”** 的组合。

## 现状分析：为什么目前的加密“太弱”？

1.  **密钥派生太快**：如果直接用用户密码的 MD5/SHA 结果当密钥，黑客可以利用 GPU 轻松爆破。
2.  **缺乏完整性校验**：使用 AES-CBC 等模式时，攻击者虽然看不见内容，但可以篡改密文，甚至通过“填充预言攻击”解密。

---

## 行业标准方案：AES-256-GCM + Argon2id

这是目前安全界最推荐的黄金组合：

### 1. AES-256-GCM (认证加密)
*   **为什么用**：它不仅提供加密（Confidentiality），还自带“签名”（Authenticity）。如果备份文件被篡改哪怕 1 个字节，解密时会直接报错。
*   **优点**：有效防止“重放攻击”和“密文篡改”。

### 2. Argon2id (密钥派生函数)
*   **为什么用**：它是密码哈希大赛（PHC）的冠军。相比于传统的 PBKDF2，它具有 **“内存硬性”**。
*   **优点**：黑客无法通过廉价的显卡（GPU）或专用矿机（ASIC）进行大规模并行爆破，因为 Argon2id 运行时会占用大量内存，让爆破成本增加数千倍。

---

## 升级路线对比

| 维度       | 弱方案 (不推荐)         | 专业方案 (V2 建议)                              |
|:---------|:------------------|:------------------------------------------|
| **算法模式** | AES-CBC / AES-ECB | **AES-256-GCM**                           |
| **密钥派生** | SHA-256(password) | **Argon2id** (推荐) 或 PBKDF2-HMAC-SHA512    |
| **迭代次数** | 1 次               | Argon2: 迭代 3, 内存 64MB / PBKDF2: 600,000 次 |
| **随机性**  | 固定 IV / 无 Salt    | **随机 Salt + 随机 Nonce (IV)** (存储于文件头)      |
| **物理形式** | 纯文本 JSON          | **二进制封装** (Header + CipherText + Tag)     |

---

## Android 项目落地指南

在执行 **Phase 3** 的 `BackupManager` 拆分时，建议按以下步骤引入新逻辑：

### 第一步：引入 Argon2 库
建议引入 Signal 团队维护的轻量级实现：
```gradle
dependencies {
    implementation "org.signal:argon2:0.1.0"
}
```

### 第二步：设计备份文件结构 (Binary Header)
不要只存加密数据，文件应包含一个明文头部，指导程序如何解密：

| 偏移量 | 长度    | 说明                      |
|:----|:------|:------------------------|
| 0   | 1 字节  | 版本号 (例如 `0x02`)         |
| 1   | 16 字节 | Argon2 Salt (随机生成)      |
| 17  | 12 字节 | AES-GCM Nonce/IV (随机生成) |
| 29  | N 字节  | 加密后的 ZIP 数据流 (包含 Tag)   |

### 第三步：派生密钥 (KDF)
```kotlin
// 这里的计算耗时约 0.5s，这正是对抗爆破的关键
val key = Argon2id.deriveKey(
    password = userPassword,
    salt = saltFromHeader,
    iterations = 3,
    memory = 65536, // 64MB
    parallelism = 4
)
```

### 第四步：流式加密核心逻辑
使用 Java Cryptography Architecture (JCA) 处理 ZIP 流：
```kotlin
val cipher = Cipher.getInstance("AES/GCM/NoPadding")
val spec = GCMParameterSpec(128, nonce) // 128位认证标签
cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
```

---

## 工程落地细节与注意事项

### 1. 内存优化 (防止 OOM)
**风险**：AES-GCM 是认证加密模式。在解密时，Java 的 `Cipher`（即使使用 `CipherInputStream`）必须验证整个文件的认证标签（Tag）。如果备份文件（包含大量原图）体积过大，可能导致解密时内存压力激增。

**对策**：
*   **压缩素材**：在保存图片时强制压缩（如单张最大 500KB，最大 1080p）。
*   对于密码管理器，建议保持备份 ZIP 体积在数 MB 以内，避免触发 `OutOfMemoryError`。

### 2. JNI 依赖与 ABI 过滤
`org.signal:argon2` 包含 C 语言原生实现以保证性能。为了防止 APK 体积过度膨胀，请在 `app/build.gradle` 中配置 ABI 过滤：

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }
}
```

### 3. 核心胶水代码
将 Argon2 派生的 `ByteArray` 包装为 AES 密钥：
```kotlin
// 推荐派生 32 字节 (256 bits) 的 key
val secretKey = SecretKeySpec(key, "AES")
```

### 4. 密码学安全的随机数
生成 Salt 和 Nonce 时，**严禁使用** `java.util.Random`。必须使用 `SecureRandom`：
```kotlin
val secureRandom = SecureRandom()
val salt = ByteArray(16)
val nonce = ByteArray(12)
secureRandom.nextBytes(salt)
secureRandom.nextBytes(nonce)
```

---

## 🔗 与 REFACTOR-PLAN.md 的关联

在 `refactoring-plan.md` 的 **Section 6.1 (格式策略)** 中补充：

*   **新版本加密方案**：
    *   **KDF**: Argon2id (iterations=3, memory=64MB)
    *   **Encryption**: AES-256-GCM
    *   **Payload**: ZIP (包含 data.json 和 images/ 目录)

---

**总结**：这一套选型已经达到了行业顶级安全水平（与 1Password 处于同一维度）。只要注意流式处理中的内存控制和随机数安全性，该备份模块将无懈可击。
