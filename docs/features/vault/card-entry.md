# 银行卡资料与有效期

当前实现：`AddBankCardViewModel` 将表单转换为草稿，经 `CreateEntryUseCase` 校验后写入 Repository。
标题与卡号必填，有效期可不设置。卡号规则由 Domain 的 `CardNumberValidator` 提供。

表单与最终 `EntryValidation` 共用卡号规则：仅接受 ASCII 数字，允许空白和连字符分组；数字长度为 13–19，
并校验 Luhn。其他字符不得先删除再校验；缺失数字返回必填错误，长度错误与字符/校验和错误分别映射到
`INVALID_LENGTH`、`INVALID_FORMAT`。有效但未识别卡组织的号码仍可保存，原始分组格式保持不变。
这些规则用于新建条目的格式校验，不会重写既有条目或改变原生归档恢复策略。

## 可选银行资料

创建页的“银行资料”包含以下可选字段，保存前去除首尾空白；币种允许多币种文本，如 `CNY / USD`。
标题用于用户自己的命名，卡片产品名称用于记录银行产品资料。

| 字段 | Domain 属性 | 原生归档稳定键 |
| --- | --- | --- |
| 发卡行 | `issuingBank` | `card_issuing_bank` |
| 开户支行 | `bankBranch` | `card_bank_branch` |
| 卡片产品名称 | `cardProduct` | `card_product` |
| 币种 | `currency` | `card_currency` |
| 银行客服电话 | `bankPhone` | `card_bank_phone` |

这些资料与持卡人、账单地址一起保存在 `CardCredential` 的加密 Secret bundle 中；`EntrySecretMapper`
负责持久化 Payload 往返，原生 JSON、加密备份与 TXT 导出保留对应字段。Room、内部 Payload 和归档版本保持 1。
Bitwarden 的自定义资料继续作为自定义字段导入，不从标题、卡号或品牌推测发卡行。

新建银行卡时，Domain 的 `EntryDraftMaterializer` 只将持卡人写入 `card.cardHolder`，不再复制到明文
`profile.username`。标题、持卡人和用户名是不同字段；详情修改持卡人继续使用同一凭据属性。
此规则不改写已有开发数据，也不清空原生归档中显式提供的 Profile 数据。

详情页展示上述字段和账单地址；长按对应行可编辑，每次只编辑一个逻辑字段。清空可选资料会移除该值。
纯字段更新由 Domain `EntryFieldEditor` 根据条目类型与字段定义处理，App 用例负责会话、互斥、版本冲突及写入。
修改持卡人只更新 `card.cardHolder`，不会修改或回退读取 `profile.username`；跨凭据类型的字段修改返回校验失败。
未揭示的卡号、CVV 等高敏值仍按 Data 的 Preserve/Clear/Replace 契约处理。

## 有效期与导入

有效期年月由 Domain 的 `CardExpiry` 表达：月份为 1–12，年份为 1–9999，创建时统一写为
`MM/YYYY`（例如 `03/2030`）。`EntryValidation` 使用同一解析规则，拒绝非法月份、零年份和两位年份，
不推测世纪。未选择有效期由 `CardExpirySelection.NotSet` 表达，草稿中省略该字段。

页面年月选择器提供当年至未来 20 年，这是输入界面的选择范围；Domain 不以当前时间拒绝已过期银行卡，
也不将格式校验视为支付可用性验证。UI 仅持有选中状态，年月验证与格式化不重复实现。

Bitwarden 的分离年月字段在适配边界允许一位或两位月份，完整合法值经 `CardExpiry` 校验并统一为
`MM/YYYY`。缺失部分、非法月份或两位年份不生成有效期；原始值以隐藏自定义字段 `Bitwarden.expMonth`、
`Bitwarden.expYear` 保存，保留原文与已有自定义字段，不猜测世纪。两项都缺失或仅含空白时视为未设置。

结构化年月转换仅发生在 Bitwarden 适配器；原生备份原样保留有效期字符串，既有条目与归档沿用各自读取规则。
