package com.aozijx.passly.domain.model.attachment

import com.aozijx.passly.domain.model.attachment.AttachmentStatus.COMMITTED
import com.aozijx.passly.domain.model.attachment.AttachmentStatus.PENDING


/**
 * 附件状态。
 *
 * - [PENDING]：文件已选择/上传，但尚未与条目正式关联（待提交）。
 * - [COMMITTED]：文件已完全关联到条目，DB 记录与文件系统一致。
 *
 * 文件删除操作必须在 DB 记录成功删除之后执行，
 * 避免文件已删但 DB 仍持有引用（孤儿记录）或 DB 已删但文件残留。
 */
enum class AttachmentStatus {
    PENDING,
    COMMITTED
}
