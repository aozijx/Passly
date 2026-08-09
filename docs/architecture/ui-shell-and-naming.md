# UI 宿主、导航与命名边界

状态：当前实现与约束。

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

## MainViewModel 的职责

`MainViewModel` 是应用会话宿主，不是条目列表或导航 ViewModel。当前职责为：

- 观察认证状态，驱动数据库预热、故障恢复与锁定状态；
- 向页面提供认证、重新认证和敏感访问入口；
- 观察会影响应用根主题与自适应外观的设置；
- 首次解锁后触发 Blind Index 重建；
- 发布应用级数据库错误和恢复结果。

它直接耦合 `AuthenticationManager`、`DatabaseLifecycleUseCases`、`SearchIndexMaintenance` 和
`AppSettingsRepository`，并被 `MainActivity`、`AppMainContent`、`PasslyNavHost` 使用。条目 CRUD、OTP、
筛选和卡片展示属于 `VaultViewModel`，不应继续加入 `MainViewModel`。导航决策也应留在 Compose
导航宿主，ViewModel 只发语义 effect。

## Passly、Vault 与 Entry

Passly 是产品名；`vault` 是安全领域概念，不应再作为界面品牌文案。

- UI 类优先使用 `EntryListScreen`、`EntryListViewModel`、`PasslyData` 等产品语义；
- `VaultEntry` 后续可逐步收敛为 `EntryAggregate`；
- `vaultId` 当前表示逻辑数据空间所有权。只有产品正式改成 workspace/space 模型时才应迁移为
  `spaceId`，不能把它改名为 category 或 group；
- 数据库、备份格式和认证中的 Vault 表示“已加密凭据域”，仍是有效术语。此类持久化名称不能只做源码替换，
  必须与 schema/备份版本一起演进。

## EntryType、分类与关联账户

`EntryType` 是结构契约，决定 payload、验证器、详情组件和自动填充能力。详情页现在只读显示它，新增页也不再
提供没有落库能力的“自定义分类”下拉框。

用户自定义分类尚未实现。实现时应新增独立 `categoryId` 或 Entry-Category 关联表；`tags` 继续表示多值标签。
同一账户下的 LOGIN、OTP、PASSKEY 等仍是多个原子 Entry，通过 `parentEntryId` 指向 `ACCOUNT`，不使用
`entryType`、`vaultId` 或 category 来组合。
