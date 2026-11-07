# Autofill 验证矩阵

状态：当前实现的验证范围；剩余自动化/设备验收由[Autofill 后续清单](../../superpowers/plans/2026-11-07-passly-autofill-followups.md)统一维护，不在迁移记录重复建项。

## 当前流程

- Legacy：锁定发现、认证、同 interaction 选择、结果先交付后关闭；SaveInfo 角色、完整会话直接保存与锁定 Inbox 入队。
- Legacy 三种展示：悬浮菜单、遵守键盘容量/规格的 Inline、认证后 BottomSheet；无 Inline 请求时菜单可用。
- 无障碍手动回退：服务启用后点击浮动按钮才扫描；认证后仅填原窗口/包名的可编辑节点；目标变化或超时拒绝。
- 日志记录展示模式和 Inline 能力、手动请求/不可用/交付/异常；只允许脱敏包名，不含节点文字或凭据。
- Credential Manager：locked begin-get、同 interaction 重发现、caller/allowed-user 拒绝、get/create result 合同、
  clear-state 和 PendingIntent identity。
- 三套入口不导入对方 Android 类型；共享层只处理平台无关合同。
- 文本 URI 与数字密码按 Android input class 加 variation 分类，不能因 variation 位重合把 URI 当密码。
- 进程死亡拒绝旧 interaction/token；超时、返回、取消和异常释放自己的 lease。
- 已有完整 App session 不被迟到 Autofill 完成锁定；临时 session 完成后不可再次显示上一轮秘密。
- result delivery 失败不伪造 delivered，也不提前关闭可重试会话。
- `setResult` 后、`finish` 前不得关闭临时会话；关闭期间的新请求等待完成后创建新的 locked interaction。
- 成功交付会过期未提交结果的遗留 interaction；并发且都已提交的 Legacy/Credential 结果分别记为 completed。

## Inbox 实现验收

- 已解锁 save 直接写 Entry，不创建 pending、不重复认证。
- 锁定 Legacy/CM create 密封 pending，不弹认证；fill/read 仍认证。
- 未 provision、容量满、超限、损坏 public metadata 明确失败，不虚报 Queued。
- private/public/fingerprint mismatch 停止导入并 quarantine，不写错误 Entry。
- header/AAD/ciphertext/tag/draft ID 任意篡改都失败；日志不含 payload、包名或异常 message。
- 数据库提交后、文件删除前崩溃由 import-log 唯一键阻止重复。
- 过期、旧 key-ID、retry/quarantine、全 root 512 KiB 上限按合同执行。
- Android Auto Backup/device transfer 和 Passly Backup 均不包含 Inbox。
- Vault 删除清理 key material、文件和 import log；真实 DEK rotation 原子重包 private envelope。

## Settings observation recovery

Autofill preferences and Android's selected Autofill service are observed as independent sources.
Each source keeps its own loading, ready, and failed state; a failure in one does not stop the other.
Failed preference reads retain the last readable settings for display, disable preference edits, and
offer a preference-only retry. A failed Android status read is displayed as unavailable, never as
“not enabled”; retry recreates only that status collector. If no previous preference value exists,
the page keeps its Autofill identity visible and offers a retry instead of rendering empty content.

The App source registers its `ContentObserver` before taking the initial snapshot. The observer flow
emits one registered snapshot, republishes after changes, and removes the observer when collection
fails or is cancelled. Registration exceptions and ordinary `Exception` failures from the initial
read or observer callback fail that source; fatal `Error` values remain fatal, including during
cleanup. A null secure setting remains a valid “not enabled” value. `AutofillServiceStatusObservationTest`
covers this flow contract, while `AutofillSettingsViewModelTest` covers replacing a failed system
source and cancelling its previous collector.

`AutofillSettingsViewModelTest` covers independent errors, last-known values, source-only retries,
recovery, write blocking while preferences are unavailable, and cancellation after the state
subscriber leaves. `AutofillSettingsSectionTest` covers the unknown status and read-only preferences;
it compiled but was not part of the device test run in this change.

## AVD field-fill acceptance

On 2026-11-07, the current Debug build was installed on `Pixel_8_Pro` (API 36).
The temporary system Autofill service was set to the Debug package's
`LegacyAutofillService`, and a fictional credential associated with
`com.example.androidproject` was used. Tapping the focused username field caused
Android to receive a Legacy response with authentication and no datasets while
the Vault was locked. Selecting the system's “Passly is locked” row opened the
single `AutofillAuthActivity`; after App Password authentication, the candidate
“Synthetic test” appeared. Selecting it returned to the target form, whose
username field contained `autofilltest` and whose password field was populated.
Android logged `AutofillManager.autofill()` for both target IDs and successful
`notifyValueChanged` callbacks (12-character username and 16-character password),
and Passly logged `autofill.result.delivered`. No app fatal exception appeared in
the captured interval. The temporary `secure/autofill_service` setting was
restored to its original `null` value and the temporary Debug Vault data was
cleared after capture.

This confirms one end-to-end Legacy field-fill path with `SYSTEM_INLINE` selected.
The request did not provide InlinePresentation support, so Android displayed its
system dropdown; Inline-specific presentation is still unverified. This run also
does not verify BottomSheet direct fill, configuration recreation, callback
failure/retry, or Accessibility/Credential Manager host teardown; those remain in
the Autofill follow-up plan.

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Autofill 设置源状态、隔离重试和写入门禁 | `AutofillSettingsViewModel.observeSource()` | [`AutofillSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/autofill/AutofillSettingsViewModel.kt) |
| Autofill 设置失败和重试 UI | `AutofillRoute`、`AutofillSettingsSection` | [`AutofillRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/autofill/AutofillRoute.kt)、[`AutofillSettingsSection.kt`](../../../app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/autofill/AutofillSettingsSection.kt) |
| Android 系统服务状态观察和 observer 清理 | `AndroidAutofillServiceStatusSource.observeServiceEnabled()`、`observeBooleanStatus()` | [`AndroidAutofillServiceStatusSource.kt`](../../../app/src/main/java/com/aozijx/passly/app/autofill/AndroidAutofillServiceStatusSource.kt)、[`AutofillServiceStatusObservation.kt`](../../../app/src/main/java/com/aozijx/passly/app/autofill/AutofillServiceStatusObservation.kt)、[`AutofillServiceStatusObservationTest.kt`](../../../app/src/test/java/com/aozijx/passly/app/autofill/AutofillServiceStatusObservationTest.kt) |
