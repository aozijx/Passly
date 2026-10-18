# Autofill

状态：过渡中；填充和当前保存流程已实现，sealed Inbox 已批准但尚未实现。

Passly 在 Android 12+ 保留两套平台入口：

| 入口 | 系统范围 | 当前状态 |
| --- | --- | --- |
| Legacy `AutofillService` | API 31+ | 填充、SaveInfo、onSaveRequest 已实现 |
| Credential Manager Provider | API 34+ | Password get/create/clear-state 已实现 |

两套入口共享平台无关的候选发现、授权、单条秘密解析、草稿校验和 Entry 写入，但 Android 结果合同保持独立。
Legacy `Dataset/FillResponse/SaveCallback` 不转换为 Credential Manager response，反方向也一样。

## 主题文档

- [共享填充流程](fill-flow.md)
- [Legacy Autofill Service](legacy-service.md)
- [Credential Manager Provider](credential-manager.md)
- [保存语义](save-flow.md)
- [Sealed Save Inbox（已批准，尚未实现）](sealed-inbox.md)
- [验证矩阵](verification.md)
- [安全策略](../../security/autofill.md)

当前锁定状态保存仍会认证：Legacy 使用进程内一次性草稿和 `LegacySaveActivity`，Credential Manager create 使用
自己的交互。目标设计是在无读权限、无认证弹窗的情况下把凭据密封进 app-private Inbox，之后解锁导入。文档不得
把这一目标写成当前能力。

平台依据：[Autofill services](https://developer.android.com/identity/autofill/autofill-services)、
[Credential provider](https://developer.android.com/identity/sign-in/credential-provider)。
