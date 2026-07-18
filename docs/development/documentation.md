# 文档维护约定

## 分类与命名

- 目录使用稳定主题：`architecture`、`data`、`security`、`features`、`development`、`decisions`、`reviews`。
- 文件名使用小写 kebab-case；目录内 `README.md` 只作为索引。
- 一个主题只有一份“当前实现”文档，避免 Design、Implementation、outline 并存。
- 类与包名以仓库当前代码为准；版本号、字段表等易漂移信息尽量链接源码而不是整段复制。

## 内容结构

主题文档优先采用“职责、边界、流程、约束、相关实现”。复杂流程使用 Mermaid，代码只保留能解释契约的关键片段。

ADR 统一包含：状态、日期、背景、决策、后果、备选方案和关联资料。已发布 ADR 不删除；决策变化时新增 ADR，并用
`Superseded by` 建立关系。

## 校验

修改文档后至少检查：

- 相对链接目标存在；
- 类名、包名和 Gradle 任务仍存在；
- Mermaid 节点文本不依赖大段实现代码；
- “已实现”和“计划中”有清晰区分；
- 没有密钥、签名凭据或本地路径进入文档。
