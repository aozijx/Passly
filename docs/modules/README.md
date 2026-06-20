# 模块文档索引

用于按模块记录职责、边界、调用链、改造状态与待办事项。

## 目录约定

- 路径：`docs/modules/<layer-or-feature>/<module>.md`
- 命名：建议小写英文（例如 `core/error.md`、`features/auth.md`）
- 状态：在模块文档顶部标记 `Draft` / `Active` / `Deprecated`

## 当前模块

- `core/error`：`docs/modules/core/error.md`（已创建）

## 待补模块（建议）

- `features/auth`
- `features/vault`
- `data/backup`
- `service/autofill`

## 模块文档模板

```markdown
# <模块名>

- Status: Draft
- Owner: <可选>
- Last Updated: YYYY-MM-DD

## 1. 模块职责

## 2. 关键数据结构/状态

## 3. 分层边界与约束

## 4. 关键调用链

## 5. 已完成改造

## 6. 待修改项（P0/P1/P2）

## 7. 验证清单

## 8. 相关文件
```