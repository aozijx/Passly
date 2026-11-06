# 二维码扫描

状态：当前实现（代码核对：2026-11-06）。

Scanner 是应用内通用二维码入口。相机帧与相册图片最终都进入 `ScannerResultClassifier.classify(rawValue)`：有效 `otpauth://` 生成 OTP 结果，受支持的 `WIFI:` 载荷生成 Wi-Fi 结果，有效 HTTP/HTTPS 地址生成网页链接，其余非空内容成为普通文本；无法解析或不支持的专用格式不会创建条目。

OTP URI 使用 Domain `OtpAuthUriCodec.parse(rawValue)`；Wi-Fi 使用 `WifiQrPayloadParser.parse(rawValue)`。识别输入、结果分类、调用方导入与数据流分别见[扫描输入与生命周期](input-lifecycle.md)和[结果导入与调用方](import-results.md)。

相机权限、相机客户端和图片解码状态归 Scanner；二维码文本只保留在当前页面状态或一次性导入调用中，不写入数据库、设置、日志或 Navigation 参数。暂停、停止、页面销毁及认证撤权后的行为由两个流程页各自说明。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 统一分类 | `ScannerResultClassifier.classify(rawValue)` | [`ScannerResultClassifier.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultClassifier.kt) |
| OTP URI 分类规则 | `OtpAuthUriCodec.parse(rawValue)` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt) |
| Wi-Fi QR 分类规则 | `WifiQrPayloadParser.parse(rawValue)` | [`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
