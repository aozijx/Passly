package com.aozijx.passly.core.di

/**
 * 全局依赖容器入口：作为模块化依赖的聚合入口
 */
object AppContainer {

    // --- 聚合领域层模块 ---
    internal val domain by lazy { DomainModule() }
}