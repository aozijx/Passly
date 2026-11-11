# ViewModel 状态与 owner 审查 TODO

**盘点日期：2026-11-10。** 本轮检索 `app`、`feature/autofill`、`security` 的 34 个 `*ViewModel.kt`，并抽查状态、Reducer、长 Flow、异步任务及现有专题计划。此文件是后续逐项验证入口；源码线索不是已复现缺陷，也不按文件行数决定拆分。生产代码本轮不改。

## 判断准则

- 先确定 Repository/Domain、页面草稿和展示投影各自的真相源，再检查是否有非法组合。简单字段更新留在 ViewModel；多个纯转换汇合且需要原子、穷尽验证时用 Reducer；状态独有的纯计算可以贴近状态，但 I/O、授权、取消、清理和系统 effect 仍归实际 owner。
- 每项先追调用方、Route/Activity 生命周期和现有测试，再决定保留或修改。若提取工作流，原 ViewModel 必须实际减少状态、任务和依赖；不得增加一一转发层。
- 异步 owner 变更须在 ViewModel/宿主级覆盖替换、锁定/关闭、失败重试、取消和迟到结果。纯 Reducer 测试只能证明纯转换。

## 后续批次

### 1. 当前未提交流程的 owner 验收（优先）

- [ ] **Scanner**：基于[输入与生命周期合同](../../../product/scanner/input-lifecycle.md)，审查 `ScannerViewModel` 的 `Work.Active` owner 与 reducer 展示状态之间是否有重复职责或无效状态组合。Route/Activity Result 关闭交错、decoder client 与候选几何验证分别由 Scanner feature 文档记录；Scanner 反馈现改为 UI state 中的单个待消费值和显式消费确认，需在定向验证中覆盖宿主暂停/恢复、owner 替换及停止时清理。只追 ViewModel owner、取消和反馈交付合同，不复制其他验收项。
- [ ] **Detail**：从 `DetailViewModel` 的加载、历史、标签、favicon、二维码、修订及关联密码任务绘制状态/任务/依赖表，检查各工作流关闭、撤权、迟到结果与重试。`DetailReducer`、`DetailWorkSession` 和已有工作流已承担部分职责；只对仍由 ViewModel 混合持有且能独立拥有生命周期的流程提出提取。对照 [主维护待办](2026-10-30-passly-maintainability-todo.md) 的 Detail 验收，避免重复列缺陷。
- [ ] **Autofill 设置**：验证 `AutofillSettingsViewModel` 的偏好读取失败与系统服务观察失败各有独立重订阅入口；核对页面退出后 `PageEffects` 丢弃旧 effect，及设置写入期间状态变更的 owner。当前两种读取状态已经分开，不因事件数增加而增设 Reducer。对照 [Autofill 专题](2026-11-07-passly-autofill-followups.md)。

### 2. 其他复合流程

- [ ] **Backup**：沿 `BackupViewModel`、`BackupWorkflow`、`BackupReducer`、文件选择器 Route 核对 `pendingImportDocument`/`pendingExportTarget` 与页面 owner、request ID 的一致性，尤其是退出、重入、回调迟到和执行中关闭；检查凭据擦除与提交后结果。现有 sealed workflow 是合理基础，先做宿主行为测试，不按文件长度拆分。
- [ ] **Unlock 与 Recovery Code**：对 `UnlockViewModel` 的认证、尝试次数刷新和首次密码设置，以及 `RecoveryCodeSettingsViewModel` 的生成、验证、提交与 disclosure 清理，分别列出唯一状态 owner、durable commit point、取消与 fatal 清理路径。安全流程保留显式 `when` 和提交顺序，不能改成通用事件 handler 或持依赖的状态对象；优先复用现有安全/维护待办的验收项。
- [ ] **Autofill 宿主流程**：核对 `:feature:autofill` 的 Legacy Fill、Credential Response、Accessibility 三个 ViewModel 与对应 Activity/Service 的交付 owner、callback 迟到、撤权和完成标记。此项是宿主行为验收，不以统一三类 ViewModel 的状态形状为目标；与 [Autofill 专题](2026-11-07-passly-autofill-followups.md) 去重。

### 3. 其余页面的轻量核对

- [ ] 对 Vault 列表/编辑器/回收站、设置、Shell、诊断、数据库重置及 Recovery Mode 的其余 ViewModel 按“真相源、合法状态、长 Flow 失败恢复、任务取消、敏感清理、Route effect”逐项核对。Vault 列表已将界面、库和交互设置读取并入一个 `VaultViewModel` owner，删除原 `VaultDisplayViewModel` 和重复失败/重试门控；定向列表单测、AndroidTest 编译与 `verifyArchitecture` 通过。继续检查 `TrashViewModel` 的冷 Flow 失败重订阅、`StorageSettingsViewModel` 的多个刷新任务，以及四类编辑器共用的 `EditorWriteSession`；已有明确 owner 且无行为缺口者记录保留结论，不强加 Reducer。
- [ ] 每一项输出具体调用链、反例测试和修改前后 owner 差异；确认无实际问题时关闭对应待办。执行受影响单测、`verifyArchitecture`、`git diff --check`；涉及 Android 宿主则补 AndroidTest 编译和设备行为验收。

## 当前判断与限制

34 个文件的结构盘点用于确定审查顺序，不是 34 个文件均有问题的结论。本轮抽查显示 Scanner、Vault、Unlock、Backup 已有纯 Reducer，简单设置页直接投影 Repository Flow；两类实现都符合单向数据流。未运行 Gradle 或设备测试，待办中的交错风险尚未复现。
