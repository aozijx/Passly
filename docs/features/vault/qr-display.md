# 条目二维码展示

状态：当前实现。

Wi-Fi 详情页通过“显示网络二维码”生成连接码；OTP 详情页保留二维码按钮。两者共用详情页的临时二维码
会话与弹层，文案分别说明连接网络和导入动态口令。

二维码区域使用 Material 3 `Surface`：主题大圆角、1 dp `outlineVariant` 描边及
`surfaceContainerLow` 底色，四周保留 16 dp 内边距。码图保持黑白和编码器留白，不随深色主题反色；
加载与失败状态沿用同一容器，避免状态切换时边框跳动。

## 内容与授权

`ExportEntryQrUseCase` 只处理 Wi-Fi 和 OTP。Wi-Fi 使用 `REVEAL_SECRET` 授权，在授权闭包内通过
`SensitiveFieldRepository.readBundle` 读取当前网络凭据，不使用页面缓存的密码。OTP 仍按 `OTP_SECRET`
的敏感字段作用域授权和读取。拒绝或取消授权不生成二维码；成功生成才记录查看行为。

OTP 的 URI 由 Domain 编解码器生成，Base64 密钥会转换为等价 Base32；无效配置不输出载荷。
元数据、参数校验和转义规则见[动态口令 URI 导入与导出](otp-uri.md)。

Wi-Fi 载荷由 Domain 的 `WifiQrPayloadFormatter` 生成，使用稳定的 `nopass`、`WEP`、`WPA` 键和显式隐藏
网络标志，并转义反斜杠、分号、逗号、冒号和双引号。开放网络不包含密码；不完整或不符合现有 Wi-Fi
校验规则的凭据显示生成失败。格式遵循 [ZXing Wi-Fi 内容约定](https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11)。

## 生命周期与边界

`DetailQrSession` 用 `Hidden / Loading / Ready / Failed` 表达互斥阶段，持有可擦除的二维码字符缓冲，向 UI
只暴露受作用域控制的读取接口。关闭、进入后台、切换条目、锁定或详情页销毁都会取消请求并擦除内容；
迟到的请求结果必须擦除，不能重新打开弹层。二维码不经过导航、SavedState、Intent、日志或磁盘。

Core 的 `QrCodeEncoder` 使用 UTF-8 生成位图，支持中文网络名。会话关闭立即清空派生位图的像素，弹层移除时释放位图；
UI 的无障碍描述只包含“二维码”，不包含网络密码或 OTP 密钥。失败日志仅记录结构化失败及异常类型，
不包含网络名、密码或二维码载荷。

## 验证

Domain 测试覆盖稳定键、特殊字符、开放网络、隐藏网络和损坏凭据；App 单测覆盖授权顺序、读取最新密码、
取消传播、锁定清理和迟到结果。AndroidTest 使用真实 Bitmap 和 ZXing 解码检查中文及特殊字符，并检查
Wi-Fi 展示入口。无设备时只能确认 AndroidTest 编译与测试 APK 打包，不能宣称真实扫码连接已验证。
