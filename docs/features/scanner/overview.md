# 二维码扫描

Scanner 是应用内通用二维码入口。CameraX/ML Kit 只负责识别并交付原始字符串，不判断它是 OTP、Wi-Fi、链接或普通文本。结果由 `ScannerRoute.resolveResult(rawValue)` 交给打开 Scanner 的页面；页面使用自己的 Domain parser 和业务入口完成转换，并返回包含展示策略与确认动作的 `ScannerResultPlan`。

Vault 首页负责解析 OTP 与 Wi-Fi，并分别调用 OTP 导入或导航到 Wi-Fi 编辑页；OTP 编辑器和 Wi-Fi 编辑器只调用各自的 parser，将解析结果交给当前 ViewModel。链接也由 Vault owner 处理。调用方在结果出现时生成一次 `ScannerResultPlan`，同时给出展示策略和确认动作；Vault 的有效 OTP 隐藏复制按钮，主按钮标为“保存”，点击后直接调用 OTP 导入，不额外弹出添加确认框。OTP 编辑器隐藏复制并使用“填入”。Scanner 不维护内容类型映射，解析只在 owner 侧执行一次。

扫码原始内容只保留在当前页面状态或一次性回调中，不写入数据库、设置、日志或 Navigation 参数。相机、相册图片、状态 owner 与资源生命周期见[扫描输入与生命周期](input-lifecycle.md)；结果交接和调用方处理见[扫描结果交接](import-results.md)。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 将相机/图片结果保存为原始字符串 | `ScannerViewModel.completeScan(raw)`、`ScannerResult(rawValue)` | [`ScannerViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerViewModel.kt)、[`ScannerResult.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResult.kt) |
| 调用方一次解析并创建展示/确认计划 | `ScannerResultPlan`、`ScannerRoute(resolveResult)` | [`ScannerResultPlan.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultPlan.kt)、[`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRoute.kt) |
| 各 owner 的 OTP 与 Wi-Fi 转换 | `OtpAuthUriCodec.parse(rawValue)`、`WifiQrPayloadParser.parse(rawValue)` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt)、[`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
