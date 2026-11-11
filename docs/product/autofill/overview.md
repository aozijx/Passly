# Autofill 功能总览

状态：当前实现。

Autofill 将系统提供的页面/字段结构转换为候选查询，经用户认证和授权后读取所选凭据，再按调用方的 Android 协议交付。Legacy Autofill、Credential Manager 与 Accessibility 是不同平台入口；它们共享必要的候选与安全能力，但保留各自的请求、结果和生命周期合同。

## 功能路径

| 工作流 | 权威专题 |
| --- | --- |
| 通用候选发现、结构解析、授权、秘密读取与结果交付 | [共享填充流程](fill-flow.md) |
| Android Legacy Autofill Service | [Legacy Service](legacy-service.md) |
| Credential Manager Provider | [Credential Manager](credential-manager.md) |
| 保存请求与凭据写入 | [保存语义](save-flow.md) |
| 锁定期间的加密保存 Inbox 与解锁后导入 | [Sealed Inbox](sealed-inbox.md) |
| 平台流程验证、清理和验收范围 | [验证矩阵](verification.md) |
| 对外 Autofill 安全合同 | [Autofill 安全](../../platform/security/autofill.md) |

## 跨层协作

- Android Service/Provider、Accessibility service 和 Activity 属于宿主/平台边界。平台回调被解析为 Feature 内部类型化交互；Android response 不跨入口互相转换。
- `:feature:autofill` 通过 Domain/Core/Runtime 合同执行候选查询、设置读取、授权交互、凭据保存和结果交付；它不依赖 App、Data 或 Security 的实现模块。
- App 持有最终 Manifest、组合根与认证 Activity 宿主；Security 持有认证/授权/会话编排；Data 持有持久化实现。Hilt 绑定不会改变 Gradle 源码依赖边界。
- 候选发现、认证、读取、用户取消和平台完成绑定同一交互/session owner。Activity 停止、取消或请求替换时，迟到结果不得向 Android callback、页面状态或旧会话回写。
- 敏感值只在获得授权的读取和结果构造窗口内传递；日志、普通 UI state、导航参数和持久设置不保存凭据明文。

通用依赖、状态与错误合同见[层间契约与通信](../../platform/architecture/layer-interactions.md)、[Autofill 安全](../../platform/security/autofill.md)和[认证与恢复码](../../platform/security/authentication.md)。

## 代码位置

| 职责 | 代表类型 | 源码 |
| --- | --- | --- |
| 统一 Feature 入口与 launch 解析 | `AutofillFeatureEntry` | [`AutofillFeatureEntry.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/AutofillFeatureEntry.kt) |
| App 认证 Activity 宿主 | `AutofillAuthActivity` | [`AutofillAuthActivity.kt`](../../../app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt) |
| Domain 创建能力合同 | `AutofillEntryCreation` | [`AutofillEntryCreation.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillEntryCreation.kt) |
