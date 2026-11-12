# Scanner 行为与设备验收

相机、图片和结果交接的当前实现合同见[扫描输入与生命周期](../../../product/scanner/input-lifecycle.md)、[二维码扫描总览](../../../product/scanner/overview.md)和[扫描结果交接](../../../product/scanner/result-handoff.md)。相机 Host 的资源 owner 复核见[Scanner 相机与结果确认审查](../../reviews/2026-11-12-scanner-camera-and-result-owners.md)；其权限变化、重组和执行中释放验收由[并发计划 Task 4](2026-11-06-app-architecture-concurrency-todo.md#task-4)跟踪。

## 行为回归

- [ ] 覆盖 `ScannerRoute`、Activity Result 与显式关闭的交错：picker 结果先于 `HostStarted` 到达、宿主停止/恢复、overlay 退出动画期间快速重开，以及结果 owner callback 迟到；确认显式 `Stopped` 后不重建扫描 owner，旧会话不能覆盖新会话。
- [ ] 覆盖结果确认与 Vault/OTP/Wi-Fi/外链 owner 的组合：失败保留结果并可重试，并发确认/关闭不重复提交，关闭后的迟到回执不回写；验证 Wi-Fi seed 只按当前 token 清理，敏感扫码文本不进入日志或 Toast。
- [ ] 覆盖 `BarcodeImageDecoder` 在图片加载、ML Kit 处理和取消交错时关闭已创建的 client，并丢弃取消后的结果；同步失败与取消都不能泄漏 client。
- [ ] 为 `ScannerRecognitionRegion` 和候选筛选补确定性覆盖：无值/空白/框外候选后接框内有效码，`cornerPoints` 四角边界、无角点时 `boundingBox` fallback、无效坐标跳过处理；确认候选顺序不会让无效码阻断后续有效码。

## 设备验收

- [ ] 在连接的 Android 设备上记录设备型号与 API 级别；验证旋转、非方形预览与取景裁剪下框内二维码可识别、框外二维码被拒绝，并确认底部进入动画、返回关闭和结果卡片锚定动画稳定。图库选中图片仍按整张图片识别。
