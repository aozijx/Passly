# ADR-0022: 使用稳定键值字段的备份文档

- 状态：Accepted
- 日期：2026-10-05
- 替代：ADR-0016 中的 typed backup document；其加密容器决策继续有效

## 背景

ADR-0016 将备份协议与 Room 模型分离，但 document v1 仍按 Login、Card、Identity、OTP 等
Kotlin payload 分叉，并直接使用枚举名称。新增或重命名条目类型、字段、关系时，协议会跟随内部代码
漂移；删除产品类型也容易造成历史数据库内容被完整备份静默遗漏。

项目仍处于开发期，可以破坏旧备份文件，但不能升级或清空现有 Room 数据。新文档需要从现有数据库
重新导出，并允许未来版本在不降低文件完整性要求的前提下跳过单个未知条目或字段。

## 决策

原生 JSON 与加密备份统一使用 **passly-field-archive v1**。Entry 由稳定 type key、revision、时间戳、
keyed field 数组和 attachmentIds 表达；字段值使用 text、text_list、boolean、integer、long、
custom_fields 六种封闭形状。

协议键通过显式注册表映射到 Domain 类型。外部输入不得使用 Enum.valueOf、enumValueOf、enum.name 或
ordinal。archive registry 覆盖数据库仍可读取的全部 EntryType；完整导出默认使用该集合。

导入提供两种策略：

- COMPATIBLE 默认忽略未知字段、跳过未知或无效 Entry，并裁剪其 Link 与 Resource；
- STRICT 对同样的语义不兼容拒绝整个导入。

文档身份和版本、容器认证、KDF 上限、重复顶层 ID、资源清单、大小、SHA-256、ZIP 路径及输入大小在
两种策略下始终严格。兼容过滤后没有可恢复 Entry 时，OVERWRITE 在清库前失败。导入结果返回写入、
已有、跳过、忽略字段和裁剪依赖的计数。

旧 passly-vault、passly-archive 和 typed payload 不保留 Decoder。由于格式身份已经改变，新文档仍从
version 1 开始；Room Schema 保持 version 1。加密容器继续使用 ADR-0016 的 PSLYBKP1、Argon2id 和
AES-256-GCM 边界。

## 后果

新增或重命名 Kotlin 类型不再自动改变备份 wire schema；遗漏 archive key 会由完整性测试或导出失败
暴露。JSON、加密、TXT 和 Bitwarden Adapter 共用 canonical keyed records，旧的类型专用 wire payload
与 valueOf 解析路径被删除。

代价是必须长期维护注册表、值形状校验、兼容导入规划和 golden fixtures。兼容模式需要明确统计被跳过
或裁剪的内容，不能把损坏文件误当作向前兼容。

## 备选方案

- **继续扩展 typed payload**：新增类型会持续修改中心协议对象，未采用。
- **直接序列化 Domain 或 Room 模型**：使内部重构成为协议破坏，未采用。
- **使用枚举名称作为键**：重命名与大小写变化会破坏历史文件，未采用。
- **兼容模式放宽全部校验**：会掩盖篡改与资源损坏，未采用。
- **把新结构写成 document v2**：格式标识已变化，不需要共享旧版本序列，未采用。

## 关联

- [Backup 功能与 Passly 备份协议](../features/backup.md)
- [ADR-0016](ADR-0016-backup-format.md)
