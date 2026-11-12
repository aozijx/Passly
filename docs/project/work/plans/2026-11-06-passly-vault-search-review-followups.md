# Vault 搜索审查待办

**范围：** Vault 列表查询链、搜索归一化及 SQLCipher 查询行为。

## Task 1：明确并覆盖非 ASCII 大小写搜索合同

- [ ] 明确 Vault 搜索是否承诺 Unicode 大小写不敏感；若承诺，选择与目标 SQLCipher 行为一致的策略，并在 `SupportOpenHelperFactory` 路径验证混合大小写的非 ASCII 搜索。保留六个字段统一匹配及 `%`、`_`、反斜杠按字面值搜索的回归。

**缺口：** [`EntryPagingDaoTest.pagingQuery_searchesEveryStructuredEntryField()`](../../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/EntryPagingDaoTest.kt) 已覆盖六个字段的 ASCII 大小写查询；[`pagingQuery_treatsLikeMetacharactersAsLiteralSearchText()`](../../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/EntryPagingDaoTest.kt) 已覆盖 `%`、`_` 和反斜杠的字面匹配。尚无非 ASCII 混合大小写样例，目标 SQLCipher 行为也未验证。SQLite 内置 `lower()` 默认只转换 ASCII（[SQLite 函数参考](https://www.sqlite.org/lang_corefunc.html)）；Unicode 映射由单独的 [ICU 扩展](https://github.com/sqlcipher/sqlcipher/blob/master/ext/icu/README.txt)提供。当前代码没有注册应用级 Unicode lower 函数，因此需先在目标 SQLCipher 构建上验证，再决定是否承诺 Unicode 大小写不敏感。

**相关主题：** [Vault 列表、搜索与排序](../../../product/vault/list-and-search.md)。
