# Passly Backup 后续待办

**2026-11-07 复核/排序：** 已完成的 Task 1/3/4/5 从本待办移除，其实现合同与验证记录分别留在[导入工作流](../../features/backup/import.md)、[备份验证](../../features/backup/verification.md)和[操作生命周期](../../features/backup/operation-lifecycle.md)。剩余仅 Task 2，按实现依赖先迁移 FileStore/Service/Adapter 流接口，再迁移格式解析与输出。

**状态：** Task 2 设计草案按用户最新决定改为分块 AEAD 新容器，不保留旧 v1 加密文件导入；待审核后拆分实现计划。Task 4 的内存边界评估已完成并迁入操作生命周期文档。

## Task 2：将备份导入与导出迁移到端到端流式 I/O

- [ ] 审核 [`流式备份设计`](../specs/2026-11-07-backup-streaming-import-design.md)，确认分块 AEAD v2 framing、256 MiB 文件预算、KDF 参数边界、ZIP/JSON 资源限制与单遍格式识别。
- [ ] 在审核通过后编写实现计划；先迁移 FileStore、Service 和全部 Adapter 契约，再切换 JSON、Bitwarden、Text 与加密格式的读写实现，删除完整文件 ByteArray API 和旧 v1 单块加密路径，不增加双路 API/兼容转发。
- [ ] 流式解析时执行输入、记录数、单资源和累计资源限制；失败、取消、认证失败与部分恢复路径清理临时数组，且只有完整认证和 EOF 校验成功后才启动 restore。
- [ ] 更新正式格式/导入文档与验证矩阵；覆盖新容器认证、截断/尾随数据、错误密码/KDF 参数、资源边界、取消和清理；旧 v1 加密文件按用户要求不提供兼容导入。

**当前证据：** `BackupFileStore.readBytesSafely()` 返回受 256 MiB 限制的完整 `ByteArray`；Adapter probe/decode/encode 与 `BackupArchiveServiceImpl` 继续持有完整 payload。Passly JSON 构造完整文本和 Base64 字符串，Bitwarden 先建完整 `JsonElement` 并重序列化，加密导入会同时持有密文、AES-GCM 解密后的 ZIP 与解压资源。现有单资源 16 MiB、资源累计 128 MiB、文档 16 MiB、ZIP 解压累计 128 MiB、100,000 条记录限制需要移到分块读取/追加前检查。
