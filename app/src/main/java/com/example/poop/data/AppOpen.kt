package com.example.poop.data

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings
import com.example.poop.BuildConfig

/**
 * 打开应用/功能的配置类
 * @param app 应用/功能名称（如“微信”）
 * @param packageName 目标应用包名（如微信com.tencent.mm，本机功能可null）
 * @param requiredPermission 所需权限（如相机Manifest.permission.CAMERA，无需权限则null）
 * @param intentBuilder 自定义Intent构建逻辑（用于本机功能如相机/相册，优先级高于packageName）
 * @param errorTip 未安装/无功能时的提示文本（默认值）
 */
data class AppOpenConfig(
    val app: String,
    val packageName: String? = null,
    val requiredPermission: String? = null,
    val intentBuilder: ((Context) -> Intent?)? = null,
    val errorTip: String
)

object AppConfigs {
    val appList = listOf(
        AppOpenConfig(
            app = "微信",
            packageName = BuildConfig.WECHAT_PACKAGE,
            errorTip = "未安装微信"
        ),
        AppOpenConfig(
            app = "QQ",
            packageName = BuildConfig.QQ_PACKAGE,
            errorTip = "未安装QQ"
        ),
        AppOpenConfig(
            app = "抖音",
            packageName = BuildConfig.DOUYIN_PACKAGE,
            errorTip = "未安装抖音"
        ),
        AppOpenConfig(
            app = "相机",
            intentBuilder = {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            },
            errorTip = "无法打开相机"
        ),
        AppOpenConfig(
            app = "相册",
            intentBuilder = {
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            },
            errorTip = "无法打开相册"
        ),
        AppOpenConfig(
            app = "精选相册",
            intentBuilder = {
                Intent().apply {
                    action = Intent.ACTION_GET_CONTENT
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            },
            errorTip = "无法打开相册"
        ),
        AppOpenConfig(
            app = "设置",
            intentBuilder = { Intent(Settings.ACTION_SETTINGS) },
            errorTip = "无法打开设置"
        )
        // 可以继续添加更多应用...
    )
}