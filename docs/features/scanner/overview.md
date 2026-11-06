# 二维码扫描

Scanner 是应用内通用二维码入口。CameraX/ML Kit 只负责识别并交付原始字符串，不判断它是 OTP、Wi-Fi、链接或普通文本。结果由 `ScannerRoute.onResult(rawValue)` 交给打开 Scanner 的页面；每个页面使用自己的 Domain parser 和业务入口完成转换。

Vault 首页负责解析 OTP 与 Wi-Fi，并分别调用 OTP 导入或导航到 Wi-Fi 编辑页；OTP 编辑器和 Wi-Fi 编辑器只调用各自的 parser，将解析结果交给当前 ViewModel。链接也由 Vault owner 处理。结果卡片只显示原始内容并提供重扫、复制和交给当前页面处理，不维护按类型映射的 UI 分支。

扫码原始内容只保留在当前页面状态或一次性回调中，不写入数据库、设置、日志或 Navigation 参数。相机、相册图片、状态 owner 与资源生命周期见[扫描输入与生命周期](input-lifecycle.md)；结果交接和调用方处理见[扫描结果交接](import-results.md)。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 将相机/图片结果保存为原始字符串 | `ScannerViewModel.completeScan(raw)`、`ScannerResult(rawValue)` | [`ScannerViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerViewModel.kt)、[`ScannerResult.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResult.kt) |
| 将字符串交给调用方并处理确认回执 | `ScannerRoute(onResult)`、`ScannerResultSession.confirm { onResult(rawValue) }` | [`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRoute.kt)、[`ScannerResultSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultSession.kt) |
| 各 owner 的 OTP 与 Wi-Fi 转换 | `OtpAuthUriCodec.parse(rawValue)`、`WifiQrPayloadParser.parse(rawValue)` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt)、[`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
