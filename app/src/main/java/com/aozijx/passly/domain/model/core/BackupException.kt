package com.aozijx.passly.domain.model.core

sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class PasswordIncorrect : BackupException("备份密码错误，请核对后重试")
    class FileCorrupted : BackupException("备份文件损坏或格式不正确")
    class StoragePermissionDenied : BackupException("没有文件写入权限，请重新授权")
    class Unknown(cause: Throwable?) : BackupException("备份操作失败：未知错误", cause)
}