# 诊断日志采集策略实施审查

**日期：** 2026-10-30

**范围：** 本地加密诊断日志的持久化等级策略、Data Management 设置入口、清除并发边界和两处设置页面的结果反馈。

## 结果

- Domain Settings Port 与 Proto 字段 7 持久化 `DISABLED`、`WARN_AND_ABOVE`、`INFO_AND_ABOVE`、`DEBUG_AND_ABOVE`；使用稳定 wire ID，未知值有明确默认行为，Proto/Room schema 版本不变。
- App 在本地 `LogRecordStore` 写入边界过滤；首次读到持久化配置前不写入本地文件。过滤不改变内存崩溃捕获、原 handler 委托、`LocalAlertPolicy` 或 Android Trace。
- 采集等级下拉框位于 Data Management 日志区；诊断查看页保持独立，只负责筛选、查看、导出、复制和清除。独立设置页、路由、ViewModel 和冗余状态已移除。
- 清除返回 `LogClearResult`。Encrypted store 在写入接受锁内排空普通队列和 emergency writer；500 ms 超时保留原记录并返回失败；成功删除后，清除边界之后接受的日志继续写入。Storage Settings 与 Diagnostics 均显示清除失败。

## 验证

- Proto mode 映射测试 2 项通过；Data Management ViewModel 与 Settings 导航聚焦测试 18 项通过；运行时策略、加密 writer timeout/post-clear 顺序、Storage Settings 和 Diagnostics 结果测试通过。
- 完整 `:data:testDebugUnitTest`、`:app:testDebugUnitTest`、`verifyArchitecture`、`:data:lintDebug`、`:app:lintDebug` 和 `git diff --check` 通过。

## 启动顺序复核（2026-10-30）

- RED 复现：配置策略仍在等待 DataStore 首次发出时，`start()` 已读取并推进上次进程退出游标；初始策略为 fail-closed，导致异常退出记录被过滤并永久丢失。
- 修复：先安装崩溃处理器，再启动策略 Flow；收到首次持久化模式后更新过滤策略并读取/记录上次退出事件。后续模式变化只更新策略，不重复消费退出记录。
- 验证：新增延迟策略 Flow 回归，旧实现稳定失败；修复后完整 `:app:testDebugUnitTest`、`verifyArchitecture`、`:app:lintDebug` 和 `git diff --check` 通过。

## 当前实现文档

- [诊断查看与导出](../features/diagnostics/viewer-and-export.md)
- [日志运行时与加密存储](../architecture/observability/runtime-and-storage.md)
