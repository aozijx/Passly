# Presentation 状态管理

状态：当前开发规范（代码核对：2026-10-18）。

Passly 使用单向数据流，但不要求每个页面都有 Intent、Effect、Reducer 四件套。模式由状态复杂度决定。

| 页面 | 模式 |
| --- | --- |
| 静态说明 | Stateless Compose |
| Repository Flow 投影/简单设置 | UiState + 语义方法 |
| 多异步来源或复合输入 | UiState + UiAction + Effect |
| 有非法转换和多路异步结果 | Mutation + 纯 Reducer |

## 合同

- `UiState` 只表达渲染状态，不包含 Context、NavController、ViewModel、DAO 或长期敏感明文。
- 一个复合流程使用自己的 sealed branch，使 Loading/Success/Failure、打开/关闭、确认/执行等非法组合不可表示。
- `UiAction` 表达用户动作或宿主输入，不传 lambda、Compose 类型或格式化文案。
- `Effect` 表达导航、文件选择器、权限、Snackbar 等一次性平台动作，并使用类型化结果。
- Section/Screen 直接发送页面 Action，不定义字段一一对应的 Event 再由 Route 转发。

## ViewModel 和 Reducer

ViewModel 拥有页面生命周期、异步任务和 effect，只依赖内层端口。简单设置可以暴露语义方法；复杂页面统一
`onAction(action)`。对外只暴露不可变 `StateFlow`/`SharedFlow`。

Reducer 是可选的。只有多种异步结果汇合、需要阻止非法转换或状态转换已淹没 ViewModel 时才提取。Reducer 消费
Mutation/Result，不直接执行数据库、认证或导航，也不直接消费需要异步工作的 UiAction。

```text
UiAction -> ViewModel/handler -> UseCase/Repository
                              -> Mutation/Result -> Reducer -> UiState
Effect -----------------------------------------------------> Host/UI
```

## UI 与宿主

Screen/Route 收集状态和 effect，并连接导航、Activity Result 和平台 API；Content 只渲染状态并分发 Action。
焦点、Popup 展开、动画进度等瞬时交互可由 Compose state holder 管理，只要它不承载业务授权或可恢复流程。

Host 拥有 ViewModel、effect、导航和 Domain/UI 映射。被动 UI 文件不得为了复用而反向获取 ViewModel。

## 当前应用

- Vault 列表使用 `VaultUiAction`、ViewModel、Effect 与 Reducer；搜索焦点和下拉位移由 Compose state holder 管理。
- Backup 使用封闭工作流状态，导入与导出参数只存在于相应分支。
- Diagnostics 是独立二级页面，拥有 `DiagnosticsUiState`、Reducer/ViewModel 和 Route；数据管理页只持有容量和
  清理状态，不持有查看或导出状态。
- About 是无 ViewModel 的静态页面。
- 简单 Settings 页面直接投影 Proto-backed repository Flow，不为每个开关创建完整 MVI 套件。

UI 与 Domain 语义相同的设置选项直接使用 Domain 类型；需要不同展示语义时才建立 UI model，并使用穷尽映射，
不按枚举名称反射转换。简单设置 Content 直接发送页面 Action，不增加字段一一对应的 Event 转发层。
页面私有 Sheet、Dialog 和 UiModel 放在拥有其状态的 feature，而不是入口页面目录。
