# Vault 搜索审查待办

**范围：** Vault 列表查询链、搜索归一化及 SQLCipher 查询行为。

## Task 1：明确并覆盖非 ASCII 大小写搜索合同

- [ ] 明确 Vault 搜索是否承诺 Unicode 大小写不敏感；若承诺，选择与 SQLCipher 实际函数行为一致的归一化策略。增加 `EntryPagingDaoTest` 覆盖混合大小写的非 ASCII 搜索文本和存储值，并在当前 SQLCipher `SupportOpenHelperFactory` 路径运行。验收还需确认标题、用户名、URL、标签、域名、应用 ID 六个字段使用同一合同，`%`、`_`、反斜杠仍按字面值搜索。

**调用链证据（代码复核：2026-11-11）：** [`VaultViewModel.entries`](../../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt) 将 `EntryPagingRepository.pages(query, ENTRY_PAGING_CONFIG)` 作为 [`switchRecoverableQueryGenerations()`](../../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultQueryGeneration.kt) 的 `load`。[`buildRecoverableVaultQueryGenerations()`](../../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultQueryGeneration.kt) 组合 `VaultUiState.queryInputs()` 与库设置并直接构造 `EntryListQuery`；有效搜索文本 trim、去重并防抖 250 ms，类型筛选生效时层级模式不进入查询。当前实现不再使用 `VaultQueryState.toEntryListQuery()` 或 `buildRecoverableVaultQueryStates()`。Domain 的 [`EntryListQuery.normalizedSearchText`](../../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntryListQuery.kt) 再调用 Kotlin `lowercase()`。Data 的 [`buildEntryPagingQuery(query)`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/query/EntryPagingQueryFactory.kt) 构造 `pattern = "%${query.normalizedSearchText.escapeLike()}%"`，将同一 pattern 绑定到六个 `LOWER(column) LIKE ? ESCAPE '\'` 条件；`escapeLike()` 依次转义反斜杠、`%`、`_`。参数化查询不把搜索文本拼入 SQL。

**缺口：** `EntryPagingDaoTest.pagingQuery_filtersStructuredSummary_andReturnsUsageAndAccount` 只以 ASCII `example` 验证搜索命中，没有非 ASCII 大小写样例或通配符字面匹配回归。SQLite 内置 `lower()` 默认只转换 ASCII（[SQLite 函数参考](https://www.sqlite.org/lang_corefunc.html)）；Unicode 映射由单独的 [ICU 扩展](https://github.com/sqlcipher/sqlcipher/blob/master/ext/icu/README.txt)提供。SQLCipher 测试数据库使用 `SupportOpenHelperFactory`，当前代码没有注册应用级 Unicode lower 函数；由于实际行为取决于所用 SQLCipher 构建，需先通过目标数据库测试确认，再决定修正文档合同或实现方案。本待办只记录问题，不修改运行时代码。

**相关主题：** [Vault 列表、搜索与排序](../../../product/vault/list-and-search.md)。
