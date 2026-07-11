# Passly 架构决策记录 (Architecture Decision Records)

## 1. 什么是 ADR？

架构决策记录 (ADR) 是一个简短的文本文件，用于记录项目在特定时间点所做的重要架构决策。它捕捉了决策背后的背景、可选方案以及最终选择的理由，为项目的长期演进提供了可追溯的依据。

---

## 2. 文档规范

- **状态管理**：每个 ADR 应包含明确的状态（如 Accepted, Superseded, Deprecated）。
- **不可变性**：编号一经发布不再修改。若后续决策发生重大变化，应新增 ADR 并标注对旧决策的替代关系。
- **职责边界**：ADR 聚焦于“为什么”而非“如何实现”，避免包含过细的代码实现细节。

---

## 3. 核心决策索引

| 编号           | 核心标题                           | 关联文件                                                                                             |
|:-------------|:-------------------------------|:-------------------------------------------------------------------------------------------------|
| **ADR 0001** | 项目核心原则与设计哲学                    | [ADR-0001-project-principles.md](./ADR-0001-project-principles.md)                               |
| **ADR 0002** | 采用信封加密机制 (Envelope Encryption) | [ADR-0002-envelope-encryption.md](./ADR-0002-envelope-encryption.md)                             |
| **ADR 0003** | 采用双 DEK (双重数据加密密钥) 架构          | [ADR-0003-dual-dek.md](./ADR-0003-dual-dek.md)                                                   |
| **ADR 0004** | Repository 作为唯一解密边界            | [ADR-0004-repository-is-decryption-boundary.md](./ADR-0004-repository-is-decryption-boundary.md) |
| **ADR 0005** | Autofill Pipeline 分层边界定义       | [ADR-0005-autofill-layer-boundaries.md](./ADR-0005-autofill-layer-boundaries.md)                 |
| **ADR 0006** | 使用 ResolvedCandidate 隔离领域模型    | [ADR-0006-resolvedcandidate-dto.md](./ADR-0006-resolvedcandidate-dto.md)                         |
| **ADR 0007** | 将 UseCase 定义为可选架构层             | [ADR-0007-usecase-is-optional.md](./ADR-0007-usecase-is-optional.md)                             |
| **ADR 0008** | 运行时威胁边界定义 (Runtime Dump)       | [ADR-0008-runtime-threat-boundary.md](./ADR-0008-runtime-threat-boundary.md)                     |
| **ADR 0009** | 暂不引入 Google Tink 加密库           | [ADR-0009-no-google-tink.md](./ADR-0009-no-google-tink.md)                                       |
| **ADR 0010** | Repository 返回明文领域模型契约          | [ADR-0010-repository-returns-plaintext.md](./ADR-0010-repository-returns-plaintext.md)           |
| **ADR 0011** | 威胁模型驱动架构设计原则                   | [ADR-0011-threat-model-drives-design.md](./ADR-0011-threat-model-drives-design.md)               |
| **ADR 0012** | 坚持使用符合 NIST 规范的随机 Nonce        | [ADR-0012-random-nonce.md](./ADR-0012-random-nonce.md)                                           |

---

## 4. 总结

Passly 的架构设计始终以 **安全 (Security)** 与 **健壮 (Robustness)**
为最高优先级。通过这些架构决策记录，我们确保了每一项底层技术的引入都有明确的风险场景支撑，为用户构建了一个透明且可靠的安全底座。