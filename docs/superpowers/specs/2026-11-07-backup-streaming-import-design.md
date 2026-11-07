# Backup 端到端流式重构设计

**状态：** 已实施。容器保留数字版本号 `1`，载荷采用分块 AEAD record；旧单块 v1 布局不兼容、不导入。实现后的当前合同见[Archive v1 格式说明](../../features/backup/passly-archive-v1.md)和[实施记录](../plans/2026-11-07-backup-streaming-implementation.md)。

## 目标

保留 256 MiB 单文件上限及现有 Passly JSON、Bitwarden JSON 领域数据模型，把备份导入、导出、格式 Adapter、ZIP 和 FileStore 的整档 `ByteArray` 路径整体切换为流式 I/O。Passly 加密容器使用分块 AES-GCM record 格式；移除旧单块容器和全量 buffer API，不保留双路、fallback 或兼容转发。旧 v1 加密备份在本重构后不再支持导入。导入最终只持有 restore 所需的 `BackupBundle` 和受限资源数组。

## 非目标

- 不改变稳定格式 ID、JSON 字段名、归档 key、KDF 算法标识或 keyed-entry wire keys；容器 magic/version 仍为 `PSLYBKP1`/`1`，但其加密载荷采用分块 record 格式。
- 不降低输入文件 cap，也不写临时明文文件。
- 不保留旧单块 AES-GCM codec、完整文件 `ByteArray` adapter、双路 API、fallback 或兼容层。数字版本号虽仍为 1，旧单块 v1 文件不能导入。
- 不声称精确限制 JVM 对象图驻留字节数；以输入/资源/记录数上限和固定流 buffer 限制处理字节，最终领域对象仍由记录数及内容决定。

## 重构开始时的基线（2026-11-07）

`BackupFileStore.readBytesSafely()` 将最多 256 MiB 文件返回完整 `ByteArray`。Service 把完整 payload 交给 Adapter，export Adapter 也返回完整 encoded `ByteArray`。Passly JSON 构建全文 UTF-8 `String` 和每个 Base64 资源 `String`；Bitwarden JSON 构建完整 `JsonElement`，再重序列化并解析 DTO。旧 encrypted import 还会同时保留整份密文、JCA AES-GCM `doFinal()` 返回的整份 ZIP 和 ZIP 解压资源。现有限额包括输入 256 MiB、Passly 资源单项 16 MiB/累计 128 MiB、加密归档 document 16 MiB/ZIP 单项 16 MiB/解压累计 128 MiB、ZIP 100,001 entries、JSON 100,000 records；部分累计检查在完整分配后执行。

## 方案

### 实现职责划分

- `io/LimitedInputStream.kt` 与新增的 `io/LimitedOutputStream.kt` 分别负责单一方向的字节预算；`BackupFileStore` 只打开受限流，不负责格式和编码。
- `format/encrypted/BackupContainerHeader.kt` 只拥有 canonical header 的编码、解码和 KDF 参数校验。`format/encrypted/BackupRecordCrypto.kt` 复用 record 常量、nonce/AAD 编码和 JCA 操作；`ChunkedBackupInputStream.kt` 与 `ChunkedBackupOutputStream.kt` 分别拥有输入和输出状态、terminal 与 EOF 验证。不要增加只转发调用的 facade，也不要把旧 ByteArray codec 留作第二条路径。
- `format/encrypted/BackupZipCodec.kt` 只读写 ZIP entry 和资源预算；`EncryptedBackupImporter` / `EncryptedBackupExporter` 编排 ZIP、header stream、JSON document 和资源所有权，不复制密码学细节。协议常量集中在 `BackupContainerV1Spec`，头部、record crypto 和输入/输出流各由对应单一 owner 处理。
- `format/json/JsonFormatDispatcher.kt` 负责一次 root token 遍历和格式歧义判定；`JsonBackupImporter` 负责 Passly document 与资源导入，`BitwardenJsonParser` 逐条解析受限 record，再由 Bitwarden adapter 做领域映射。两条路径共享 `JsonStreamSupport` 和 Base64 resource reader，不共享格式规则。
- `BackupArchiveServiceImpl` 只负责 store、adapter、snapshot gateway 的流路由、资源生命周期和操作日志；`BackupFormatRegistry` 负责唯一 adapter 注册及输入前缀探测/回放。不要在 Registry、Adapter 和 Service 各自重复 probe、读取或预算包装。

### 备选方案

1. **端到端流式和分块 AES-GCM（选择，容器版本号仍为 1）**：FileStore、Service、所有 Adapter、JSON/Bitwarden、ZIP 和加密容器同一批切换为 streams。平台 JCA 每个有序 record 独立 `AES/GCM/NoPadding` 操作，每块以 `doFinal()` 校验 tag 后才向 ZIP parser 交付；terminal record 认证整条流的 chunk 数/总字节数并要求物理 EOF 后才允许 restore。不引入 Bouncy Castle；删除旧单块 v1 codec 和导入路径。
2. **Bouncy Castle 单一长 GCM 流（舍弃）**：引入新密码库；GCM streaming 更新会在最终 tag 校验前输出明文，需要暂存整份明文或额外验证机制。独立 record JCA 直接在块边界完成认证且无完整密文缓存。
3. **仅在当前 `ByteArray` 内做分块解码（舍弃）**：完整原始文件仍需驻留，全文 String/tree 与整份 ZIP 副本仍存在。
4. **临时明文文件中转（舍弃）**：解密 ZIP 和 JSON 会落盘，新增明文残留和本地敏感数据边界。

### 存储、Service 与格式分派

- `BackupFileStore` 只暴露有界输入流/输出流合同；调用边界拥有并关闭 stream。删除 `readBytesSafely()`、`writeBytes()` 和只服务它们的全量数组封装。
- Service 在同一计数边界内读取/写入，所有 adapter probe/decode/encode 改为流接口；输入/输出达到 256 MiB 上限前继续读取/写入允许，下一字节到来前拒绝，输出 cap 计算容器 header、全部 record metadata/tag 和 terminal。
- Registry 不得只看文件前缀识别 JSON：目前 adapter 会根据 root fields 给整份 payload 评分，marker 可以出现在任意 root 属性位置。JSON 由唯一的 root-aware streaming dispatcher 在一次解析中构造最终候选 Bundle，不把已消费 token 移交给另一个 reader；属性顺序不敏感，忽略嵌套诱饵，并拒绝有歧义的 root shape。root `document` 是 Passly 判别字段，root `items` 是 Bitwarden 判别字段；仅在 root 层出现 `document` 和 `items` 两者时报告歧义。`resourcesBase64` 可先于 `document`，解析成受既有 16/128 MiB 限制的 Passly resource accumulator；Bitwarden `folders` 可先于 `items`，保留受 100,000 条记录上限约束的必要 folder ID 索引。解析过程中发现另一格式判别字段时立即拒绝歧义。没有强判别字段则报未知格式。这样主体数组可在判别字段之前出现，无需整档缓存、重开流或删除 `format = null` 自动识别；同一 parser 最终返回对应 Bundle。移除旧 `probe(ByteArray)`。

- Passly encrypted header 保留 container magic `PSLYBKP1` 和 container version `1`，header 其余字段由固定宽度 canonical 字节组成：固定 header length、KDF/cipher IDs、Argon2 version/iterations/memoryKiB/parallelism/key bits、salt 长度和 16-byte salt、4-byte nonce prefix、固定 1 MiB chunk size、固定 16-byte tag 长度。container version 固定为 1；Argon2 version 单独写入运行参数中的实际值，由 Argon2 参数类型按支持的版本验证，不能把它硬编码成 container version。unsigned integers 均为 big-endian；header length 与字段常量逐项精确核验。所有字段在分配或派生密钥前做静态硬上限校验；当前 KDF cap 为 iterations `1..10`、memory `8..262144 KiB`、parallelism `1..8`。每次 export 使用新 salt 和随机 nonce prefix。数字版本号仍为 1 不代表兼容原单块 v1 payload；旧布局在 frame 解析时拒绝。
- 每个 frame 布局固定为 `type(1 byte) | sequence(8 byte) | plaintextLength(4 byte) | ciphertext(plaintextLength) | tag(16 byte)`。整数为 big-endian。Data type 的明文长度 `1..1 MiB`；Nonce 为 `4-byte header prefix || 8-byte big-endian sequence`；AAD 为完整原始 header 字节拼接该 frame 固定 header。Sequence 从 0 连续递增，重复、倒序、跳号、溢出、未规定 type 均拒绝。
- 唯一 terminal frame 使用保留 type、长度 16；AEAD 加密的 16-byte 明文是 `totalDataFrameCount(8-byte big-endian) || totalPlaintextBytes(8-byte big-endian)`。Terminal 用下一个 sequence 独立 nonce，AAD 编码仍是完整 header 加 frame header。空容器仍必须有 terminal。Terminal tag 通过后必须确认底层输入是物理 EOF；缺 terminal、总量不符、末尾添加任意数据均失败。ZIP parser 达到 ZIP EOF 后，Service 继续 drain record decoder 到 terminal/EOF，不能提前 restore。
- Importer 每次只读一个有界 frame；校验 sequence、长度、tag 后才把明文块写进 Service 内的私有受限 ZIP/parser。即使各 chunk 已认证，Bundle 在 terminal 与 EOF 全部验证前也不得触发 restore、数据库/文件写入或任何外部副作用。所有失败/取消路径清理私有 Bundle 资源和暂存块。

### JSON、Bitwarden 与 ZIP 流式处理

- Passly JSON 用 Jackson token reader 单遍处理 root fields。`resourcesBase64` 经共同 reader 逐项解码并受单项 16 MiB、累计 128 MiB、资源数量及 ID 约束；`document` 子树保留在最多 16 MiB 的可擦除缓冲区后再解码为 `BackupDocument`，不建立整份 package/tree/Base64 String。重复/非法资源键、畸形 UTF-8/Base64 和不完整 JSON 拒绝并清理。
- Bitwarden JSON 按 item-at-a-time 复制一个最多 8 MiB 的 record、解码为单条 DTO 并累积在数量有界的列表，再映射为 canonical bundle；不构建完整 `JsonElement` 或重序列化字符串。第 100,001 item/folder 在解析前拒绝；未知字段跳过其值子树，unsupported fields/types 与非空 attachments/passwordHistory/FIDO2 等现有拒绝规则仍由 adapter 负责。
- ZIP encoder/decoder 全程接 streams。Document 最多 16 MiB；资源单项最多 16 MiB；document 加资源解压总量最多 128 MiB；条目最多 100,001（document + 100,000 resources）。每条限制在追加 accumulator 前执行。
- 资源 reader 对每项使用最多 16 MiB 的可擦除缓冲区，转换为 Bundle 所有的 resource 数组后立即擦除缓冲区；分别检查单项、资源累计与 document+resources 总展开量。禁止建立整份资源拼接副本。
- 所有 export adapter 改为直接写 `OutputStream`：Passly JSON 资源 Base64 按块输出，Bitwarden JSON 按记录输出，Text 写 `Writer`，encrypted ZIP 直接进入分块 AEAD writer。移除旧整档 `encode(): ByteArray`、`probe(ByteArray)`、`decode(ByteArray, ...)` 和 v1 单块加/解密路径。`BackupBundle.resourceData: Map<String, ByteArray>` 仍是最终领域资源边界，不是整档 I/O API。

## 预算

| 阶段 | 上限 | 执行位置 |
| --- | ---: | --- |
| 原始输入文件 | 256 MiB | FileStore 流读取计数器 |
| 完整输出文件 | 256 MiB | 每次写 header/frame/tag 前预留 terminal 空间，超限先拒绝 |
| AEAD data chunk | 1 MiB plaintext + 16-byte tag | frame 读取校验长度后才分配 |
| Passly JSON/ZIP 单资源 | 16 MiB | 每次解码/追加前 |
| Passly JSON 资源累计 | 128 MiB | Base64 结果写入前 |
| ZIP document JSON | 16 MiB | entry 分块读取 |
| ZIP 解压合计 | 128 MiB | document + 全部资源，每次追加前 |
| ZIP entry 数 | 100,001 | parser 在第 100,002 个前拒绝 |
| JSON records | 100,000 | parser 在构造第 100,001 个前拒绝 |
| Argon2 header | 现有限额 | KDF 参数在 derive 前校验 |

`BackupBundle` 的领域记录/字段是恢复所需的最终对象图。正式文档说明这是有界输入与资源并减少大副本，不是 JVM heap 精确上限。

## 密码学边界

Android Conscrypt 的 AES-GCM `update()` 会缓存输入直到 `doFinal()`；Oracle 也指出 `CipherInputStream` 可能不会重抛完整性异常。因此不使用 provider 长流或 `CipherInputStream`。每个有界 record 单独调用平台 JCA AES-GCM `doFinal()`，只有 tag 通过才释放该块；terminal 认证完整长度和 frame 数并要求物理 EOF。实现源与合同：[AOSP Conscrypt `OpenSSLAeadCipher`](https://android.googlesource.com/platform/external/conscrypt/%2B/22595bfd47b61fc496b95061a08e79c4d4b5bfd0/common/src/main/java/org/conscrypt/OpenSSLAeadCipher.java)、[Oracle `CipherInputStream`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/javax/crypto/CipherInputStream.html)。

## 验收

- 生产 adapter 全部使用 InputStream/OutputStream；静态搜索确认无整档 `ByteArray` 通路、v1 encrypted implementation、双路 fallback 或兼容转发；资源数组只在 Bundle/domain boundary 保留。
- 256 MiB 输入恰好达到上限可读；第 256 MiB+1 字节在累积前失败并关闭流。输出上限包含 header、record metadata/tag 和 terminal，并在写超限帧前失败。
- version 1 分块加密往返、wrong password/KDF 越界、header tamper、chunk tag tamper、跨块重排、重复/跳号序号、缺 terminal、total length/count 篡改、错误 frame length、尾部数据、截断、取消与清理覆盖；旧单块 version 1 fixtures/实现不再保留。
- JSON/ZIP 的单项/累计边界值与越界、畸形 UTF-8/Base64/JSON/ZIP、重复资源、属性重排/嵌套诱饵、未知格式、Bitwarden 不支持字段和记录数边界均覆盖。资源限额在 accumulator 扩容前执行。
- encrypted ZIP parser 完成但 terminal 无效/缺失/之后有尾随数据时，不调用 restore；认证完整且输入物理 EOF、ZIP/Bundle 校验通过后 restore 才启动。
- 取消/普通异常/fatal `Error` 清理暂存块、Bundle 资源和密码副本；部分 export 文件补偿删除结果沿用 Gateway 已记录合同。
- 更新备份格式/导入文档、验证矩阵及操作生命周期；跑 Adapter/codec/service 定向测试、`:app:testDebugUnitTest`、`verifyArchitecture` 和 `git diff --check`。按 Task 分批提交，隔离其他工作区改动。
\n
