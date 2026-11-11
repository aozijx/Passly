# Data 层文档总览

状态：当前架构规范。

`:data` 是 Domain 数据端口的持久化实现 owner，隔离 Room、Proto DataStore、文件资源、存储映射和数据库事务。它不拥有页面状态、跨页面应用流程、认证/授权决策或归档协议。

| 主题 | 权威文档 |
| --- | --- |
| Room、SQLCipher、schema、迁移、数据库 session/lease 和事务 | [数据库与 Session](database.md) |
| Entity、Proto/payload 与 Domain model 的映射、敏感字段读写边界 | [Data 映射边界](mapping.md) |
| Proto DataStore 设置端口、默认值及序列化职责 | [设置存储](settings-storage.md) |
| Markdown/HTML 等内容解析与渲染数据边界 | [内容渲染](content-rendering.md) |

## 与其他层协作

- ViewModel/App/Feature 依赖 Domain port，不导入 DAO、Entity、Data Repository 或 DataStore 实现。
- Repository 对上层提供挂起命令、一次性结果或观察用 Flow。单一数据源可以由 Repository 直接使用；只有确有多源协调、共用访问策略或独立测试边界时才抽 DataSource。
- Domain 决定业务不变量与对调用方公开的模型/端口；Data Mapper 转换持久模型，Data Repository 编排存储访问、事务和持久化错误。
- Vault Room 操作必须通过 `AppDatabaseSession` 的授权与 Runtime lease 合同；Security 与 Data 通过 Domain/Core/Runtime 稳定接口协作，不形成实现模块互相依赖。
- Room/DataStore 是各自数据的持久事实源；UI state 只能是展示投影或明确 owner 的短期编辑草稿。Flow 查询失败与授权撤销不能伪装成空列表或记录不存在。

Android 官方 Data layer 建议与 Passly 安全、错误、取消和 durable commit 合同见[层间契约与通信](../architecture/layer-interactions.md)及[Data 映射边界](mapping.md#android-官方数据层建议与-passly-约定)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 持有 Room 访问、Session 授权与 lease 边界 | `AppDatabaseSession` | [`AppDatabaseSession.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt) |
| 绑定 Entry 持久化 Repository 与 Mapper 实现 | `EntryPersistenceModule` | [`EntryPersistenceModule.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/EntryPersistenceModule.kt) |
| 映射并持久化 Proto 设置 | `ProtoAppPreferencesStore` | [`ProtoAppPreferencesStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/settings/ProtoAppPreferencesStore.kt) |
