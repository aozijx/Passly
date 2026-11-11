# 条目二维码展示

状态：当前实现。

Wi-Fi 详情页通过“显示网络二维码”生成连接码；OTP 详情页保留二维码按钮。两者共用详情页的临时二维码
会话与弹层，文案分别说明连接网络和导入动态口令。

二维码区域使用 Material 3 `Surface`：主题大圆角、1 dp `outlineVariant` 描边及
`surfaceContainerLow` 底色，四周保留 16 dp 内边距。码图保持黑白和编码器留白，不随深色主题反色；
加载与失败状态沿用同一容器，避免状态切换时边框跳动。

## 内容与授权

`DetailViewModel.onAction(ShowEntryQr)` 将当前 `uiState.entry` 传给 `DetailQrSession.request(entry)`。Session 先关闭旧请求，再检查自身 scope 与完整会话访问权，按条目类型选择 `Otp` 或 `Wifi`，并发布 `Authorizing(kind)`。它把原 `Entry` 和 `onAuthorized` 回调交给 `ExportEntryQrUseCase.invoke(entry, onAuthorized)`；授权闭包开始后，回调仅在请求 generation 仍有效且会话访问权仍有效时将状态推进到 `Loading(kind)`。

`ExportEntryQrUseCase` 只处理 Wi-Fi 和 OTP。Wi-Fi 调用 `authorizationGate.authorize(Global(REVEAL_SECRET))`，在闭包内以 `entry.id` 调用 `SensitiveFieldRepository.readBundle(entry.id)`，并将最新的 `ssid`、密码（空值转为空串）、安全类型和隐藏标记传给 `WifiQrPayloadFormatter.format(WifiQrPayload(...))`。OTP 从传入的 `entry.secret.otp.config` 取非敏感配置，用 `entry.id`、`OTP_SECRET` 和 `REVEAL` 构造 `SensitiveFields` scope；获准后将 permit 传给 `SensitiveFieldRepository.reveal(entry.id, OTP_SECRET, REVEAL, permit)`，把揭示的 secret 放进配置副本后调用 `OtpAuthUriCodec.format(config, entry.title)`。两种格式化成功后都经 `ready(kind, content)` 建立 `OwnedChars`、调用 `activityRecorder.recordUsage(entry.id.value, EXPORT)` 并返回 `Ready`；失败格式不记录导出活动。拒绝返回 `Denied`，用户取消返回 `Cancelled`，均不会生成内容。

OTP 的 URI 由 Domain 编解码器生成，Base64 密钥会转换为等价 Base32；无效配置不输出载荷。
元数据、参数校验和转义规则见[动态口令 URI 导入与导出](otp-uri.md)。

Wi-Fi 载荷由 Domain 的 `WifiQrPayloadFormatter` 生成，使用稳定的 `nopass`、`WEP`、`WPA` 键和显式隐藏
网络标志，并转义反斜杠、分号、逗号、冒号和双引号。开放网络不包含密码；不完整或不符合现有 Wi-Fi
校验规则的凭据显示生成失败。格式遵循 [ZXing Wi-Fi 内容约定](https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11)。

手动创建和扫码种子中的网络名原样映射到 `WIFI_SSID` 与初始标题，首尾空格不做 trim；全空白网络名仍校验失败。
`AddWifiViewModelTest` 通过实际创建用例检查交给 Repository 端口的名称，再将该条目编码为二维码载荷，验证中文、空格和转义字符的往返。

## 生命周期与边界

`DetailQrSession` 用 `Hidden / Authorizing / Loading / Ready / Failed` 表达互斥阶段。导出用例返回的 `OwnedChars` 只在页面工作流中短暂存在：Session 在 `Dispatchers.Default` 上调用 `QrCodeEncoder.encodeMatrix()`，随后立即擦除字符缓冲。只有结果仍属于当前 generation 且会话仍可访问时，`Ready` 才持有可擦除的 `QrCodeMatrix`；取消会重抛并隐藏当前请求，过期或撤权的矩阵会在发布前擦除。关闭、进入后台、切换条目、锁定或详情页销毁都会取消请求并擦除矩阵；迟到结果不能重新打开弹层。二维码不经过导航、SavedState、Intent、日志或磁盘。

Core 的 `QrCodeEncoder.encodeMatrix()` 使用 UTF-8 生成紧凑的二维码模块矩阵。`QrCodeMatrix` 由页面 Session 持有，并在关闭时清零内部位集合。`EntryQrSheet` 只按矩阵绘制黑白模块和无障碍描述，不读取二维码载荷、不编码内容，也不持有 Bitmap。导出用例使用结构化日志记录异常；矩阵编码失败映射到可见失败态。日志不包含网络名、密码或二维码载荷。

## 验证

Domain 测试覆盖稳定键、特殊字符、开放网络、隐藏网络和损坏凭据；Core 单测覆盖矩阵编码与关闭擦除；App 单测覆盖授权顺序、读取最新密码、
字符缓冲和矩阵擦除、取消传播、锁定清理和迟到结果。AndroidTest 使用真实 Bitmap 和 ZXing 解码检查中文及特殊字符，并检查
Wi-Fi 展示入口。无设备时只能确认 AndroidTest 编译与测试 APK 打包，不能宣称真实扫码连接已验证。
`DetailQrSessionTest` 使用与生产相同的双参数 export 合同，可明确控制授权回调，并验证授权前保持 `Authorizing`、进入授权闭包后转为 `Loading` 的时序。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 从 Detail Action 请求或关闭当前条目二维码 | `onAction(ShowEntryQr)` -> `_uiState.value.entry?.let(qrSession::request)`；`onAction(DismissEntryQr)` -> `qrSession.close()` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt) |
| 授权读取并生成条目二维码内容 | `ExportEntryQrUseCase.invoke()` | [`ExportEntryQrUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/ExportEntryQrUseCase.kt) |
| 管理授权、加载及二维码显示状态 | `DetailQrState`、`DetailQrSession.request()` | [`DetailQrState.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailQrState.kt)、[`DetailQrSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailQrSession.kt) |
| 在授权作用域开始时切换 Authorizing -> Loading，完成后发布或擦除结果 | `DetailQrSession.request(entry)` -> `export(entry, onAuthorized)` -> `ExportEntryQrUseCase.invoke(entry, onAuthorized)` | [`DetailQrSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailQrSession.kt)、[`ExportEntryQrUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/ExportEntryQrUseCase.kt) |
| 页面停止时关闭请求；Authorizing 时不显示二维码弹层 | `Lifecycle.Event.ON_STOP` -> `onAction(DismissEntryQr)`；`EntryQrSheet(state)` 对 `Authorizing` 直接返回 | [`DetailRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailRoute.kt)、[`EntryQrSheet.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/EntryQrSheet.kt) |
| 格式化 Wi-Fi 二维码载荷 | `WifiQrPayloadFormatter.format()` | [`WifiQrPayloadFormatter.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadFormatter.kt) |
| 将授权后的字符缓冲编码成可擦除矩阵 | `QrCodeEncoder.encodeMatrix()` -> `QrCodeMatrix.close()` | [`QrCodeEncoder.kt`](../../../core/src/main/kotlin/com/aozijx/passly/core/platform/qr/QrCodeEncoder.kt)、[`QrCodeMatrix.kt`](../../../core/src/main/kotlin/com/aozijx/passly/core/platform/qr/QrCodeMatrix.kt) |
| 按矩阵渲染黑白二维码及无障碍描述 | `EntryQrImage(matrix)` | [`EntryQrSheet.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/EntryQrSheet.kt) |
