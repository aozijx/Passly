# Kotlin 编码规范

状态：当前开发规范。

## 基础格式

- 遵循 Kotlin 与 Android Kotlin 官方风格，4 空格缩进。
- 文件只容纳高度相关类型，文件名与主要类型一致，包名与目录一致。
- 多行参数和集合使用尾逗号；按语义换行，不压缩命名。
- import 由 IDE/格式化工具排序。格式化改动与功能改动分开提交。

## 命名

- 类型使用 PascalCase；函数和属性使用 camelCase；真正编译期常量使用 UPPER_SNAKE_CASE。
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
