# ADR-0012: AES-GCM 使用随机 Nonce

- 状态：Accepted
- 日期：未记录

## 背景

AES-GCM 在同一密钥下复用 nonce 会严重破坏机密性与完整性。

## 决策

每次加密使用安全随机生成的 12-byte nonce，与 ciphertext 一起存储。同一 key/nonce 组合不得复用。

## 后果

格式需要保存 nonce，并在测试中覆盖长度、随机性和截断处理；无需维护易出错的全局计数器。

## 备选方案

未采用由 record id 确定性派生 nonce，因为版本、重写和并发更容易造成复用。
