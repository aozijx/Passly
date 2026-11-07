# Autofill BottomSheet 认证合同实施记录

**状态：协议切换已实现；不是待执行计划。** 真实字段填值及宿主行为统一由[Autofill 后续 Task 21](2026-11-07-passly-autofill-followups.md#task-21)验收，不在本页复制任务。

## 当前结果合同

- 锁定 BottomSheet 使用 dataset authentication；认证并选择条目后直接返回 Dataset。
- 锁定系统/Inline 使用 response authentication；认证后交还 FillResponse，保留平台候选选择语义。
- Legacy 结果目标使用显式稳定键；缺失/未知目标拒绝，不读取旧布尔标志作 fallback。
- 候选 lazy-list key 是稳定 String，点击回调仍保留 EntryId。
- framework 结果由 Feature entry 交付，App Activity 执行 setResult/finish；result.delivered 只表示平台回调已返回，不证明目标控件填值。

响应工厂和结果 claim/lease 测试各有自己的覆盖范围；历史占位 Dataset 或设备 locked 提示不算最终填值验收。本次不运行设备测试，不再沿用固定 AVD、示例条目或重新初始化 Vault 的操作步骤。

当前协议与验证范围见[Legacy 流程](../../features/autofill/legacy-service.md)和[验证矩阵](../../features/autofill/verification.md)。
