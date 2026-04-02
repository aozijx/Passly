package com.aozijx.passly.ui.screens.profile

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import com.aozijx.passly.core.util.ImageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class ProfileMenuItemData(val icon: ImageVector, val title: String, val id: String)

data class UserProfile(
    val name: String,
    val bio: String,
    val avatarUrl: String? = null,
    val menuItems: List<ProfileMenuItemData> = emptyList()
)

data class ProfileUiState(
    val user: UserProfile = UserProfile(name = "", bio = ""),
    val avatarUri: Uri? = null,
    val coverUri: Uri? = null,
    val screenUri: Uri? = null,
    val exifInfo: Map<String, String>? = null,
    val isLoading: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _uiState.update {
            it.copy(
                user = UserProfile(
                    name = "Android 开发者",
                    bio = "热爱编程，喜欢探索新技术。这是我的个人简介。"
                )
            )
        }
    }

    fun updateSelectedImage(uri: Uri?, type: ImageType) {
        _uiState.update { currentState ->
            // 仅在 COVER 类型且 URI 不为空时解析 Exif
            val newExif = if (type == ImageType.COVER && uri != null) parseExif(uri) else null

            when (type) {
                ImageType.AVATAR -> currentState.copy(avatarUri = uri, exifInfo = null)
                ImageType.COVER -> currentState.copy(coverUri = uri, exifInfo = newExif)
                ImageType.SCREEN -> currentState.copy(screenUri = uri, exifInfo = null)
            }
        }
    }

    private fun parseExif(uri: Uri): Map<String, String> {
        val context = getApplication<Application>()
        return try {
            // Photo Picker 的 URI (content://media/picker/...) 不支持 setRequireOriginal
            val isPickerUri = uri.toString().contains("com.android.providers.media.photopicker") ||
                    uri.authority?.contains("photopicker") == true

            val finalUri = if (!isPickerUri) {
                runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
            } else {
                uri
            }

            context.contentResolver.openInputStream(finalUri)?.use { stream ->
                val exif = ExifInterface(stream)
                buildMap {
                    // 1. 设备型号
                    val make = exif.getAttribute(ExifInterface.TAG_MAKE).orEmpty().trim()
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL).orEmpty().trim()
                    val device = if (model.contains(make, true)) model else "$make $model".trim()
                    if (device.isNotEmpty()) put("设备型号", device)

                    // 2. 拍摄参数
                    exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { put("光圈", "f/$it") }

                    val expTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                    if (expTime > 0) {
                        put(
                            "曝光时间",
                            if (expTime >= 1.0) "%.1fs".format(
                                Locale.US,
                                expTime
                            ) else "1/${(1.0 / expTime).toInt()}s"
                        )
                    }

                    exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                        ?.let { put("ISO", it) }

                    // 3. 焦距
                    val focal35mm =
                        exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
                    if (focal35mm > 0) {
                        put("焦距", "${focal35mm}mm (等效35mm)")
                    } else {
                        val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                        if (focal > 0) put("焦距", "%.1fmm".format(Locale.US, focal))
                    }

                    // 4. 地理位置与时间
                    exif.latLong?.let { (lat, lng) ->
                        put(
                            "地理位置",
                            "%.4f, %.4f".format(Locale.US, lat, lng)
                        )
                    }
                    (exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(
                        ExifInterface.TAG_DATETIME
                    ))
                        ?.let { put("拍摄时间", it) }
                }
            } ?: mapOf("提示" to "无法打开图片流")
        } catch (e: Exception) {
            mapOf("错误" to (e.localizedMessage ?: "解析失败"))
        }.ifEmpty { mapOf("提示" to "该图片无 Exif 记录") }
    }
}