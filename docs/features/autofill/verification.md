# Autofill 验证矩阵

状态：当前与已批准目标的验收合同。

## 当前流程

- Legacy：锁定发现、认证、同 interaction 选择、结果先交付后关闭；SaveInfo 角色和当前认证式 save。
- Credential Manager：locked begin-get、同 interaction 重发现、caller/allowed-user 拒绝、get/create result 合同、
  clear-state 和 PendingIntent identity。
- 两套入口不导入对方 Android 类型；共享层只处理平台无关合同。
- 进程死亡拒绝旧 interaction/token；超时、返回、取消和异常释放自己的 lease。
- 已有完整 App session 不被迟到 Autofill 完成锁定；临时 session 完成后不可再次显示上一轮秘密。
- result delivery 失败不伪造 delivered，也不提前关闭可重试会话。
- `setResult` 后、`finish` 前不得关闭临时会话；关闭期间的新请求等待完成后创建新的 locked interaction。
- 成功交付会过期未提交结果的遗留 interaction；并发且都已提交的 Legacy/Credential 结果分别记为 completed。

## Inbox 实现验收

- 已解锁 save 直接写 Entry，不创建 pending、不重复认证。
- 锁定 Legacy/CM create 密封 pending，不弹认证；fill/read 仍认证。
- 未 provision、容量满、超限、损坏 public metadata 明确失败，不虚报 Queued。
- private/public/fingerprint mismatch 停止导入并 quarantine，不写错误 Entry。
- header/AAD/ciphertext/tag/draft ID 任意篡改都失败；日志不含 payload、包名或异常 message。
- 数据库提交后、文件删除前崩溃由 import-log 唯一键阻止重复。
- 过期、旧 key-ID、retry/quarantine、全 root 512 KiB 上限按合同执行。
- Android Auto Backup/device transfer 和 Passly Backup 均不包含 Inbox。
- Vault 删除清理 key material、文件和 import log；真实 DEK rotation 原子重包 private envelope。
