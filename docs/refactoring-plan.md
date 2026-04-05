# Passly 项目重构执行手册

本项目正在进行从单体大文件向领域驱动、职责分离架构的迁移。本文档用于追踪各阶段进度与技术规范。

## 1. 当前问题诊断

| # | 问题文件                       | 症状                                            | 状态  |
|---|----------------------------|-----------------------------------------------|-----|
| 1 | AppDatabase.kt             | Room 定义 + 加密口令管理 + 迁移 + DatabaseConfig，全在一个文件 | 已解决 |
| 2 | VaultDao.kt                | 20+ 方法混杂 CRUD / 搜索 / 摘要 / 自动填充 / 历史           | 进行中 |
| 3 | SpecificEntryStrategies.kt | 9 种条目策略全挤在 400+ 行的单文件                         | 已解决 |
| 4 | VaultUseCasesImpl.kt       | 15 个 UseCase 类堆在一个文件                          | 已解决 |
| 5 | BackupManager.kt           | 导出 / 导入 / 加密 / JSON 序列化全在一起                   | 已解决 |
| 6 | core/common/ui/            | UI Token 与非 UI 通用类型混放                         | 进行中 |
| 7 | EntryTypeResolver.kt       | FieldKey 枚举嵌在 Resolver 逻辑里                    | 进行中 |

---

## 2. 目标结构

```
com.aozijx.passly/
├── core/
│   ├── security/
│   │   └── DatabasePassphraseManager.kt   (已完成：从 AppDatabase 提取)
│   ├── backup/
│   │   ├── BackupManager.kt              (已完成：V1-Implemented 架构)
│   │   ├── BackupVSerializer.kt          (已完成：支持 ZIP 与 JSON 分离)
│   │   └── BackupExportStorageSupport.kt  (已完成：包含延迟创建与清理逻辑)
│   └── designsystem/
│       ├── model/
│       │   ├── VaultCardStyle.kt         (待迁移：从 core/common/ui 移入)
│       │   └── FieldLabel.kt             (待提取：从 Resolver 拆分)
│       └── utils/
│           └── EntryTypeResolver.kt       (进行中：仅保留逻辑)
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt                (已完成：仅保留定义)
│   │   ├── DatabaseConfig.kt             (已完成：常量分离)
│   │   ├── dao/
│   │   │   ├── VaultEntryDao.kt          (待拆分)
│   │   │   └── VaultHistoryDao.kt        (待拆分)
│   │   └── migration/
│   │       └── Migrations.kt             (已完成：迁移链收敛)
└── domain/
    ├── strategy/
    │   └── impl/ (9个独立策略文件)        (已完成)
    └── usecase/
        └── vault/
            └── impl/ (15个独立用例文件)   (已完成)
```

---

## 3. 分阶段验收清单

### Phase 1 — 实现分离 (核心逻辑解耦)

- [x] 提取 DatabaseConfig：常量已移至 data/local/DatabaseConfig.kt。
- [x] 拆分策略类：SpecificEntryStrategies.kt 已按类型拆分为 9 个独立文件。
- [x] 拆分用例类：VaultUseCasesImpl.kt 已拆分为 15 个独立文件并放入 impl 包。
- [x] 门面类更新：VaultUseCases.kt 已改为引用拆分后的具体实现。
- [x] 物理清理：已删除 SpecificEntryStrategies.kt 和 VaultUseCasesImpl.kt。

### Phase 2 — 数据层重构 (数据库与安全收敛)

- [x] 提取口令管理：DatabasePassphraseManager 已接管 KeyStore 加密逻辑。
- [x] 提取迁移逻辑：Migrations.kt 已独立，支持 MIGRATION_1_2, 2_3。
- [x] 瘦身 AppDatabase：单例构建逻辑清晰，不再包含业务常量。
- [ ] 拆分 VaultDao：将条目 CRUD 与历史记录操作彻底分离。
- [x] 认证链路收敛：详情页与列表滑动动作已统一走 ViewModel 认证加解密流程。

### Phase 3 — 备份系统重构 (安全与健壮性升级)

- [x] 算法升级：采用 Argon2id (iterations=3, memory=64MB) + AES-256-GCM。
- [x] 容器升级：采用 ZIP 格式，支持 data.json 与 images/ 目录打包。
- [x] 幻数校验：文件头增加 PASSLY 标识，准确识别非备份文件。
- [x] 异常映射：能够准确区分 密码错误 与 文件损坏。
- [x] 导出控制：支持 包含媒体文件 开关，全量备份自动标记 _full 后缀。
- [x] 事务性清理：导出失败或密码验证不通过时，自动清理 0 字节残留文件。

### Phase 4 — 资源磁盘化与 UI 整理 (进行中)

- [x] 图片备份适配：BackupManager 已支持从磁盘路径读取图片并打包。
- [ ] 移除 DB BLOB：确保新条目保存图标时仅存相对路径，不再写入数据库 BLOB 列。
- [ ] UI 模型归类：将 core/common/ui 下的 VaultCardStyle 等移入 designsystem/model。
- [ ] 枚举解耦：将 FieldKey 和 FieldLabel 从 EntryTypeResolver 中提取到领域模型。
- [ ] DI 模块化：将 AppContainer 逻辑拆分为 DataModule 和 DomainModule。

---

## 4. 技术规范准则

1. 敏感字段：禁止在列表 (Summary) 模型中存储明文密码。
2. 解密路径：严禁绕过认证链路调用静默解密 (decryptSilently)。
3. 文件操作：涉及外部存储的 Uri 读写必须通过 StorageSupport 处理，并具备清理机制。
4. 命名规范：UseCase 统一使用 动词+名词+UseCase 后缀；实现类放入 impl 子包。
