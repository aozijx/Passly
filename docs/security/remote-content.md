# 远程内容与图标下载

## 当前实现

图标下载并非名存实亡：

- Release 默认只访问固定的 Google favicon 与 DuckDuckGo icon provider。
- HTML `<link rel="icon">` 抓取和站点直连只在 Debug 开放；显式白名单可以放宽远程主机。
- 仅接受 HTTP(S)，拒绝 `javascript:`、`data:`、`file:`、localhost、常见私网/链路本地 IPv4、
  `.local`/`.internal`/`.lan` 和 IPv6 字面量。
- 下载结果经图片解码后重新编码为应用私有 PNG；不会把 SVG/HTML/脚本原样保存后执行。
- 普通卡片渲染不会根据域名直接联网；只展示已保存到应用私有目录的图标。
- 下载解码固定请求为最多 512×512，且不注册 SVG 解码器。

## 仍存在的边界

- 主机名检查不能彻底消除 DNS rebinding/解析时序差异；安全级别高于 favicon 的资源不能复用这套
  下载器。
- 图片解码器仍面对压缩炸弹和畸形位图。应保持固定 provider，后续增加响应字节上限、MIME 校验和专用
  受限网络客户端。
- 白名单是安全例外，不应从网站内容自动扩充，也不能接受通配的私网或用户信息 URL。
- 图标只能作为不可信位图展示，不能作为 WebView、文件打开 Intent 或可执行内容。

结论：Release 固定 provider 路径可继续使用；任意 URL 下载不应作为默认产品能力。
