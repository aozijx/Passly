2026-01-22 21:06:09  
**优化**
- 导航优化，使用 REORDER_TO_FRONT 保持页面状态。
- 页面载入渐变动画
- 丰富页面

2026-01-01 02:44:02  
**项目重构与UI优化：**
- 遵循MVVM思想，分离了`Detail`, `Animation`, `Profile`页面的UI与逻辑。
- 重新设计了`Home`页，采用更现代的信息流布局。
- 统一并优化了`IconTitleCard`与`ArticleCard`的UI风格，采用边框替代阴影。
- 修复了`GlanceWidget`点击后无法切换句子的Bug。
- `MainActivity`中增加了启动时通知权限请求的逻辑。

2025-12-09 00:48:34  
**初步探索与问题记录：**
- 实现了设备信息展示页，用于获取各类系统变量。
- 学习并实践了 `ModalBottomSheet` 的使用。
- 探索了 Compose 的基础动画语法。
- **记录**：遇到了部分 API 过时、语法不兼容的问题，增加了调试难度。

2025-12-01 16:49:08  
**UI组件与问题修复：**
- 使用 `Scaffold` 与 `CenterAlignedTopAppBar` 构建了带居中标题的页面布局。
- 修复了 `Card` 点击时涟漪效果溢出的问题，通过 `clip` 修饰符进行修正。

2025-11-30 20:42:06  
**项目初始化与基础功能学习：**
- 搭建了多 `Activity` 的基本项目结构，实现页面切换。
- 学习了 `Card` 控件的基本使用。
- 集成了 `Coil` 库，使用 `AsyncImage` 实现网络图片加载。
- **记录**：在项目初期遇到了 Compose 语法、依赖管理和权限变更（如 `Camera` 权限）等方面的挑战。
```text
android.permisition.CAMERA
android.permisition.CAMERA2
android.permisition.CAMERAX
```
