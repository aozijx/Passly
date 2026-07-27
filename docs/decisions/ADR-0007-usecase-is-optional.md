# ADR-0007: UseCase 层按需使用

- 状态：Accepted
- 日期：未记录

## 背景

为每个简单 Repository 方法建立一一对应 UseCase 会产生大量无业务语义的转发文件。

## 决策

UseCase 不是强制层。跨仓库流程、事务、安全验证或稳定业务不变量使用 UseCase；简单查询和设置可由 ViewModel
依赖 Domain Repository。

## 后果

减少样板文件，同时要求 Review 识别复杂逻辑是否错误地流入 ViewModel。

## 备选方案

未采用“所有调用都必须经过 UseCase”的机械规则。
