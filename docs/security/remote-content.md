# 远程内容与图标下载

## 当前实现

图标下载并非名存实亡：

- Release 默认只访问固定的 Google favicon 与 DuckDuckGo icon provider。
- HTML `<link rel="icon">` 抓取和站点直连只在 Debug 开放；显式白名单可以放宽远程主机。
- 仅接受 HTTP(S)，拒绝 `javascript:`、`data:`、`file:`、localhost、常见私网/链路本地 IPv4、
  `.local`/`.internal`/`.lan` 和 IPv6 字面量。
- 下载结果经图片解码后重新编码为应用私有 PNG；不会把 SVG/HTML/脚本原样保存后执行。

## 仍存在的边界

- 主机名检查不能彻底消除 DNS rebinding/解析时序差异；安全级别高于 favicon 的资源不能复用这套
  下载器。
- 图片解码器仍面对压缩炸弹、畸形 SVG 和高内存输入。应保持固定 provider，后续增加响应字节上限、
  MIME 校验、像素上限和专用受限网络客户端。
- 白名单是安全例外，不应从网站内容自动扩充，也不能接受通配的私网或用户信息 URL。
- 图标只能作为不可信位图展示，不能作为 WebView、文件打开 Intent 或可执行内容。

结论：Release 固定 provider 路径可继续使用；任意 URL 下载不应作为默认产品能力。
