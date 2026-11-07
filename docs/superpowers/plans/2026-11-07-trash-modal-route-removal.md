# 回收站弹层移除导航目的地

- 决策：Settings 点击回收站时由应用组合根显示现有 `TrashRoute` 底部弹层，不再导航到 `vault/trash`；ViewModel 使用当前导航栈 owner，弹层关闭时停止查询、取消命令并使旧会话结果失效。
- 依据：旧 `AppRoute.Trash` 只注册 `TrashRoute`，而 `TrashRoute` 始终显示 `TrashBottomSheet`；弹层无需独立页面状态或路由参数。
- 涉及：`PasslyAppNavigation`、Settings/Vault 图注册、`AppRoute`、`TrashRoute`、`docs/features/vault/list-and-search.md`。
- 验证：全局搜索确认生产代码已无 `AppRoute.Trash` / `vault/trash`；`:app:compileDebugKotlin --no-parallel --max-workers=2` 成功；`git diff --check` 通过。代码复审发现临时 ViewModelStore owner 缺少 Hilt SavedState extras，已改为使用 Navigation back stack entry 并重新编译。
