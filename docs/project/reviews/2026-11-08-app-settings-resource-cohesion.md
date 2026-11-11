# App 设置文案归属与本地化审查

**范围：** 记录设置资源集中、共享文案归属和中英文资源配对结果。当前资源维护约定见[设置功能总览](../../product/settings/overview.md#本地化文案)。

| 验收项 | 结果 |
| --- | --- |
| 设置文案 owner | 设置专属文案位于 `values/strings_settings.xml` 与 `values-en/strings_settings.xml`；认证提示 `authentication_app_password_hint` 留在认证资源，由共享 `AppPasswordSetDialog` 使用。 |
| 资源组和引用 | 当前两份设置 XML 各有 319 个资源项，key 与顺序一致；设置前缀资源未在其他 App XML 文件重复定义。 |
| 占位符 | 对应字符串与英文 `other` plural 项的格式占位符一致；英文 `one` plural 额外保留其语言规则所需的单数分支。 |
| 历史验收 | 原实施计划记录已检查资源引用迁移与旧定义清理，并完成 `git diff --check`；该次按计划明确未运行测试或构建。本轮复核重新核对了资源 XML、资源引用和归属。 |

文案改动只调整归属与表达，不改变设置行为。不同语义的相似文案继续保留独立 key，例如操作动作与完成状态。

## 代码位置

| 职责 | 类/资源 | 文件 |
| --- | --- | --- |
| Settings 页面专属中英文资源 | `strings_settings.xml` | [`strings_settings.xml`](../../../app/src/main/res/values/strings_settings.xml)、[`strings_settings.xml`](../../../app/src/main/res/values-en/strings_settings.xml) |
| 共享认证密码提示的 UI 消费方 | `AppPasswordSetDialog` | [`AppPasswordSetDialog.kt`](../../../app/src/main/java/com/aozijx/passly/presentation/shared/components/apppassword/AppPasswordSetDialog.kt) |
