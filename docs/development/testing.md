# 测试与质量门禁

## 提交前门禁

按以下顺序执行，便于优先得到低成本反馈：

1. `:app:compileDebugKotlin`
2. `:app:testDebugUnitTest`
3. `:app:lintDebug`
4. `:app:assembleDebug`
5. `:app:assembleDebugAndroidTest`
6. 有设备时执行 `:app:connectedDebugAndroidTest`
7. `git diff --check`

单元测试任务必须实际执行测试，不能以 `NO-SOURCE` 作为通过。Lint 不建立
baseline；只允许对冻结工具链产生且有明确理由的版本提示做局部禁用。

## 核心覆盖面

- Proto 默认值、损坏数据和编解码
- Entity/Domain Mapper、分页和候选解析
- 锁定、认证取消、恢复码信封和敏感状态清理
- 备份 keyed golden、注册表完整性、每种 archive EntryType 往返、错误密码、截断、未知格式/版本、附件与路径穿越
- 同一备份 fixture 的 compatible/strict 行为；未知字段/类型可隔离，但资源哈希、清单和容器损坏在两种策略下都拒绝
- 覆盖导入过滤为空时保留原数据库，导入摘要计数覆盖写入、已存在、跳过字段/条目及裁剪关系/资源
- Room 全新建库、加密打开、锁定关闭、再次解锁、错误密钥不删库

## 仪器测试说明

连接设备的测试属于环境相关门禁。若失败，需区分应用断言、APK 安装/资源打包和设备状态，不能仅以“本地没有设备”关闭问题。

