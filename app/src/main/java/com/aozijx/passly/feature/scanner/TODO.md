# Scanner 临时审查 TODO

本文件只是 Scanner 目录内的临时入口；审查状态和结论统一记录在[长期审查计划 Task 84](../../../../../../../../../docs/project/work/plans/2026-10-30-passly-maintainability-todo.md#task-84)。完成审查与必要调整后删除本文件。

按 Task 84 中的 Scanner 项核对 `ScannerViewModel`、`ScannerUiStateReducer` 与 Route 对 `Work.Active`、可观察 UI state、反馈消费、宿主停止/恢复及取消/关闭的 owner 合同；如需调整，只提取可独立拥有状态和任务生命周期的职责，并在 ViewModel/Route 边界验证行为。当前专题合同见 [Scanner 输入与生命周期](../../../../../../../../../docs/product/scanner/input-lifecycle.md)；相机识别、decoder 和调用方结果交付不在此重复审查。
