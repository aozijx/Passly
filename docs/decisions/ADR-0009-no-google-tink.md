# ADR-0009: 暂不引入 Google Tink

- 状态：Accepted
- 日期：未记录

## 背景

项目当前密码学需求集中在 AES-GCM、HMAC、Argon2id 和 Android Keystore，已有明确格式与生命周期。

## 决策

暂不引入 Google Tink，继续使用平台 JCA 与受控封装。任何密码学调用仍必须集中在 Security 边界并有格式测试。

## 后果

减少依赖与格式迁移，但团队需自行维护 nonce、AAD、错误映射和 key lifecycle 的正确性。

## 备选方案

若未来需要跨平台 keyset、轮换和成熟格式支持，应以新 ADR 重新评估 Tink。
