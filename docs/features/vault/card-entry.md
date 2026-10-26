# 银行卡创建与有效期

当前实现：`AddBankCardViewModel` 将表单转换为草稿，经 `CreateEntryUseCase` 校验后写入 Repository。
标题与卡号必填，有效期可不设置。卡号规则由 Domain 的 `CardNumberValidator` 提供。

有效期年月由 Domain 的 `CardExpiry` 表达：月份为 1–12，年份为 1–9999，创建时统一写为
`MM/YYYY`（例如 `03/2030`）。`EntryValidation` 使用同一解析规则，拒绝非法月份、零年份和两位年份，
不推测世纪。未选择有效期由 `CardExpirySelection.NotSet` 表达，草稿中省略该字段。

页面年月选择器提供当年至未来 20 年，这是输入界面的选择范围；Domain 不以当前时间拒绝已过期银行卡，
也不将格式校验视为支付可用性验证。UI 仅持有选中状态，年月验证与格式化不重复实现。

Bitwarden 的分离年月字段在适配边界允许一位或两位月份，完整合法值经 `CardExpiry` 校验并统一为
`MM/YYYY`。缺失部分、非法月份或两位年份不生成有效期；原始值以隐藏自定义字段 `Bitwarden.expMonth`、
`Bitwarden.expYear` 保存，保留原文与已有自定义字段，不猜测世纪。两项都缺失或仅含空白时视为未设置。

结构化年月转换仅发生在 Bitwarden 适配器；原生备份原样保留有效期字符串，既有条目与归档沿用各自读取规则。
