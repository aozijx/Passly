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
import com.example.poop.ui.screens.profile.ImageType

/**
 * 封装的照片选择工具
 */
@Composable
fun rememberImagePicker(
    onImagePicked: (Uri, ImageType) -> Unit
): (ImageType) -> Unit {
    val pendingImageType = rememberSaveable { mutableStateOf<ImageType?>(null) }

    fun handleUriResult(uri: Uri?) {
        val type = pendingImageType.value ?: return
        if (uri != null) {
            // 现代选择器返回的 URI 在单次会话中已有读取权限
            onImagePicked(uri, type)
        }
        pendingImageType.value = null
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> handleUriResult(uri) }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleUriResult(result.data?.data)
        } else {
            pendingImageType.value = null
        }
    }

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
            }
            galleryLauncher.launch(intent)
        }
    }
}
