# 内容渲染边界

状态：当前实现（代码核对：2026-11-06）。

## Markdown 备注

详情页备注使用 Android 原生 Markwon，通过 Compose `AndroidView` 承载 `TextView`。编辑状态仍保存
原始 Markdown 文本，渲染结果不是存储格式。

当前边界：

- 支持标题、强调、列表、引用、代码块、表格、任务列表等 GFM 语法。
- 不实现 Notion block 数据模型、数据库视图、折叠块、提及、同步块或 Notion API 导入。
- 不注册 Markwon 图片插件，因此不会加载 Markdown 中的远程图片，避免详情页因为备注内容产生
  隐式网络请求、追踪像素或 SSRF。
- Markdown 中的 HTML 不是受信任 UI 扩展点；不得接入 WebView 执行脚本。
- 编辑器当前是纯文本输入。所见即所得、语法工具栏和预览切换属于后续 UI 工作。

因此，“渲染成 Notion 风格”在当前实现中指视觉排版接近，而不是声称兼容 Notion 的专有内容模型。

## 详情备注调用链

当前唯一生产调用方是 Vault 详情的 `NotesSection`：只读分支将 `model.notes`、`startEditing` 和本地化 `noNotesLabel` 原样传给
`PasslyMarkdownDocument(content, modifier, onLongClick, emptyContent)`。编辑分支仍使用纯文本输入，Markdown 源字符串才是
保存值；打开编辑器由 `startEditing` 触发。

`PasslyMarkdownDocument` 调 `normalizeMarkdownDocument(content)`，该函数对非空值执行 `trim()` 并将空白结果转成 `null`。
空内容调用 `emptyContent`；非空内容调用 `MarkwonDocumentView(content, onLongClick)`。该边界仅规范化显示副本，不回写或修改
Repository 中的原始备注。

`MarkwonDocumentView` 用 `remember(applicationContext)` 缓存 `createMarkwon(applicationContext)`；Builder 明确注册
`StrikethroughPlugin`、`TablePlugin` 与 `TaskListPlugin`。`AndroidView.factory` 创建 `PasslyMarkdownTextView`，其设置透明背景、
可选择文本、`LinkMovementMethod` 和无额外 padding；`update` 设置当前 Material 文字/链接颜色、字号和长按监听，只有
`textView.renderedSource != content` 才调用 `markwon.setMarkdown(textView, content)` 并更新 renderedSource。

`NotesSection` 同时在 Compose 容器 `combinedClickable` 和 TextView 的 `onLongClick` 上绑定同一 `startEditing`。TextView
开启文本选择后可能自行消费长按，因此保留容器和文本视图两层入口；不要把此处机械合并成只依赖父容器的回调。
现有 [`MarkdownDocumentPolicyTest`](../../core/src/test/java/com/aozijx/passly/core/ui/components/markdown/MarkdownDocumentPolicyTest.kt)
只证明外层空白规范化与空内容委托，不证明 Markwon 插件实际渲染或 Android 链接点击行为。


## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Vault 详情备注 UI owner | `NotesSection()` | [`NotesSection.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/NotesSection.kt) |
| Markdown 显示副本规范化与空状态选择 | `normalizeMarkdownDocument(content)`、`PasslyMarkdownDocument(content, modifier, onLongClick, emptyContent)` | [`MarkdownDocumentPolicy.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/ui/components/markdown/MarkdownDocumentPolicy.kt)、[`PasslyMarkdownDocument.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/ui/components/markdown/PasslyMarkdownDocument.kt) |
| Markwon/AndroidView 渲染与插件配置 | `MarkwonDocumentView(content, modifier, onLongClick)`、`createMarkwon(context)` | [`MarkwonDocumentView.kt`](../../core/src/main/kotlin/com/aozijx/passly/core/ui/components/markdown/MarkwonDocumentView.kt) |
