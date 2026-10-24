# Compose 与 Material 规范

状态：当前开发规范。

## Composable API

页面 Screen 可连接 ViewModel、导航和 effect；纯 Content 只接收状态和事件；可复用组件不读取 ViewModel。
`modifier` 是第一个可选参数，回调命名为 `onXxx`。业务对象、数据库、加密和认证任务不能在 Composable 中创建。

`remember` 保存 UI 瞬时状态；页面可恢复业务状态由 ViewModel/持久化源拥有。`LaunchedEffect` 只负责 effect 收集、
一次启动和 UI 副作用。Flow 使用 lifecycle-aware collection。

## Material 和主题

- 优先使用 Material 3/Expressive 官方组件、Defaults、`MaterialTheme` 排版、颜色、shape 和对齐。
- 动态颜色直接使用 Android 提供的动态 `ColorScheme`；手动主题由三个协调种子生成完整的亮色/暗色
  `ColorScheme`。背景和容器来自低 chroma 的 neutral palette，不在默认画布上叠加可调颜色遮罩。
- 手动主题切换可以对完整色彩角色做短时插值，但不能通过全屏 overlay 或二次 tint 模拟主题变化。
- 应用圆角设置通过 `MaterialTheme.shapes` 统一传播，不复制组件 shape/gap 算法。
- 设置分组使用 `SegmentedListItem`；没有图标时不保留不可见 leading 占位。
- Switch 只由自身表达 checked，不用容器形状或颜色重复表达。
- 只有 Material 主题无法表达的产品语义才新增项目 token。
- Material 表现层不能决定认证、数据库、导出或其他业务规则。

## 输入和表单

- 单行普通输入可复用 `NextFocusTextField`；需要特殊键盘、错误、敏感清理或多行行为时直接使用适合的 Material
  输入组件。
- 密码输入复用专用小组件处理显隐、错误和清理，不创建万能表单引擎。
- OTP、Bank Card、Wi-Fi、SSH、Identity、Seed、Passkey 使用各自的字段规则和 Host，不塞进一个通用 section。
- trim 由字段语义决定；密码是否 trim 必须显式，不能套用普通文本规则。

## Sheet、Dialog 和菜单

- 文件名体现真实形态：`XxxSheet`、`XxxDialog`；组件只收集输入，提交交给页面所有者。
- `ModalBottomSheet` 的外层 `modifier` 约束 Sheet 容器；内容跟随高度的 Sheet 不对外层使用 `fillMaxSize()`。
- 互斥 overlay 使用 sealed 状态而不是多个 Boolean。
- Popup 保持稳定锚点，内容切换不反复销毁 Popup；转场只让目标内容参与尺寸测量。
- 破坏性动作必须有明确确认，不能把“关闭”和“确认”绑定为同一无语义回调。

## 资源、无障碍和导航

- 固定 UI 文案进入资源；ViewModel 和 Effect 不传硬编码中文。
- 颜色不能是错误/等级的唯一信号，必须同时保留文字或语义。
- 页面转场保留 predictive back；窗口单双栏变化不是导航动作。
- Route 连接导航出口，Content 不依赖全局 NavController。

共享元素的实现与排障见[共享元素转场](shared-element-transitions.md)。
