package com.example.poop.ui.screens.scanner

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultItem
import com.example.poop.util.CryptoManager
import com.example.poop.util.Logcat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerViewModel : ViewModel() {
    private val _scanResult = MutableStateFlow("")
    val scanResult: StateFlow<String> = _scanResult.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _parsedTotp = MutableStateFlow<ParsedTotp?>(null)
    val parsedTotp: StateFlow<ParsedTotp?> = _parsedTotp.asStateFlow()

    private val barcodeScanner = BarcodeScanning.getClient()
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun onPermissionResult(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
    }

    /**
     * 扫码成功后的处理
     */
    fun onBarcodeDetected(context: Context, barcode: String) {
        if (barcode.isNotBlank() && _scanResult.value != barcode) {
            _scanResult.value = barcode
            vibrate(context)
            
            if (barcode.startsWith("otpauth://")) {
                _parsedTotp.value = parseOtpAuthUri(barcode)
            } else {
                _parsedTotp.value = null
            }
        }
    }

    private fun vibrate(context: Context) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    @ExperimentalGetImage
    fun getAnalyzer(context: Context): ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { imageProxy ->
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { onBarcodeDetected(context, it) }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    fun decodeImage(context: Context, uri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let { onBarcodeDetected(context, it) }
                    } else {
                        _scanResult.value = "未发现二维码"
                        _parsedTotp.value = null
                    }
                }
                .addOnFailureListener {
                    _scanResult.value = "识别失败"
                    _parsedTotp.value = null
                }
        } catch (e: Exception) {
            Logcat.e("ScannerViewModel", "Error decoding image", e)
            _scanResult.value = "解析图片出错"
            _parsedTotp.value = null
        }
    }

    private fun parseOtpAuthUri(uriString: String): ParsedTotp? {
        return try {
            val uri = uriString.toUri()
            if (uri.scheme != "otpauth" || uri.host != "totp") return null
            
            val path = uri.path?.trimStart('/') ?: return null
            val label = URLDecoder.decode(path, "UTF-8")
            
            val secret = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer")
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
            val algorithm = uri.getQueryParameter("algorithm") ?: "SHA1"
            
            ParsedTotp(
                label = label,
                secret = secret,
                issuer = issuer,
                digits = digits,
                period = period,
                algorithm = algorithm
            )
        } catch (e: Exception) {
            Logcat.e("ScannerViewModel", "Failed to parse OTP URI", e)
            null
        }
    }

    fun saveToVault(context: Context, onSuccess: () -> Unit) {
        val totp = _parsedTotp.value ?: return
        viewModelScope.launch {
            try {
                val dao = AppDatabase.getDatabase(context).vaultDao()
                
                // 加密密钥
                val encryptedSecret = CryptoManager.getEncryptCipher()?.let { cipher ->
                    CryptoManager.encrypt(totp.secret, cipher)
                } ?: totp.secret

                val newItem = VaultItem(
                    title = totp.issuer ?: totp.label.split(":").firstOrNull() ?: "TOTP",
                    username = totp.label,
                    password = "", // TOTP 项通常不需要主密码，或者在 username 里体现
                    category = "OTP",
                    totpSecret = encryptedSecret,
                    totpDigits = totp.digits,
                    totpPeriod = totp.period,
                    totpAlgorithm = totp.algorithm
                )
                dao.insert(newItem)
                onSuccess()
            } catch (e: Exception) {
                Logcat.e("ScannerViewModel", "Failed to save to vault", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        barcodeScanner.close()
        cameraExecutor.shutdown()
    }
}

data class ParsedTotp(
    val label: String,
    val secret: String,
    val issuer: String? = null,
    val digits: Int = 6,
    val period: Int = 30,
    val algorithm: String = "SHA1"
)
