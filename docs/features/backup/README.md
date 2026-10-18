# Backup

状态：当前实现（Passly 文档格式 v1，代码核对：2026-10-18）。

Backup 是 Vault 业务数据归档，不是 Room 数据库镜像。UI 与流程位于 presentation/feature/backup，协议、
加密和文件 I/O 位于 feature/backup/internal，Room 快照适配位于 app/database/backup。

## 能力

| 格式 | 方向 | 资源 | 保密性 |
| --- | --- | --- | --- |
| `passly.encrypted` | 导入、导出 | 附件与可选图标 | Argon2id + AES-256-GCM |
| `passly.json` | 导入、导出 | Base64 内嵌 | 明文 |
| `passly.text` | 仅导出 | 不支持 | 明文、有损 |
| `bitwarden.json` | 仅导入 | 不支持附件 ZIP | 仅接收明文 JSON |

完整备份可包含所选条目及业务字段、关系、已提交附件和可选图标。Draft、活动记录、修订历史、可重建索引、
设置、会话、DEK、Bootstrap、认证材料、Room 密文结构、本机路径和未提交附件不进入备份。

## 主题文档

- [导入、导出与 SAF](import-export.md)
- [Passly Archive v1](archive-format.md)
- [验证要求](verification.md)

相关决策见 [ADR-0022](../../decisions/ADR-0022-keyed-backup-document.md)。文档 v1、加密容器 v1 与 Room
Schema 1 是彼此独立的版本；当前协议替换没有升级数据库，也没有兼容旧 `passly-vault`/`passly-archive` 文档。
