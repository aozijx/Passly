# ADR-0014: 使用 Blind Index 检索

- 状态：Accepted
- 日期：未记录

## 背景

敏感字段使用 AES-GCM 后不能直接 `LIKE`/FTS 搜索；保存明文搜索列会泄露内容。

## 决策

对规范化 token 使用会话相关搜索密钥生成不可逆 Blind Index，存入独立 `lookup_index`。查询端以同一规范化和
key 生成候选索引。

## 后果

支持等值/受控 token 检索并减少明文泄露，但会暴露相等关系和频率，且 key/规范化变化需要重建索引。

## 备选方案

未采用明文 `LIKE`、解密全表扫描或对密文使用 FTS。
