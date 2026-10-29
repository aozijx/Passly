# Passly 待办实施与验收

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成本地 AGENTS.md 的五项待办，按可独立审查的批次交付代码、测试和正式文档。

**Architecture:** 继续使用 Android/Kotlin、Compose Material 3、Room/SQLCipher 与 Proto DataStore。业务规则与端口留在 Domain，持久化原子性由 Data 负责；页面只持有展示与交互状态。

**Spec:** 本会话中用户要求完成的 AGENTS.md 待办；实施以当前生产代码为依据，本文记录验收，不替代正式功能文档。

## 全局约束

- Room、内部协议与持久化格式版本保持 1，APK 版本不变；不迁移旧开发数据。
- 保留已有改动，AGENTS.md 不提交，不推送。
- 每批修复先建立故障证据，再实现、清理旧逻辑、同步正式文档、验证和精确提交。
- Gradle 串行运行；相关模块单测、verifyArchitecture、App 编译/单测/lint/两个 APK 构建及 diff 检查为基础门禁。有设备才报告仪器测试运行结果。

## 重点验收

- 轮换已提交但其他写入失败时，页面策略不能与真实绑定分叉。
- 取消、锁定、销毁后的候选密钥、敏感缓冲与异步结果必须清理或拒绝发布。
- 无内容变化、访问活动与恢复版本不得混淆，活动不得记录字段值。
- 历史列表和比较页在长短内容、全屏与系统栏下均应有正确滚动和底部布局。
- 应用自身认证、备份和编辑输入不得启动不必要的填充或保存会话。

## 批次

实施顺序调整：先完成独立的 Topbar 与应用输入边界清理，再集中处理历史的数据、隐私与布局问题；范围与验收项不变。

### 1. 生物识别策略与密钥生命周期

- [x] 完成 `BiometricConfigurationSource` 与 Data 的 bootstrap 非敏感配置投影；删除 security preferences 重复字段和二次写入。
- [x] 更新 `SecurityAuthenticationSettingsInteractor`、`SecuritySettingsViewModel` 及回归测试；确认相同认证方式集合下策略变化仍刷新。
- [x] 检查 `RotateBiometricKeyUseCase`、`BiometricRotationReconciler`、`BiometricCryptoFactory` 的提交、取消、失败与录入变更路径，补足有缺口的清理测试。
- [x] 更新 settings-storage 与 authentication 文档，运行 Domain/Data/Security/App 对应门禁并提交。

策略真相源子批验证：`build-biometric-policy-final.log` 全部门禁通过；Domain 165、Data 70、App 864 项单测，零失败。
密钥清理子批验证：`build-biometric-cleanup-final.log` 全部门禁通过；Security 50、App 864 项单测，API 34 模拟器 48 项仪器测试，零失败。
修复取消/异常漏清理，并以持久化 binding 判断 candidate 是否已提交；核对 Keystore 录入变更失效标志和 Reconciler 的重试保护。
实体设备的真实录入变更尚未验收；此处完成的是代码路径核查、清理修复及上述自动验证。
仪器测试的既有选择器问题单独提交：`7045351`。

### 2. 版本历史、活动日志与历史界面

比较脱敏、活动语义与 Sheet 布局子批均已完成；后续验收记录见本计划的“当前交付与剩余验证”。比较脱敏子批按字段输出类型化差异，Wi-Fi 密码、安全答案及隐藏自定义字段仅标记变化，并移除整对象字符串化。该子批的 `build-revision-disclosure-final.log` 门禁通过，Domain 172、Data 70、App 862 项单测及 API 34 的 52 项仪器测试零失败。

- [x] 追踪 `EntryRevisionWriter`、各 Command Executor、`EntryActivityWriter` 与活动记录仓库，区分内容快照和访问事件；复现重复或含糊记录。
- [x] 以真实内容修改/恢复为版本依据，完善不含值的活动语义及持久化，移除旧补丁或重复记录路径。
- [x] 检查 `RevisionHistorySheet`、`RevisionHistorySection`、`ActivityTimelineSection` 的状态、比较/恢复操作与滚动布局，修复全屏底部空白。
- [x] 完成事务、DAO、模型及 UI 的 API 34 验证，更新 entry-revision-history 文档，并以 `1ddf8d7b` 提交。
- [ ] 完成下方列出的 API 31 复验与未覆盖的设备检查。

### 3. Topbar

- [x] 检查 `PasslyTopBar`、Vault/Detail/Settings 顶栏消费者，移除透明色与补偿背景，统一使用 M3 主题容器色。
- [x] 删除不再需要的函数、插值或桥接；验证主题、滚动与导航顶栏并提交。

验证：`build-topbar-cleanup-final.log` 全部门禁通过；删除旧插值的两项测试后，App 862 项单测与 API 34 的 48 项仪器测试均零失败。旧函数引用已清零。

### 4. 应用内自动填充边界

- [x] 检查 MainActivity、认证/填充 Activity、公共密码输入、备份密码输入与编辑保存退出路径。
- [x] 在合适的 Android 宿主/输入边界统一阻止自身非凭据用途的填充和保存会话，避免按单个页面叠补丁。
- [x] 保留对外 Autofill、Inline、BottomSheet 与无障碍回退能力；补充边界测试、Manifest/API 检查及正式文档后提交。

实现：同一 `excludeFromAutofill()` 策略覆盖 27 个实际 Compose 输入节点与 5 个 Activity；Compose 明确使用 `ContentDataType.None`，宿主使用 `NO_EXCLUDE_DESCENDANTS` 并取消自己的旧会话。无障碍原有自身包名排除继续保留。
验证：`build-internal-autofill-final.log` 全部门禁通过；Core 74、Security 50、App 862 项单测及 API 34 的 52 项仪器测试零失败。新增测试先观察到三类输入为 Text，修改后验证为 None，且密码显隐与 IME 焦点行为保持可用。

### 5. 职责清理与收尾

- [x] 盘点 ViewModel、Route、Mapper、Coordinator 的职责和依赖，重点复核本轮涉及的安全设置、历史与编辑流程。
- [x] 将业务编排归入现有应用服务/use case，页面状态留在 presentation；删除纯转发映射和无用文件，保留有实际转换语义的 mapper。
- [x] 运行最终相关门禁，检查文档链接和遗留符号，逐项记录代码与验证证据及未验证边界；待办只在本计划维护，不回填本地 AGENTS.md。

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

- [x] DAO/事务仪器测试使用与生产一致的 SQLCipher 驱动，保留已有外键和附件约束 callback；核对 JSON 查询能力（API 36：30 项全过）。
- [x] 分页测试明确报告 LoadResult.Error 的真实异常，避免强转掩盖 SQL 根因。
- [x] 使用 API 31 真机重新执行 Data 与 App 仪器测试（Data 33 项、App 55 项均通过）。
- [x] 补齐并检查空历史页有效截图。真实生物录入变更后的硬件 Keystore 行为仍未实测，不宣称通过。

最终独立只读复核未提出新的代码问题；API 31 Data/App 仪器测试已在真机复验通过，硬件 Keystore 的真实生物识别录入变更仍未实测，因此本计划尚未整体关闭。
收尾：`verifyArchitecture` 通过；文档相对链接检查和移除候选符号检索通过，`git diff --check` 通过。工作区本地 `AGENTS.md` 与 `.agents/skills/` 保持未跟踪，不暂存、不提交。
