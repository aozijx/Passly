# Passly Backup 后续待办

**2026-11-07 复核/排序：** 已完成的 Task 1/3/4/5 从本待办移除，其实现合同与验证记录分别留在[导入工作流](../../features/backup/import.md)、[备份验证](../../features/backup/verification.md)和[操作生命周期](../../features/backup/operation-lifecycle.md)。剩余仅 Task 2，按实现依赖先迁移 FileStore/Service/Adapter 流接口，再迁移格式解析与输出。

**状态：** Task 2 已批准实施：保留数字容器版本 1，使用分块 AEAD 载荷并删除旧单块布局导入；按实现计划逐批完成。Task 4 的内存边界评估已完成并迁入操作生命周期文档。

## Task 2：将备份导入与导出迁移到端到端流式 I/O

- [ ] 执行 [`流式备份实施计划`](2026-11-07-backup-streaming-implementation.md)：容器 magic/version 保留 `PSLYBKP1`/1，载荷采用分块 AEAD；root `document`/`items` 单遍分派、完整流式 I/O、迁移测试与旧单块路径删除。
- [ ] 流式解析时执行输入、记录数、单资源和累计资源限制；失败、取消、认证失败与部分恢复路径清理临时数组，且只有完整认证和 EOF 校验成功后才启动 restore。
- [ ] 更新正式格式/导入文档与验证矩阵；覆盖容器认证、截断/尾随数据、错误密码/KDF 参数、资源边界、取消和清理；旧单块 version 1 加密文件不提供兼容导入。

**当前证据：** 当前 `BackupFileStore.readBytesSafely()` 返回受 256 MiB 限制的完整 `ByteArray`；Adapter probe/decode/encode 与 `BackupArchiveServiceImpl` 继续持有完整 payload。`DefaultBackupOperationGateway.performImport()` 将 `format = null` 传给 `BackupImportRequest`；Service 当前读取整档后调用 score-based `probe(payload)`。现有探测会把嵌套同名字段当候选信号。已批准设计规定 JSON 单遍 root dispatcher 在同一 parser 中构造最终 Bundle：只认 root `document`/`items`，忽略嵌套诱饵，root 两者并存则拒绝；resources/folders 前置时在既有资源/记录上限内累积，保留 `format = null` 自动识别。Passly JSON 当前构造完整文本和 Base64 字符串，Bitwarden 先建完整 `JsonElement` 并重序列化，加密导入同时持有密文、整份 ZIP 与解压资源。现有单资源 16 MiB、资源累计 128 MiB、文档 16 MiB、ZIP 解压累计 128 MiB、100,000 条记录限制将迁移到分块读取/追加前检查。
