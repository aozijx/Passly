# 银行卡完整表单编辑保留付款平台审查

## 当前合同

银行卡表单没有 `PAYMENT_PLATFORM` 控件，`AddBankCardFormState.toEntryDraft()` 会省略该字段。Domain `EntryFormEditor.apply()` 进行完整表单替换时，对此字段保留原 `CardCredential.paymentPlatform`；显式提供 `PAYMENT_PLATFORM` 的 draft 则采用新物化值。这样省略字段表达编辑表单不拥有该字段，不会意外清除旧数据。正式行为见[银行卡资料与有效期](../../product/vault/card-entry.md#表单映射与保存参数链)。

## 验收证据

`EntryFormEditorTest.cardFormPreservesPaymentPlatformWhenEditorDoesNotOwnIt` 验证银行卡编辑省略字段时保留旧值；`cardFormUsesPaymentPlatformWhenDraftExplicitlyProvidesIt` 验证显式输入覆盖旧值。2026-11-11 执行 `:domain:test --tests '*EntryFormEditorTest' --no-parallel --max-workers=2`，5 项通过、0 failures/errors。该验证覆盖 Domain 替换策略，不代替 Task 42 中仍开放的 Detail Route 与完整编辑 owner 生命周期验收。

## 代码位置

| 职责 | 类/测试 | 源码文件 |
| --- | --- | --- |
| 银行卡表单映射及省略付款平台字段 | `AddBankCardFormState.toEntryDraft()` | [`BankCardFormMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/bankcard/BankCardFormMapper.kt) |
| 完整表单替换时保留未提供的平台字段 | `EntryFormEditor.apply(entry, draft)` | [`EntryFormEditor.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryFormEditor.kt) |
| 省略字段保留与显式字段替换回归 | `EntryFormEditorTest` | [`EntryFormEditorTest.kt`](../../../domain/src/test/kotlin/com/aozijx/passly/domain/entry/policy/EntryFormEditorTest.kt) |
