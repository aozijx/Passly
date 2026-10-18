# 远程内容与图标下载

状态：当前实现。

## 当前入口

详情页的图标编辑器允许用户显式输入 HTTPS 图片 URL。普通卡片渲染不会根据域名自动联网，也没有
Release 专用的 Google/DuckDuckGo provider 白名单。下载完成的内容必须先通过图片处理流水线，最终只展示
应用私有目录中的本地图标。

## URL 与网络边界

`FaviconUrlPolicy` 对初始 URL 和每一次重定向目标执行相同验证：

- 只接受 `https`，拒绝 HTTP、`javascript:`、`data:`、`file:` 等其他 scheme；
- 拒绝 URL user-info、IP 字面量、`localhost`、`.local`、`.internal` 和 `.lan`；
- DNS 解析结果只要包含 loopback、link-local、site-local、multicast、CGNAT 或 IPv6 unique-local 地址即拒绝；
- 下载客户端关闭自动重定向，最多手动跟随 5 次，每次重新执行 URL 和地址验证；
- 实际请求使用验证阶段得到的地址集合做固定 DNS 解析，缩小验证后换址窗口。

这降低 SSRF 和 DNS rebinding 风险，但不能把通用图标下载器当成高信任网络代理。解析器、系统网络栈或
上游代理被控制仍属于残余风险。

## 内容与存储边界

- 响应必须是 2xx 且 `Content-Type` 以 `image/` 开头；下载流最大 10 MiB，空响应被拒绝；
- 位图解码和裁剪限制输出为 512×512，并重新编码后写入应用私有目录；远端原始 HTML、SVG 或脚本不会作为
  可执行内容保存或展示；
- staging、转换输出和提升后的候选文件都有明确所有权；取消和失败路径在 `NonCancellable` 清理临时文件；
- 图标路径只作为私有资源引用保存，不能用于 WebView、外部文件 Intent 或任意路径读取。

## 维护约束

- 不允许从下载内容自动扩充白名单或信任其他 URL；
- 修改私网判断、重定向、大小上限或 MIME 规则时，必须同步更新 `FaviconUrlPolicyTest` 与
  `FaviconImageDownloaderTest`；
- 若以后引入固定 favicon provider，应作为新的获取策略显式建模，不能悄悄放宽当前任意 HTTPS URL 的
  安全校验。

## 相关实现

- `app/src/main/java/com/aozijx/passly/app/entry/favicon/FaviconUrlPolicy.kt`
- `app/src/main/java/com/aozijx/passly/app/entry/favicon/FaviconImageDownloader.kt`
- `app/src/main/java/com/aozijx/passly/app/entry/favicon/DefaultFaviconImageProcessor.kt`
- `app/src/main/java/com/aozijx/passly/app/entry/favicon/FaviconBitmapTransformer.kt`
