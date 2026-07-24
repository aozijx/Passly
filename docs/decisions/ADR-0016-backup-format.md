# ADR-0016: 独立版本化的 Vault Backup 与可插拔格式

- 状态：Accepted
- 日期：未记录；2026-07-24 修订

## 背景

备份需要独立密码、格式识别、完整性校验以及 Entry 和附件的共同传输。数据库、
Domain 和 UI 会持续重构，备份 wire schema 不能随它们漂移；第三方密码管理器导入也
不能继续扩张主 Service。

## 决策

采用独立的 document v1 wire model 和 container v1。容器 magic 为 `PSLYBKP1`，
完整自描述头作为 AES-GCM AAD；备份密码经 Argon2id 派生 AES-256 key。附件总是进入
可恢复备份，图标可选，恢复码和 Vault DEK 不用于备份。

`VaultBackupService` 只提供通用 request API。格式通过 Export/Import Adapter 和
Registry 扩展，外部格式先映射到 canonical `BackupBundle`。

## 后果

能拒绝错误密码、篡改、截断、尾随字节、未知版本和危险 KDF 参数。数据库 DTO 和 UI
变化不再改变备份协议。新增 Bitwarden 等格式只增加 Adapter。

代价是维护独立 wire model、版本 Reader 和格式映射测试。Room 与文件系统之间无法
获得真正跨介质 ACID，因此恢复使用文件 Journal，并记录进程强杀窗口这一限制。

## 备选方案

未采用明文 ZIP、与 Vault DEK 绑定的备份、Room 数据库镜像、格式专用 Service 方法和
静默跳过未知外部字段。

## 关联

[备份格式](../data/backup-format.md)
