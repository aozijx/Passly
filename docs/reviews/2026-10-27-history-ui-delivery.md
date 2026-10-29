# Passly History, UI, and Input Boundary Delivery Review

**Period:** 2026-10-27 to 2026-10-29
**Scope:** Biometric policy and key lifecycle, entry revision history, top bars, and in-app Autofill boundaries.

This review preserves delivery evidence for completed work. Current implementation contracts live in the linked feature and architecture documents. API 31 Data and App instrumentation passed; only physical biometric enrollment verification remains open in the [hardware follow-up plan](../superpowers/plans/2026-10-29-hardware-keystore-verification.md).

## Current contracts

- Biometric policy and authentication: [settings storage](../data/settings-storage.md), [authentication](../security/authentication.md).
- Entry revision behavior: [revision history](../features/vault/entry-revision-history.md).
- Top bar and navigation: [UI shell](../architecture/ui-shell-and-naming.md).
- In-app Autofill boundaries: [shared Autofill flow](../features/autofill/fill-flow.md).

## Delivered work

### 1. 生物识别策略与密钥生命周期

策略真相源子批验证：`build-biometric-policy-final.log` 全部门禁通过；Domain 165、Data 70、App 864 项单测，零失败。
密钥清理子批验证：`build-biometric-cleanup-final.log` 全部门禁通过；Security 50、App 864 项单测，API 34 模拟器 48 项仪器测试，零失败。
修复取消/异常漏清理，并以持久化 binding 判断 candidate 是否已提交；核对 Keystore 录入变更失效标志和 Reconciler 的重试保护。
实体设备的真实录入变更尚未验收；此处完成的是代码路径核查、清理修复及上述自动验证。
仪器测试的既有选择器问题单独提交：`7045351`。

### 2. 版本历史、活动日志与历史界面

比较脱敏、活动语义与 Sheet 布局子批均已完成；后续验收记录见本审查的“当前交付与剩余验证”。比较脱敏子批按字段输出类型化差异，Wi-Fi 密码、安全答案及隐藏自定义字段仅标记变化，并移除整对象字符串化。该子批的 `build-revision-disclosure-final.log` 门禁通过，Domain 172、Data 70、App 862 项单测及 API 34 的 52 项仪器测试零失败。

### 3. Topbar

验证：`build-topbar-cleanup-final.log` 全部门禁通过；删除旧插值的两项测试后，App 862 项单测与 API 34 的 48 项仪器测试均零失败。旧函数引用已清零。

### 4. 应用内自动填充边界

实现：同一 `excludeFromAutofill()` 策略覆盖 27 个实际 Compose 输入节点与 5 个 Activity；Compose 明确使用 `ContentDataType.None`，宿主使用 `NO_EXCLUDE_DESCENDANTS` 并取消自己的旧会话。无障碍原有自身包名排除继续保留。
验证：`build-internal-autofill-final.log` 全部门禁通过；Core 74、Security 50、App 862 项单测及 API 34 的 52 项仪器测试零失败。新增测试先观察到三类输入为 Text，修改后验证为 None，且密码显隐与 IME 焦点行为保持可用。

### 当前交付与剩余验证（2026-10-29）

实现已包含内容重写事务、无效清空/同值保存去重、活动关联实际修订、历史恢复独立事件、关系唯一键严格失败、
自然高度 Sheet 与固定恢复操作区，以及恢复确认的同步占用和授权取消后重试。删除了空转发历史用例及重复活动 UI 枚举。

本批实现的 2026-10-27 验证记录：`build-history-final.log` 全部门禁成功，Domain 175、Core 74、Security 50、
Data 73、App 863 项单测零失败；API 34 模拟器 Data 29、App 57 项仪器测试零失败。Room schema 重新导出仍为 1。
恢复状态新增测试先红后绿；长比较截图显示恢复按钮位于滚动区域之外。空历史截图为黑图，未作为视觉验收证据，
其自然高度与可见性由仪器测试验证。

API 31 真机的 `build-history-api31.log` 未通过：Data 29 项中 3 项失败，Autofill 查询报 `json_each` 缺失，
分页用例另有 LoadResult.Error 被强转为 Page；生产 `DatabaseProvider` 使用 SQLCipher，测试使用系统默认 SQLite，
后续先统一测试驱动再验证，不据此改写生产查询或跳过用例。App 仅完成 12 项并报告 1 项失败，
`PermissionRequestHostTest.changingOwnerRegistersIndependentLauncherAndRejectsOldResult` 的 XML 没有异常内容，不能视为全套已执行。

用户允许使用现有 AVD 继续验证。Data 所有 Room 仪器测试现统一通过 `TestDatabaseFactory` 使用生产 SQLCipher 驱动，并保留 `AttachmentReferenceConstraintCallback`；显式 JSON1 查询及 Autofill/Paging JSON 查询在 API 36 模拟器验证成功。分页测试遇到 `LoadResult.Error` 时现在直接抛出其原始异常。
API 36 Data 仪器测试：30 项通过、0 失败。API 36 App 全量仪器测试最终 57 项通过、0 失败，其中 `PermissionRequestHostTest` 2 项通过。首次空历史截图为黑图；改用实屏截图后又观察到白色空窗口，旧像素断言会误判。截图测试现等待画面中出现 Sheet 与遮罩的明暗差异，完整复跑通过；最终图片经人工检查，显示 Version history Sheet 和“No versions available”。

API 31 真机复验（OXF-AN00）：Data 33 项、App 55 项仪器测试均通过，零失败。Data 结果包含 HOTP 事务成功递增持久化、失败不递增、避免解密无关敏感字段的 3 项回归测试。硬件 Keystore 的真实生物识别录入变更仍未实测。

最终独立只读复核未提出新的代码问题。API 31 Data/App 仪器测试已在真机复验通过；仅硬件 Keystore 的真实生物识别录入变更仍未实测。
收尾：`verifyArchitecture` 通过；文档相对链接检查和移除候选符号检索通过，`git diff --check` 通过。工作区本地 `AGENTS.md` 与 `.agents/skills/` 保持未跟踪，不暂存、不提交。
