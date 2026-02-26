package com.example.poop.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.poop.ui.screens.profile.ImageType

/**
 * 封装的照片选择工具
 * @param onImagePicked 图片选中后的回调，返回 Uri 和对应的业务类型 (ImageType)
 */
@Composable
fun rememberImagePicker(
    onImagePicked: (Uri, ImageType) -> Unit
): (ImageType) -> Unit {
    val context = LocalContext.current

    // 使用 rememberSaveable 确保配置更改（如旋转屏幕）后 pendingImageType 不会丢失
    val pendingImageType = rememberSaveable { mutableStateOf<ImageType?>(null) }

    // 内部统一结果处理函数
    fun handleUriResult(uri: Uri?) {
        val type = pendingImageType.value ?: return
        if (uri != null) {
            try {
                // 尝试持久化权限（SAF 模式下有效，Android 10+ 建议）
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 某些选择器返回的 URI 可能不支持持久化，忽略即可
                e.printStackTrace()
            }
            onImagePicked(uri, type)
        }
        pendingImageType.value = null
    }

    // Android 13+ 现代照片选择器
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> handleUriResult(uri) }
    )

    // Android 12 及以下传统相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleUriResult(result.data?.data)
        } else {
            pendingImageType.value = null
        }
    }

    // 返回一个可调用的闭包
    return { imageType: ImageType ->
        pendingImageType.value = imageType

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            galleryLauncher.launch(intent)
        }
    }
}