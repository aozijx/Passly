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

### 1. 生物识别策略与密钥生命周期

- [x] 完成 `BiometricConfigurationSource` 与 Data 的 bootstrap 非敏感配置投影；删除 security preferences 重复字段和二次写入。
- [x] 更新 `SecurityAuthenticationSettingsInteractor`、`SecuritySettingsViewModel` 及回归测试；确认相同认证方式集合下策略变化仍刷新。
- [ ] 检查 `RotateBiometricKeyUseCase`、`BiometricRotationReconciler`、`BiometricCryptoFactory` 的提交、取消、失败与录入变更路径，补足有缺口的清理测试。
- [ ] 更新 settings-storage 与 authentication 文档，运行 Domain/Data/Security/App 对应门禁并提交。

策略真相源子批验证：`build-biometric-policy-final.log` 全部门禁通过；Domain 165、Data 70、App 864 项单测，零失败。
密钥生命周期检查尚未完成，不据此关闭整项待办。

### 2. 版本历史、活动日志与历史界面

- [ ] 追踪 `EntryRevisionWriter`、各 Command Executor、`EntryActivityWriter` 与活动记录仓库，区分内容快照和访问事件；复现重复或含糊记录。
- [ ] 以真实内容修改/恢复为版本依据，完善不含值的活动语义及持久化，移除旧补丁或重复记录路径。
- [ ] 检查 `RevisionHistorySheet`、`RevisionHistorySection`、`ActivityTimelineSection` 的状态、比较/恢复操作与滚动布局，修复全屏底部空白。
- [ ] 运行事务、DAO、模型及 UI 相关验证，更新 entry-revision-history 文档并提交。

### 3. Topbar

- [ ] 检查 `PasslyTopBar`、Vault/Detail/Settings 顶栏消费者，移除透明色与补偿背景，统一使用 M3 主题容器色。
- [ ] 删除不再需要的函数、插值或桥接；验证主题、滚动与导航顶栏并提交。

### 4. 应用内自动填充边界

- [ ] 检查 MainActivity、认证/填充 Activity、公共密码输入、备份密码输入与编辑保存退出路径。
- [ ] 在合适的 Android 宿主/输入边界统一阻止自身非凭据用途的填充和保存会话，避免按单个页面叠补丁。
- [ ] 保留对外 Autofill、Inline、BottomSheet 与无障碍回退能力；补充边界测试、Manifest/API 检查及正式文档后提交。

### 5. 职责清理与收尾

- [ ] 盘点 ViewModel、Route、Mapper、Coordinator 的职责和依赖，重点复核本轮涉及的安全设置、历史与编辑流程。
- [ ] 将业务编排归入现有应用服务/use case，页面状态留在 presentation；删除纯转发映射和无用文件，保留有实际转换语义的 mapper。
- [ ] 运行最终相关门禁，检查文档链接和遗留符号，逐项记录代码与验证证据；仅在五项均有证据时从本地 AGENTS.md 删去已完成待办。
