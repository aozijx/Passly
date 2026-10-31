# UI 宿主、导航与命名边界

状态：当前实现与约束。

## 顶栏配色

Vault、详情及公共导航顶栏共用 `passlyTopAppBarColors`，由 M3 组件绘制主题的 `surface` / `surfaceContainer`。
滚动配色由传入的 `TopAppBarScrollBehavior` 驱动；没有滚动行为的导航顶栏保持普通容器色。
不再提供透明顶栏配色，也不通过额外背景或手动颜色插值补偿透明容器。

## Compose 容器顺序

推荐的根节点顺序如下：

```text
AppTheme
└─ ProvidePasslyAdaptiveLayout
   └─ AppShell Scaffold
      ├─ bottomBar / navigation rail（固定 chrome）
      └─ content
         └─ SharedTransitionLayout
            └─ NavHost
               └─ destination Scaffold
```

`ProvidePasslyAdaptiveLayout` 只计算窗口尺寸决策，应位于导航之外，使所有目的地读取同一份
`LocalPasslyAdaptiveLayout`。`SharedTransitionLayout` 只包裹会参与转换的路由内容；底部导航、Navigation
Rail、全局 Snackbar 和认证遮罩属于 App Shell，不进入共享元素层。这样共享容器不会飞越或覆盖底部导航。

当前应用还没有根级底部导航，`SharedTransitionLayout` 位于 `PasslyNavHost` 内，因此现有 FAB 到新增页的转换
没有跨越固定底栏。以后增加底栏时，应把底栏提升到 `AppMainContent` 的 App Shell，并将
`PasslyNavHost` 放进 Scaffold 的 `content`，而不是把整个 Scaffold 放进 `SharedTransitionLayout`。

共享元素的阴影、overlay 裁剪与 visual overflow 处理见
[Compose 共享元素与阴影裁剪](../development/shared-element-transitions.md)。

每个目的地仍可使用自己的 Scaffold。目的地必须消费 App Shell 传入的 `PaddingValues`，并自行处理 IME；
共享元素使用内容坐标，不负责窗口 Insets。

模态目的地不得为绘制遮罩后背景而组合另一个业务 Route。根 `Surface` 提供宿主背景，模态 destination 只组合
自己的内容；若需要保留下层页面内容，应由导航宿主显式支持 overlay，并确保下层页面的 ViewModel、Paging、
可见数据订阅与 one-shot effect 不被重复组合。

## 页面层级与状态所有权

页面默认采用能表达职责的最短链路：`Route → Screen → Section/Component/Overlay`。

- `Route` 创建目的地作用域 ViewModel、收集状态与 Effect，并连接语义导航或平台副作用；
- `Screen` 负责页面 Scaffold、布局和只影响视觉的短期状态；
- `Section` 只消费自己的不可变 UiModel，并把同一 Feature 的语义 Action 直接向上发送；
- `Overlay` 只渲染 Sheet/Dialog/Snackbar，不自行保存、认证或导航；
- `Host` 只用于 Camera、认证 UI、权限请求等真实外部宿主或资源生命周期。

`Content` 只有在形成可独立预览、测试、复用或明确正文布局边界时才保留。若 `Host`、`Content`、包装函数或
Route 只原样转发参数、逐项改名回调或复制 UiState，应删除该层，让生产者提供最终模型、消费者直接发送业务
Action。不要用巨大 `Callbacks`、`Dependencies` 或 Context 对象把参数数量隐藏起来。

业务查询、筛选、编辑草稿、校验和保存结果由 ViewModel/UseCase 拥有；焦点、动画进度、菜单、滚动位置和临时
展开由最低共同使用者的 Compose state holder 拥有。一个状态不能同时由 Flow、`remember` 和回调同步。
SharedTransition/AnimatedVisibility 等框架 Scope 只传给真实消费者，不贯穿整棵页面树。

## AppShellViewModel 的职责

`AppShellViewModel` 只组合应用根界面需要的稳定投影，不拥有认证或数据库会话状态机：

- 将安全层发布的 `AuthenticationState` 穷尽映射为单一 `AppShellSessionMode`；不复制成多个布尔状态，也不保存认证 continuation；
- 完整观察 `DatabaseSessionFailureState` 的错误与清除事件，并投影为
  `AppShellDatabaseState.Available / Failed`；重试阶段只存在于 `Failed` 分支，不能与“数据库可用”
  同时出现，也不能提前清除安全层仍持有的错误；
- 通过 `DatabaseSessionRecovery` 请求重试，真正的数据库解锁、故障清除和认证状态发布仍由安全会话协调；
- 组合根主题、圆角和窗口安全策略等应用级显示设置。

`PasslyApp` 创建该 ViewModel，`AppShell` 只根据不可变 `AppShellUiState` 选择数据库错误、认证、恢复模式或
Vault 目的地。条目 CRUD、OTP、筛选、页面导航和敏感授权流程不得加入 Shell；这些职责分别留在 Feature、
导航宿主和安全边界。

Shell 的认证域切换采用 keyed 的即时替换，不对 Vault、认证、恢复或数据库错误内容做根级 `Crossfade`。
离开 Vault 时必须在下一组合帧释放其 NavHost、图作用域 ViewModel 和敏感 Compose state，不能为了视觉过渡
保留已失效的 Vault 内容。Vault 内部页面切换仍由 `PasslyNavHost` 的 Navigation 动画负责。

## Passly、Vault 与 Entry

Passly 是产品名；`vault` 是安全领域概念，不作为面向用户的品牌替代词。

- 新的领域模型统一使用 `Entry`。现有 `VaultEntryPageSource`、`VaultViewModel` 等名称属于 Vault Feature

  的页面/适配器命名，不代表另一个领域聚合；重命名时应连同消费者一次完成，不能再造桥接别名；
- 当前 `EntryEntity` 和 `Entry` 都没有 `vaultId`。应用当前只有一个加密凭据域；未来只有真正引入多个
  数据空间时，才新增明确的 `spaceId`/`vaultId` 合同；
- 数据库、备份格式和认证中的 Vault 表示“已加密凭据域”，仍是有效术语。此类持久化名称不能只做源码替换，
  必须与 schema/备份版本一起演进。

## EntryType、分类与关联账户

`EntryType` 是结构契约，决定 payload、验证器、详情组件和自动填充能力。详情页现在只读显示它，新增页也不再
提供没有落库能力的“自定义分类”下拉框。

详情页保存流程由 `DetailSaveState.Idle / Saving / Failed` 单独拥有当前编辑目标和失败码。Reducer 只接受
与当前 `Saving` 目标匹配的完成回调，过期回调不能关闭其他编辑器；UI 的保存进度由该状态派生，不再组合
nullable 的 `savingEdit` 与 `saveErrorCode`。

用户自定义分类尚未实现。实现时应新增独立 `categoryId` 或 Entry-Category 关联表；`tags` 继续表示多值标签。
当前也没有 `parentEntryId` 或 `ACCOUNT` 容器。LOGIN、OTP、PASSKEY 等保持原子 Entry，需要组合时使用
`entry_links` 的显式关系类型，不使用 `entryType`、标签或未来的空间 ID 冒充父子关系。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 组合根界面稳定状态 | `AppShellViewModel`、`AppShellReducer.reduce()` | [`AppShellViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt)、[`AppShellReducer.kt`](../../app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellReducer.kt) |
| 安装导航与共享转场宿主 | `PasslyNavHost()` | [`PasslyNavHost.kt`](../../app/src/main/java/com/aozijx/passly/feature/shell/presentation/navigation/PasslyNavHost.kt) |
