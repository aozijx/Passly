# 二维码扫描

Scanner 是应用内通用二维码入口。CameraX/ML Kit 只负责识别并交付原始字符串，不判断它是 OTP、Wi-Fi、链接或普通文本。结果由 `ScannerRoute.resolveResult(rawValue)` 交给打开 Scanner 的页面；页面使用自己的 Domain parser 和业务入口完成转换，并返回包含展示策略与确认动作的 `ScannerResultPlan`。

Vault 首页负责解析 OTP 与 Wi-Fi，并分别调用 OTP 导入或导航到 Wi-Fi 编辑页；OTP 编辑器和 Wi-Fi 编辑器只调用各自的 parser，将解析结果交给当前 ViewModel。链接也由 Vault owner 处理。调用方在结果出现时生成一次 `ScannerResultPlan`，同时给出展示策略和确认动作；Vault 识别出的有效 OTP 不提供复制操作，主按钮标为“保存”，点击后直接调用 OTP 导入，不额外弹出添加确认框。OTP 编辑器识别出有效 OTP 时不提供复制操作并使用“填入”。其他允许复制的结果可直接点击原始内容文本复制，不占用额外按钮空间。Scanner 不维护内容类型映射，解析只在 owner 侧执行一次。

扫码原始内容只保留在当前页面状态或一次性回调中，不写入数据库、设置、日志或 Navigation 参数。相机、相册图片、状态 owner 与资源生命周期见[扫描输入与生命周期](input-lifecycle.md)；结果交接和调用方处理见[扫描结果交接](result-handoff.md)。

## 界面状态与操作

`ScannerScreen` 保留顶部返回和相册操作，分别使用 `back` 与 `scanner_action_album` 描述；相册入口按当前结果操作状态启用或禁用。图片处理中显示本地化进度文案。结果卡根据调用方计划显示“继续”“保存”或“填入”，并提供重新扫描；允许复制的扫码文本以 Button 语义和 `scanner_copy_text` 点击说明暴露。结果会话处于处理中、已关闭或已完成时，操作状态映射会门控重复提交。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 将相机/图片结果保存为原始字符串 | `ScannerViewModel.completeScan(raw: String)` 创建 `ScannerResult(rawValue = raw)`；图片回调先经 `decodeImage(image: ImageRef)` | [`ScannerViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/viewmodel/ScannerViewModel.kt)、[`ScannerResult.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/result/ScannerResult.kt) |
| 调用方一次解析并创建展示/确认计划 | `ScannerRoute(resolveResult: (String) -> ScannerResultPlan, onDismiss: () -> Unit)` 调用 `resolveResult(rawValue)`；返回 `ScannerResultPlan(presentation, confirm)` | [`ScannerResultPlan.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/integration/ScannerResultPlan.kt)、[`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/route/ScannerRoute.kt) |
| 映射扫描状态并显示顶部、处理中和结果操作 | `ScannerScreen()`、`ScannerResultCard()`、`mapScannerScreenState()`、`mapScannerResultActionState()` | [`ScannerScreen.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ui/ScannerScreen.kt)、[`ScannerResultCard.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ui/ScannerResultCard.kt)、[`ScannerScreenUiMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/state/ScannerScreenUiMapper.kt) |
| 图片解码器接收 URI 字符串包装值并返回结构化结果 | `BarcodeImageDecoder.decode(ref: ImageRef): BarcodeImageResult`，成功分支将非空 `rawValue` 交回 `completeScan(raw)` | [`BarcodeImageDecoder.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/image/BarcodeImageDecoder.kt)、[`ScannerViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/viewmodel/ScannerViewModel.kt) |
| 各 owner 的 OTP 与 Wi-Fi 转换 | `OtpAuthUriCodec.parse(rawValue)` 返回 `OtpUriParseResult`；`WifiQrPayloadParser.parse(rawValue)` 返回 `WifiQrParseResult`，owner 仅捕获 `Parsed.config` / `Parsed.payload` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt)、[`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
