# Passly Backup 后续待办

**2026-11-07 复核/排序：** 已完成的 Task 1/3/5 从本待办移除，其实现合同与验证记录分别留在[导入工作流](../../features/backup/import.md)、[备份验证](../../features/backup/verification.md)和[操作生命周期](../../features/backup/operation-lifecycle.md)。剩余事项按实现依赖排序：先完成端到端流式格式迁移，再评估 Compose 口令 String 的驻留边界。

**状态：** Task 2 设计草案待用户审核，之后拆分实现计划；Task 4 待评估。Task 2 已按用户决定采用分块 JCA AES-GCM 新容器，并移除旧 v1 读取路径，不保留兼容层。

## Task 2：将备份导入与导出迁移到端到端流式 I/O

- [ ] 审核 [`流式备份设计`](../specs/2026-11-07-backup-streaming-import-design.md)，明确 v2 帧协议、256 MiB 文件预算、KDF 参数边界、ZIP/JSON 资源限制和单遍格式识别。
- [ ] 在审核通过后编写实现计划；先迁移 FileStore、Service 和全部 Adapter 契约，再切换 JSON、Bitwarden、Text 与加密格式的读写实现，删除完整文件 ByteArray API 和旧 v1 容器路径，不增加 fallback/兼容转发。
- [ ] 流式解析时执行输入、记录数、单资源和累计资源限制；失败、取消、认证失败与部分恢复路径清理临时数组，且只有完整认证和 EOF 校验成功后才启动 restore。
- [ ] 更新正式格式/导入文档与验证矩阵；覆盖新容器认证、截断/重排/尾随数据、错误密码/KDF 参数、资源边界、取消和清理。旧 v1 文件在重构后不再可导入，此数据兼容变更依用户明确要求执行。

**当前证据：** `BackupFileStore.readBytesSafely()` 返回受 256 MiB 限制的完整 `ByteArray`；Adapter probe/decode/encode 与 `BackupArchiveServiceImpl` 继续持有完整 payload。Passly JSON 构造完整文本和 Base64 字符串，Bitwarden 先建完整 `JsonElement` 并重序列化，加密导入会同时持有密文、AES-GCM 解密后的 ZIP 与解压资源。现有单资源 16 MiB、资源累计 128 MiB、文档 16 MiB、ZIP 解压累计 128 MiB、100,000 条记录限制需要移到分块读取/追加前检查。

## Task 4：评估备份口令在 Compose String 投影中的驻留范围

- [ ] 评估 `OutlinedTextField` 与 `BackupRestoreSheetUiState.password: String` 的输入/回显接口能否在不破坏 Compose 编辑、选择和 IME 行为的前提下缩短口令 String 生命周期；沿 TextField → Action → `OwnedChars` → workflow → UI projection 全链路评估，不只替换 ViewModel 内一个类型或增加无法擦除 String 的薄包装。
- [ ] 保留并核对字符数组责任：`BackupViewModel.execute()` 擦除 workflow `SensitiveValue`；`DefaultBackupOperationGateway.performExport()` / `performImport()` 擦除传给 `BackupArchiveService` 的副本；替换/关闭 workflow 时旧的或被拒绝的 owned buffer 必须擦除。若调整，覆盖普通失败、取消、ViewModel 清理和输入重放后的数组生命周期。
- [ ] 核对 Compose 与 Navigation 未把口令写入 SavedState、持久化、日志、异常消息或外部 Intent；若 `String` 仍不可避免，在正式安全文档记录边界，不声称可擦除所有口令副本。无需引入平行表单状态模型。

**当前证据：** 两个 `OutlinedTextField` 都以 `state.password: String` 为 value，并将 `onValueChange(value)` 发为 `BackupUiAction.UpdatePassword(value)`。`BackupViewModel.onAction()` 调用 `OwnedChars.fromString(action.password)`；`BackupSheetUiMapper` 的私有 `SensitiveValue.copyToString()` 将临时 `CharArray` 转成新 String 并在 finally 擦除临时数组。workflow 与 Gateway 各自持有的 `SensitiveValue`/`CharArray` 有 finally 擦除；Compose projection 的 String 本身不能可靠覆写。源码路径暂未见 SavedState、Navigation、日志或持久化传递。这是内存驻留边界评估，不是已证实的外泄。
