# Kotlin 编码规范

状态：当前开发规范。

## 基础格式

- 遵循 Kotlin 与 Android Kotlin 官方风格，4 空格缩进。
- 文件只容纳高度相关类型，文件名与主要类型一致，包名与目录一致。
- 多行参数和集合使用尾逗号；按语义换行，不压缩命名。
- import 由 IDE/格式化工具排序。格式化改动与功能改动分开提交。

## 命名

- 类型使用 PascalCase；函数和属性使用 camelCase；真正编译期常量使用 UPPER_SNAKE_CASE。
- 固定常量放在 Kotlin 文件顶部的声明区，即 imports 之后、主要类型/函数之前；文件私有实现常量用 `private const val`，避免藏在文件尾部的 `companion object`、函数内部或使用点旁。
- 被多个源文件共享的常量集中到所属层/Feature 的专用配置或常量文件（如同一协议的 `XxxConstants.kt` / `XxxConfig.kt`），保持最小可见性；不创建跨功能、无明确 owner 的全局 `Constants.kt` 大杂烩。单文件常量保持在该文件，不为一处使用额外造转发对象。
- 算法、持久化 key、wire tag、格式版本和输入上限属于具体协议/组件 owner；可集中在该 owner 的文件头部或专用合同文件，但不可按相同数字或相同字面量跨不相关模块合并。面向用户的文本、Android 资源、构建值与运行期配置继续由资源、Gradle 或配置数据源拥有，不复制成 Kotlin 常量。
- 抽取或移动常量只改变其归属与位置，不隐式改变值、默认行为、持久化兼容性或外部协议。
- Boolean 表达明确判断：`isEnabled`、`hasFullAccess`、`canSubmit`。
- 安全和判断边界使用有语义的 `Policy`、`Gate`、`Guard`、`Provisioner`、`UseCase`。
- 不使用 `Manager` 聚合数据、认证、导航和 UI 等无关职责。
- 不用模糊 `getById()` 掩盖高敏明文读取，也不在 ViewModel 暴露 DAO/Room 实现概念。

## 结果与空值

- Domain 返回明确结果、failure 或 sealed state；UI 映射错误，不重新决定安全策略。
- nullable 只表达真实“没有值”，不用多个 Boolean/nullable 拼装互斥阶段。
- 不吞异常。可恢复异常转为明确 failure；`CancellationException` 必须继续传播。
- 资源和敏感缓冲在 `finally` 中释放或擦除。

## 分层

- Presentation 只依赖 use case、domain repository/port、policy 或应用服务，不直接访问 DAO、文件或密码学实现。
- DAO 负责访问；Repository 负责聚合、持久化模型转换和定义过的明文边界；UseCase 表达跨端口业务动作。
- Policy 表达可复用判断，但不为一次简单 repository 调用机械增加包装层。
- 外部 wire protocol 禁止依赖 `enum.name`、ordinal 或 `Enum.valueOf()`；使用显式稳定键和穷尽映射。

## 敏感值

- 密码、恢复码、token、OTP secret 和密钥优先使用可擦除容器；所有权交接后原拥有者不得继续读取。
- 不进入日志、异常 message、Toast、SavedState、Bundle、Intent 或普通长期 UI 字符串状态。
- 不在 `toString()`、data class 默认打印或调试辅助中泄露敏感字段。
- 异常日志使用结构化 Logger 和受控错误码；业务层不调用 `printStackTrace` 或 Android Log。

更完整的状态架构见[状态管理规范](architecture-and-state.md)，UI API 见[Compose 规范](compose-ui.md)，长期边界见
[维护边界](maintenance-boundaries.md)。

## 自动格式检查

仓库通过 KtLint Gradle 插件 `14.2.0` 固定调用 KtLint `1.8.0`。根目录 `.editorconfig` 统一规定四空格缩进、
尾逗号、import 排序、文件名与包名规则；`ktlintCheckAll` 覆盖各 Gradle 模块、Gradle Kotlin 脚本和 `buildSrc`。
`check` 只运行只读检查，格式化必须显式调用 `ktlintFormatAll`。格式修正独立于行为改动；例外仅在具体
Kotlin 声明上用窄范围 `@Suppress("ktlint:standard:<rule>")`，并在代码附近说明保留理由，不创建全仓 baseline。
通配 import 仍按字母顺序检查，但不强制展开；仓库 Kotlin 规范只要求排序，保持现有 import 使用方式可以避免
格式接入带来大量与规则目标无关的 import 改写。
