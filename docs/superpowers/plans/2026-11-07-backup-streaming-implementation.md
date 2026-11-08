# Backup 端到端流式重构实施记录

**设计依据：** [`2026-11-07-backup-streaming-import-design.md`](../specs/2026-11-07-backup-streaming-import-design.md)。用户批准按分块 AEAD 整体切换。

**状态：** 实现、正式文档和 2026-11-08 顺序复核均已完成；历史实现验证和提交记录见下文。容器 magic/数字版本继续使用 `PSLYBKP1`/1；载荷采用独立 JCA AES-GCM records，并删除旧单块布局导入路径。Argon2 参数子版本由参数对象提供，与容器版本分离。

## 最终职责与数据流

- `BackupFileStore` 负责 256 MiB 流预算；archive service 负责操作编排和日志；format registry 负责格式选择与一次性 prefix replay；各 adapter 负责对应格式编码/解码。
- `BackupContainerV1Spec` 保存协议固定值；`BackupContainerHeader`、`BackupRecordCrypto`、`ChunkedBackupInputStream`、`ChunkedBackupOutputStream` 各自负责 header、record 密码操作及输入/输出记录流。ZIP 逻辑归 `BackupZipCodec`。
- JSON 共用 `BackupJsonParserFactory` 的深度/字符串约束；`JsonStreamSupport` 统一 root field 迭代和 token 转移；`JsonFormatDispatcher` 只做 root 格式选择。Passly JSON 和 Bitwarden 分别保留自己的格式映射责任。
- JSON 资源 Base64 由共享有界读取流程处理；受限且会擦除 backing buffer 的 `WipingByteArrayOutputStream` 用于必要的累积边界。Bitwarden 逐条解析并限制 record 大小。
- 参数只在边界语义变化时转换：ViewModel 的 `SensitiveValue` 到操作契约、操作契约到 archive 请求、格式能力到 snapshot read options。保留这些所有权/能力边界；移除重复 JSON root 遍历、重复资源读取与 service 中的 prefix replay 细节，不引入参数透传 facade。
- **模式选择：** `BackupFormatRegistry` 已按稳定格式 ID 选择 exporter/importer（Registry + Strategy），adapter 和 codec 按职责组合成固定顺序的流式管线。当前管线没有可选构建阶段或复杂可变配置，增加 GoF Builder 只会多一层参数转发；顺序通过显式调用链与各阶段边界测试约束。
- 授权仍由 operation gateway 持有；格式协议和 archive service 不检查或传递认证状态。`AppErrorReporter` 统一记录操作错误并继续将失败向上传递；取消和 fatal `Error` 按原合同传播。

## 格式与清理合同

- 输入、输出和解压预算在分配/扩容前执行；不恢复旧完整 payload `ByteArray` 路径。
- 分块 AES-GCM 每个 record 独立认证；认证失败、缺失/篡改 terminal、计数/总字节不符或尾随数据均阻止 restore。
- JSON parser 限制嵌套深度和字符串长度；未知 root 字段跳过其 value，识别只依据 root `document`/`items`。
- 敏感数组和临时资源在异常、取消和操作完成后由其所有者清理。日志记录操作、结果与耗时，不记录口令、URI 或备份内容。

## 验证与提交

- 定向 JSON/Bitwarden/service/I/O/observability 测试：通过。
- `:app:testDebugUnitTest verifyArchitecture --continue --no-parallel --max-workers=2`：通过；App XML 报告 1,024 tests、0 failures、0 errors、0 skipped。
- `git diff --check`：通过。
- 实现与文档分开提交；只暂存 Backup 实现及其明确相关的 observability 路径、对应正式文档。保持 `AGENTS.md`、`.agents/` 和其他无关工作区修改未暂存。

## 2026-11-08 顺序复核续验

- **Header（完成）：** 原 `headerRead_rejectsLegacySingleBlockHeaderLength` 实际把偏移 11 的 container version 改成 88，没有触及偏移 12 的 header length。现改为真实 88-byte 旧头、版本 1、header length 88，并断言因头长度拒绝；canonical header 测试逐项锁定 80-byte header 的 13 个 big-endian 整数字段和 salt/nonce 偏移。预 KDF 无效字段矩阵现包含 header length；另覆盖 magic、container/KDF/cipher IDs、Argon2 version 与硬上限、盐/nonce 长度、chunk/tag 长度，以及固定头/salt/nonce prefix 截断。header 与 stream 测试通过。
- **Record/terminal（完成）：** 覆盖跨块往返、空归档 terminal、data/terminal GCM tag 篡改、重复/跳号/重排序号、未知类型、data/terminal 非法长度、缺失 terminal、terminal record 计数/总字节错误、record header/ciphertext 截断和 terminal 后尾随数据。加密格式、service 与 codec 定向测试整体通过。
- **Import/restore（完成）：** `EncryptedBackupImporter` 在 ZIP 解码后继续 drain 解密流，触发 terminal 认证和物理 EOF 校验后才返回 bundle；service 随后才调用 `snapshotGateway.restore`。新增 service 行为测试：生成有效加密 ZIP，截掉 terminal 后经真实 encrypted importer 导入，断言失败且 restore 未启动。验证命令：`:app:testDebugUnitTest --tests "com.aozijx.passly.feature.backup.internal.archive.format.encrypted.*" --tests "com.aozijx.passly.feature.backup.internal.archive.BackupArchiveServiceImplTest" --tests "com.aozijx.passly.feature.backup.internal.archive.BackupCodecTest" --no-parallel --max-workers=2`，通过。
