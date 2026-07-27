package com.aozijx.passly.app.di.database

/**
 * DataSource 绑定。
 *
 * 后续会并入：
 * - MetadataLocalDataSource
 * - CredentialLocalDataSource
 * - LookupLocalDataSource
 * - HistoryLocalDataSource
 * - AttachmentLocalDataSource
 *
 * Repository 层通过 DataSource 访问数据，不再直接依赖 DAO。
 */
// @Module
// @InstallIn(SingletonComponent::class)
// abstract class DataSourceModule {
// }
