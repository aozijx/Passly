# Backup 端到端流式重构实施记录

**设计依据：** [`2026-11-07-backup-streaming-import-design.md`](../specs/2026-11-07-backup-streaming-import-design.md)。用户批准按分块 AEAD 整体切换。

**状态：** 实现与正式文档已完成，验证和提交记录见本文末尾。容器 magic/数字版本继续使用 `PSLYBKP1`/1；载荷采用独立 JCA AES-GCM records，并删除旧单块布局导入路径。Argon2 参数子版本由参数对象提供，与容器版本分离。

## 最终职责与数据流

- `BackupFileStore` 负责 256 MiB 流预算；archive service 负责操作编排和日志；format registry 负责格式选择与一次性 prefix replay；各 adapter 负责对应格式编码/解码。
- `BackupContainerV1Spec` 保存协议固定值；`BackupContainerHeader`、`BackupRecordCrypto`、`ChunkedBackupInputStream`、`ChunkedBackupOutputStream` 各自负责 header、record 密码操作及输入/输出记录流。ZIP 逻辑归 `BackupZipCodec`。
- JSON 共用 `BackupJsonParserFactory` 的深度/字符串约束；`JsonStreamSupport` 统一 root field 迭代和 token 转移；`JsonFormatDispatcher` 只做 root 格式选择。Passly JSON 和 Bitwarden 分别保留自己的格式映射责任。
- JSON 资源 Base64 由共享有界读取流程处理；受限且会擦除 backing buffer 的 `WipingByteArrayOutputStream` 用于必要的累积边界。Bitwarden 逐条解析并限制 record 大小。
- 参数只在边界语义变化时转换：ViewModel 的 `SensitiveValue` 到操作契约、操作契约到 archive 请求、格式能力到 snapshot read options。保留这些所有权/能力边界；移除重复 JSON root 遍历、重复资源读取与 service 中的 prefix replay 细节，不引入参数透传 facade。
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
