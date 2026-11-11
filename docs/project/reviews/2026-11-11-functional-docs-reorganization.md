# Passly 文档分类与路径重组验收

**范围：** 将产品功能、平台合同、工程规范和项目记录归入统一目录；更新唯一索引、代码路径引用与错误码生成路径。当前分类政策见[文档维护约定](../../engineering/development/documentation.md)。

| 验收项 | 结果 |
| --- | --- |
| 旧文档迁移 | 重组前 `HEAD` 有 116 篇 Markdown 文档；112 篇保留文档均有新路径目标。其余 4 篇是已完成的 Settings 资源计划/设计、Vault App Picker 计划和 Scanner Framing 设计；细节分别迁入 Settings/Vault/Scanner 正式文档及审查记录后删除。无其他未映射或意外缺失的目标。 |
| 导航链接 | 当前 `docs/` 有 134 篇主题 Markdown（不含索引 `docs/README.md`）；本地相对 Markdown 目标失效 0 项；`docs/README.md` 有 134 个互不重复的文档目标，全部存在且覆盖全部 134 篇主题页。Vault、Autofill、Settings、Backup、Scanner、Diagnostics Overview 与平台架构、Data、安全 Overview 均由唯一索引导航。 |
| 旧路径引用 | 仓库源码、README、AGENTS 和文档中的当前路径引用已更新；设计稿中仅有的旧路径映射示例在本次归档时删除。 |
| 错误码文档 | 本轮独立运行 `:buildSrc:test --no-parallel --max-workers=2` 成功（7 actionable tasks 均 `UP-TO-DATE`）；`verifyArchitecture --no-parallel --max-workers=2` 成功，执行了 `verifyErrorCodes` 与 `verifyModuleBoundaries`。 |
| Markdown 清单与有意删除 | 变更前 HEAD 跟踪 138 篇 Markdown；本次有意删除已合并到主维护计划的 ViewModel 专项 TODO 1 篇，工作区剩余 137 篇跟踪 Markdown。未跟踪的本地 AGENTS/skill 文件与 Scanner 临时 TODO 保留在索引范围外；Scanner TODO 已改指 Task 84。 |
| 总索引与相对链接 | 当前 `docs/` 有 135 篇 Markdown（含 `docs/README.md`）；扫描全部相对目标后失效 0 项；`docs/README.md` 的失效、重复和未索引文档目标均为 0 项。 |
| 独立审阅 | 发现两份 2026-10 记录错列在 2026-11 分组；已在 `docs/README.md` 新增 2026-10 分组并移回对应链接。 |
| 文档与待办生命周期 | 目录规则、唯一权威页、Overview 组合职责和完成后删除计划的约定已写入正式维护文档；本次实施计划和设计稿完成后移除。 |

相对链接检查使用 Markdown 本地目标扫描，覆盖路径存在性，不验证页面中的锚点是否存在。构建任务验证生成文档内容与错误目录一致；本次没有修改生成内容或生产行为。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 配置错误码目录生成与校验的唯一 Markdown 路径 | `ModuleBoundariesPlugin.apply()` | [`ModuleBoundariesPlugin.kt`](../../../buildSrc/src/main/kotlin/passly/ModuleBoundariesPlugin.kt) |
| 生成错误码目录 Markdown | `GenerateErrorCatalogTask.generate()` | [`GenerateErrorCatalogTask.kt`](../../../buildSrc/src/main/kotlin/passly/GenerateErrorCatalogTask.kt) |
| 比较已生成目录并拒绝过期文件 | `VerifyErrorCatalogTask.verify()` | [`VerifyErrorCatalogTask.kt`](../../../buildSrc/src/main/kotlin/passly/VerifyErrorCatalogTask.kt) |
