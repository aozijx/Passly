# 2026-11-06 Kotlin 格式门禁交付记录

## 结果

Kotlin 编码规范已接入可重复的只读格式检查。仓库固定使用 KtLint Gradle 插件 `14.2.0` 和引擎 `1.8.0`；根 `.editorconfig` 规定缩进、尾逗号和 import 排序等规则。`ktlintCheckAll` 覆盖 Gradle modules、Gradle Kotlin 脚本及 `buildSrc`，每个子项目的 `check` 依赖本模块只读 `ktlintCheck`。格式改写需显式调用 `ktlintFormatAll`，Android CI 与本地文档使用相同的全仓检查任务。

## 执行与参数流

- Gradle 插件在根构建中注册 `ktlintCheckAll` / `ktlintFormatAll`，聚合模块任务；模块 `check` 只接入自身 `ktlintCheck`，不会在每个模块重复触发全仓聚合器。
- KtLint 从各项目根目录读取共同的 `.editorconfig`，并覆盖 Kotlin 源码、Gradle Kotlin DSL 和 `buildSrc` Kotlin 源码；检查任务只报告违规，格式化任务才写回文件。
- CI 与开发文档调用 `ktlintCheckAll`，保持同一个验证入口。例外通过具体声明上的窄范围 `@Suppress("ktlint:standard:<rule>")` 表达，并在代码旁说明理由；没有全仓 baseline 或宽范围 suppress。
- 格式导入规则要求排序，但不强制展开通配 import；此设置避免引入与排序目标无关的大批改写。

## 清理规模与历史验证

首次格式差异清单覆盖 1,221 个 Kotlin 源文件，共记录 21,214 项；最高频规则包括 multiline-expression-wrapping 5,243 项、function-signature 4,092 项、blank-line-before-declaration 1,670 项、class-signature 1,297 项及 trailing-comma-on-call-site 1,143 项。另有 11 个文件命名不匹配，已逐个重命名；包名规则无违规。格式门禁接入前后没有行为改动；清理过程另外将可变状态字段改为语义化名称、常量改为大写，并为混合布尔条件补上明确括号。

实施时记录的验证：`ktlintFormatAll`、`ktlintCheckAll`、`verifyArchitecture`、`:app:testDebugUnitTest`、`:core:testDebugUnitTest`、`:core:common:test`、`:data:testDebugUnitTest`、`:domain:test`、`:runtime:session:test`、`:security:testDebugUnitTest`、`:app:compileDebugAndroidTestKotlin`、`:data:compileDebugAndroidTestKotlin` 和 `git diff --check` 通过；单测合计 1,550 项，无失败或错误。任务图检查记录 `ktlintCheckAll` 只执行 lint；`:app:check` dry-run 显示模块 `check` 接入 `ktlintCheck`，编译和测试仍属于既有 Android 生命周期。以上是实施时历史结果，本归档轮次没有重跑任务。

## 当前规范

当前可执行要求见 [Kotlin 规范](../development/kotlin-style.md) 和 [测试与质量门禁](../development/testing.md)。本文只保留门禁落地规模与历史验证，不作为当前规则来源。

## 代码位置

| 职责 | 配置/任务 | 文件 |
| --- | --- | --- |
| 固定插件版本并注册聚合检查/格式化入口 | `ktlintCheckAll`、`ktlintFormatAll` | [`build.gradle.kts`](../../build.gradle.kts)、[`gradle/libs.versions.toml`](../../gradle/libs.versions.toml) |
| 声明格式规则 | `.editorconfig` | [`.editorconfig`](../../.editorconfig) |
| 各模块接入只读 lint 检查 | `check` → `ktlintCheck` | [`build.gradle.kts`](../../build.gradle.kts) 与各模块构建脚本 |
| 开发与 CI 统一验证入口 | `ktlintCheckAll` | [`testing.md`](../development/testing.md) 与 Android CI workflow |
