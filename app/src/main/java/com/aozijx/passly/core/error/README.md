# Passly 错误处理模块 (Error Module)

## 1. 核心设计原则

本模块基于 Kotlin `sealed class` 构建，目的是利用编译器强制穷举（Exhaustive `when`）来保证所有错误类型在
UI 层都被正确处理。

## 2. ⚠️ 关键约束（必读）

`sealed class AppError` 的所有直接子类**必须在同一个 Gradle 包（Package）下**。

因此，虽然我们按业务领域（认证、数据库、加密等）在**物理磁盘**上创建了文件夹，但 **所有 `.kt`
文件的 `package` 声明都必须保持一致**：

```kotlin
package com.aozijx.passly.core.error   // 统一使用 error 包