# Backup 端到端流式重构实施计划

**设计依据：** [`2026-11-07-backup-streaming-import-design.md`](../specs/2026-11-07-backup-streaming-import-design.md)（用户已审核批准，Task 2）。

**执行状态：** 实施计划待审核。按 Task 2 整体切换，不保留旧/新双路 API；生产代码、迁移测试和正式文档分成同一 Task 的实现提交与文档提交。

## 实施约束

- Passly 加密备份迁移到新分块 AES-GCM record 协议；删除旧 v1 全量密文/明文 `ByteArray` codec 与旧 v1 文件导入支持，不添加 fallback/兼容转发。
- 文件 cap 仍为 256 MiB；资源和 ZIP cap 按设计执行，且全部在分配/扩容前检查。
- 平台 JCA 按每个有序 record 建立 AES-GCM 操作；tag 校验通过前不得向 ZIP parser 交付该块。terminal 和物理 EOF 校验完成前不得调用 restore。
- 先写失败用例再实现对应路径；每个变更检查现有调用者、DI、test fakes、格式文档与 source-boundary。不要留下旧 `ByteArray` 通路或临时兼容层。
- JSON 使用最小范围的 Jackson Core streaming API，不引入 databind/Kotlin module，不使用 kotlinx serialization internal API，也不实现手写 UTF-8/JSON tokenizer。依赖固定到 Jackson Core `2.22.4`（本计划日期的稳定 2.22 patch）；只在 app 实现模块添加 `jackson-core` catalog alias。
- 不触碰与 Backup 无关的 dirty files；Local `.agents/skills/` 与 `AGENTS.md` 保持未暂存。

## Task 2.1：先固定有界 I/O 与新加密协议

**目标文件：**

- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/io/BackupFileStore.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/io/LimitedInputStream.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/AndroidBackupFileStore.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/EncryptedBackupContainerCodec.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/BackupArchiveCodec.kt`
- `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt`
- 新增 `BackupChunkedContainerCodecTest.kt` 与必要的 `LimitedInputStreamTest.kt`

**步骤：**

1. 先为输入恰好 256 MiB/多 1 byte、输出完整容器 cap、短读/取消/流关闭写边界测试；为 chunk tag、序号、terminal、EOF、KDF 范围、帧长度与数组清理写失败用例。
2. FileStore 改为受限 `openInput(uri)` 与受限输出流回调，Android 实现直接包住 ContentResolver stream；删除 `readBytesSafely()` / `writeBytes()`。
3. 将 `EncryptedBackupContainerCodec` 整体替换为唯一的 v2 canonical header + frame codec：validate-before-allocation、每 record 新建/初始化 JCA GCM 操作、固定 1 MiB chunk、独立 16-byte tag、terminal 统计与 EOF 校验。旧 `FORMAT_VERSION=1` parser/encryptor/decryptor 和完整密文类型一并删除。
4. 将 ZIP codec 改为 `InputStream`/`OutputStream` 逐 entry 读取/写入；每次资源 append 前校验 ZIP entry、文档、单项、累计资源、总展开量和条目数；auth stream 完整 drain 到 terminal 后才返回 Bundle。
5. 输入、输出、plain chunk、cipher/tag、terminal 与 ZIP scratch buffers 在 `finally` 覆盖；失败时清理已创建资源数组。

**验证：** 新 codec round-trip、每种 tag/metadata tamper、record reorder/重复/跳号、长度伪造、terminal 缺失/篡改、terminal 后尾随数据、错误密码、危险 KDF、截断、exact cap/+1、cancel 和 buffer wipe；旧 v1 fixtures/测试路径删除。

## Task 2.2：一次迁移 Service、Registry 与 Adapter 契约

**目标文件：**

- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImpl.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatAdapter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupFormatRegistry.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/BackupProbe.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/di/BackupModule.kt`
- `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImplTest.kt`
- 与上述接口耦合的现有 test fakes/fixtures

**步骤：**

1. 修改 service 编排测试，先证明 import/export 都将 streams 直接交给 store/adapter，成功、普通失败、认证失败、restore 失败和取消时关闭正确的资源并清理 Bundle。
2. 同一切换中把 `encode(...): ByteArray`、`probe(ByteArray)`、`decode(ByteArray,...)` 改成 stream contracts；所有 call sites、fakes、DI binding 一并迁移，不留下 adapter overload。
3. 用一次输入遍历的分派替换全量 probe：固定 encrypted v2 magic 选择 encrypted importer；JSON 根对象由 streaming token parser 在同一遍中识别 Passly/Bitwarden 并生成最终 Bundle。删除 `BackupProbe.containsAscii(ByteArray)` 与旧 score-based payload probing。
4. service 只在 adapter 已完整验证对应格式后才调用 `restore`；加密 adapter 的完成条件是所有 data tag、terminal tag、total count/bytes 和物理 EOF 均通过。
5. export 直接把 adapter 输出接到受限 `OutputStream`；操作返回前沿用 `BackupBundle`/资源缓冲所有权合同。

**验证：** Service 测试覆盖 source/target stream 路由、single-pass format dispatch、restore gate、计数限制、stream close、数组清理、失败报告与取消传播；registry 测试覆盖空白/属性重排/嵌套诱饵/歧义/未知格式。

## Task 2.3：流式 Passly JSON 与 Bitwarden JSON

**目标文件：**

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/BackupJson.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/model/JsonBackupPackage.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/json/JsonBackupImporter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/json/JsonBackupExporter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/json/PasslyJsonFormatAdapter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/bitwarden/BitwardenJsonImportAdapter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/bitwarden/BitwardenExportModels.kt`
- `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt`
- `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveRoundTripTest.kt`
- 相关 Bitwarden import tests；新增 streaming Passly/Bitwarden parser/export tests

**步骤：**

1. 先新增 parser tests：root property 任意顺序、marker 嵌套诱饵、重复/歧义 root keys、未知字段、malformed UTF-8/JSON/Base64、记录上限、资源 exact cap/+1 与失败清理。更新 round-trip/wire test fixtures，使其无需 `BackupJson` 全量 String helper。
2. 将 `jackson-core:2.22.4` 纳入 version catalog/app runtime。创建只依赖 `JsonFactory` / `JsonParser` / `JsonGenerator` 的内部 reader/writer；关闭 databind、Kotlin module、全量 `JsonElement` 和字符串重序列化路径。
3. Passly importer 在 parser token 上逐字段读取 document/entry/link/resource；Base64 resource 通过 parser 的 streaming binary decode 写入受限 accumulator，完整对象或单条 resource String 不落地；记录数/字节预算在建下一项前核验。
4. Bitwarden importer 在 root `items` 数组中逐条映射到最终 Entry records；folders 仅保留必要 ID 索引。识别不支持字段时用 `skipChildren` 但不缓存 subtree；非空 attachments/passwordHistory/FIDO2 按当前拒绝合同即时失败；未知 key 按 `ignoreUnknownKeys` 行为跳过。
5. Passly exporter 用 `JsonGenerator` 逐字段/record 写 document；base64 resource 使用流式 binary writer。删除 `JsonBackupPackage` 与生产中的 `BackupJson.encodeToString`/`decodeFromString`/`parseToJsonElement` 全量序列化路径及仅供其使用的 DTO。
6. 更新现有所有 Backup tests 中对 `BackupJson` 和全量 JSON 字符串的依赖，保留 wire key、field discriminant、默认值、unknown-field 和 round-trip 断言。

**验证：** Passly 与 Bitwarden 保留既有有效文件语义；100,000/100,001 记录边界；资源 16 MiB 与 128 MiB 边界；未知字段和不支持字段合同；生成文本可被独立 Jackson parser 逐 token 读取且输出限制正确。

## Task 2.4：Text 与 encrypted stream Adapter 接线

**目标文件：**

- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/text/ReadableTextFormatAdapter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/text/TextVaultExporter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/text/TextVaultRenderer.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/EncryptedBackupExporter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/EncryptedBackupImporter.kt`
- `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/PasslyEncryptedFormatAdapter.kt`
- `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/format/text/TextVaultRendererTest.kt`
- `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt`

**步骤：**

1. Text exporter 改为 `Writer`/`Appendable` 流式逐条渲染；保留文本格式当前输出内容与 resource/favorite exclusion。
2. Encrypted exporter 把 ZIP writer 接到 chunk encrypting stream；encrypted importer 把 chunk decrypting stream 接到 ZIP parser；确保 ZIP parser close/EOF 行为不跳过 terminal 验证。
3. 删掉 v1 `EncryptedBackupExporter.export(...): ByteArray`、`EncryptedBackupImporter.import(ByteArray,...)`、整 ZIP 临时数组和旧 round-trip fixtures。
4. 资源及密钥在 Adapter、service 和 Gateway 各自清理其所有数组；fatal `Error`、取消原样传播。

**验证：** Text 输出字段顺序/escaping 与排除合同；encrypted ZIP 与 JSON wire round-trip；空资源/空记录、multi-chunk、每块边界、错误 tag/terminal 和部分 output cleanup。

## Task 2.5：完整调用闭包、正式文档与提交

**目标文件：**

- `docs/features/backup/import.md`
- `docs/features/backup/export.md`
- 删除 `docs/features/backup/passly-archive-v1.md`；新增 `docs/features/backup/passly-archive-v2.md` 并修正所有链接/文件名，不保留误导性的 v1 current-contract 文档
- `docs/features/backup/verification.md`
- `docs/superpowers/plans/2026-11-06-passly-backup-review-followups.md`
- `docs/superpowers/plans/2026-10-30-passly-maintainability-todo.md` 中 Task 2 的排序/状态行

**步骤：**

1. 搜索 `readBytesSafely`、`writeBytes`、`encode(...): ByteArray`、`decode(ByteArray`、`probe(ByteArray`、`EncryptedBackupContainerCodec` v1、`BackupJson.*String/JsonElement` 与所有 adapter fake，删除剩余旧生产/测试路径。
2. 对照设计更新格式文档：v2 canonical header/record/nonce/AAD/terminal、KDF cap、输入/输出预算、资源 limits、旧 v1 停止导入事实、tag/EOF 后 restore 的时序。
3. 更新验证矩阵和导入/导出文档，移除已删除类型、v1 fixtures、full-buffer API 与过时测试入口；将 Task 2 标为实现完成并从 TODO 移除，仅在正式 topic docs 留当前实现合同。
4. 执行定向 Adapter/codec/service tests、`:app:testDebugUnitTest --no-build-cache --no-parallel --max-workers=2`、`verifyArchitecture`、`git diff --check`，分别记录实际通过/未运行结果。
5. 审查 staged diff，确认实现 commit 只包含 Backup Task 2 生产代码和测试；正式文档/task 状态用独立 `docs(backup): document streaming v2 archive` commit。其它 checkout 改动保持 unstaged。

## 预计提交批次

1. `refactor(backup): replace full-buffer archive flow with streaming`：Task 2 生产代码、切换后的 adapter/service/codec tests、旧路径删除、依赖版本目录和 DI 一次完成；不提交不完整过渡 API。
2. `docs(backup): document streaming v2 archive`：Task 2 正式格式/验证/导入导出文档和完成后的 TODO 清理。

两次提交都应在实施前确认工作区其他 Task 的 staged 状态；只暂存此处列出的 Backup 路径与明确属于 Backup 的 shared contract。Local skills 与其它 Task 文件不进入提交。
