# Vault 页面边界调整执行计划

> 本计划跟踪 `docs/superpowers/specs/2026-11-08-vault-list-boundaries-design.md`；只重构现有行为，暂不实现多选或整理产品功能。

## 约束

- 保留当前 `dev` 工作区及 index 的无关改动；每项完成并审查后，按计划任务范围暂存、独立提交，其余改动继续留在工作区。
- 按具体状态、任务、生命周期和资源 owner 决定是否拆分。新 owner 必须实际减少原 owner 的职责或依赖。
- 每次内部切换迁移所有当前调用方并删除已无消费者的旧路径；仅保留可指出具体合同/消费者的兼容行为。
- 协程取消、fatal `Error`、认证/page/entry owner 失效和敏感资源清理合同不变。
- Gradle 串行执行并使用 `--no-parallel --max-workers=2`。

## 任务

### 1. 完成当前 picker 与列表状态切换

- [x] 为共享 app picker sealed state 增加纯映射/搜索测试，迁移 Detail 与 Password 调用方，删除旧布尔状态及 Password list getter。
- [x] 将列表分类搜索 query 收入 `VaultUiState` 并补 reducer/mapper 行为覆盖。
- [x] 运行 picker、Detail、Password、list reducer/mapper 相关套件，并核对 AndroidTest 编译。
- [x] 完成旧符号 caller 闭包与 diff whitespace 检查。

### 2. 断开 Trash contract 与组件模型

- [x] 将 Domain 列表项到 `TrashEntryUiModel` 的转换移入 `TrashUiMapper`；模型移出 `trash.ui`，`TrashContract` 不再依赖 UI 组件包。
- [x] 迁移 Route/BottomSheet/Card 消费方并扩充 mapper 测试入口。
- [x] 单独运行 `TrashUiMapperTest`。
- [x] 运行 Trash Reducer/ViewModel 行为套件并复核 sheet session、查询重试与取消的 owner 合同。

### 3. 审查 Detail 工作流

- [x] 在保留当前关联密码、favicon 和 presentation 修改的基础上，核对 DetailViewModel 的主要状态/任务/依赖 owner。
- [x] 对当前独立 session/workflow 作 retain 决策：favicon session 持有 preparation/save 与文件释放；QR session 持有授权任务、generation 与 payload 擦除；密码绑定 workflow 持有查询/绑定任务和 entry/auth token 检查；DetailWorkSession 持有访问页 token、写入与敏感 reveal 发布。
- [x] 将 `RelatedPasswordBindingUiState`、候选项与纯搜索结果从 `detail.ui.model` 移到 Detail presentation 合同包，切断 `DetailUiState`/Reducer/Workflow 对 UI 子包的反向依赖，并迁移组件和测试引用。
- [x] 审查 revision、reveal、加载和 QR 行为覆盖；修订历史继续留在 Detail 会话，因为恢复成功还需协调 Reducer、OTP runtime 与根快照刷新。初始化后订阅 metadata 的顺序已迁入 `ObserveEntryRevisionMetadataUseCase`，并删除 ViewModel 对 `EntryRevisionRepository` 的直接依赖。
- [x] 为新增用例和 owner 行为运行测试；切换状态合同后旧 package 引用已无消费者，并更新历史功能文档。

### 4. 审查四种编辑器

- [x] 核对 Password、OTP、Bank Card、Wi-Fi 的表单状态、seed/entry owner、write session 和 Route 生命周期。
- [x] 四种类型共用相同 `EditorLoadPhase` 投影；`EditorWriteSession` 继续只持有认证归属、保存单飞和 save effect 交付，类型专属表单与 seed 规则留在各 ViewModel/Route。
- [x] 验证取消、普通失败、fatal `Error`、迟到结果及 durable save 成功路径；`EditorWriteSession` 清理回调区分写入成功/撤权/owner 关闭，避免成功擦除表单时伪装成加载失败。编辑 seed 加载失败提示统一走 STARTED 生命周期 owner，STOPPED 期间暂停，未完成提示在恢复后继续，已完成提示不重复弹出。

### 5. 审查 Vault List 与显示/操作边界

- [x] 核对查询/Paging、显示设置、OTP 可见订阅、复制/删除、swipe action、页面 effect owner 和 Route 映射。
- [x] 提取 `VaultEntryCommandWorkflow`，由其持有复制/删除的 access gate、entry/page owner 检查、单飞集合、失败反馈与删除后的 OTP 清理；ViewModel 不再直接依赖四个命令能力。
- [x] 检查剩余 owner 与 state/action/UI model 单向依赖：ViewModel 保留设置可恢复读取、唯一 Paging generation、分类查询与 OTP runtime；Route 保留页面 owner、生命周期刷新、平台消息/导航/扫码 effect；Screen 接收映射状态与事件。没有找到应增加的转发 workflow。
- [x] 验证 List Query、command owner、Reducer/mapper 与菜单行为套件，并完成残余旧符号扫描。
- [x] 扫描已迁移实现后不再需要的 list state、helper、映射、测试、资源和文档；保留的单一 query generation、映射和 settings 投影均有当前消费者。

### 6. 审查导航与页面交接

- [x] 让导航请求与 editor destination registry 携带 Vault feature `AddType`，在 List UI 映射边界处转换显示 enum；移除 navigation 到 list UI model 的反向依赖。
- [x] 跑导航 registry、navigation caller 和 list menu 映射测试，确认 creation menu 顺序与 unsupported type 行为不变。
- [x] 追踪 Detail/Editor destination、一次性 seed、扫码 OTP 导入、Trash sheet 与页面级生命周期/状态 owner。
- [x] 保持 Route 的导航、宿主生命周期和平台 effect 职责，确保过期/锁定后的结果不能作用于新页面或认证会话。
- [x] 迁移所有生产与测试 caller 并移除确认无合同的旧交接接口。

### 7. 全范围验收

- [x] 更新实际变化涉及的正式 Vault 文档与维护 TODO，记录有证据保留的旧/特殊合同。
- [x] 扫描生产、测试、DI、导航、资源、文档对旧符号的引用；检查无双轨内部路径。
- [x] 运行 Vault ViewModel/Reducer/Session/Route 测试、`verifyArchitecture`、AndroidTest 编译及 `git diff --check`。
- [x] 明确记录工作区剩余的非 Vault 变更及本轮未修改边界；本计划范围内没有未运行的必需验证。

## 当前结果

- `:app:testDebugUnitTest --tests "com.aozijx.passly.feature.vault.*"` 通过；XML 报告确认 71 个类、472 个用例，无失败。
- `:app:compileDebugAndroidTestKotlin`、`verifyArchitecture`、`:app:compileDebugKotlin` 与 `git diff --check` 通过；diff check 仅有仓库 LF/CRLF 转换提示。
- Detail 修订 metadata 初始化/订阅顺序已迁入 `ObserveEntryRevisionMetadataUseCase`；Detail picker state model 已移出 component `ui.model` 依赖。
- 编辑器加载失败 snackbar 和保存/撤权后的清理结果现有明确生命周期与语义 owner，并以 STOPPED/恢复和完整编辑用例覆盖。
- Trash presentation mapper 已作为计划 Task 2 独立提交：`bd08fb6dd refactor(vault): isolate trash presentation mapping`。其余 Vault 与非 Vault 改动仍留在工作区，尚未按任务逐项审查和提交。
