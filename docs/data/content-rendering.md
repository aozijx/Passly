# 内容渲染边界

## Markdown 备注

详情页备注使用 `multiplatform-markdown-renderer-m3` 的 GFM 解析器，并以接近 Notion 的紧凑
标题层级、正文和列表排版展示。编辑状态仍保存原始 Markdown 文本，渲染结果不是存储格式。

当前边界：

- 支持标题、强调、列表、引用、代码块、表格、任务列表等 GFM 语法。
- 不实现 Notion block 数据模型、数据库视图、折叠块、提及、同步块或 Notion API 导入。
- 默认 `NoOpImageTransformer` 不加载 Markdown 中的远程图片，避免详情页因为备注内容产生
  隐式网络请求、追踪像素或 SSRF。
- Markdown 中的 HTML 不是受信任 UI 扩展点；不得接入 WebView 执行脚本。
- 编辑器当前是纯文本输入。所见即所得、语法工具栏和预览切换属于后续 UI 工作。

因此，“渲染成 Notion 风格”在当前实现中指视觉排版接近，而不是声称兼容 Notion 的专有内容模型。
