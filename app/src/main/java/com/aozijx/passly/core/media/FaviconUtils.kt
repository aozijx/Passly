package com.aozijx.passly.core.media

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.aozijx.passly.core.logging.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FaviconUtils {

    private const val TAG = "FaviconUtils"
    private const val MAX_HTML_BYTES = 512 * 1024L // HTML 解析上限 512 KB

    enum class DownloadResult {
        SUCCESS,
        NETWORK_ERROR,
        DECODE_ERROR,
        SAVE_ERROR,
        EMPTY_INPUT
    }

    data class DownloadOutcome(
        val result: DownloadResult,
        val filePath: String? = null
    )

    /**
     * 从 HTML 中解析 link 标签获取图标 URL
     */
    suspend fun fetchFaviconUrlFromHtml(domain: String): String? = withContext(Dispatchers.IO) {
        try {
            val clean = cleanDomain(domain)
            if (clean.isBlank()) return@withContext null
            if (isRestrictedHost(clean)) {
                Logcat.w(TAG, "Reject favicon fetch for restricted host: $clean")
                return@withContext null
            }

            val url = "https://$clean"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            // 关闭自动跳转：防止目标站点把我们引向内网或非 HTTP(S) 资源。
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")

            val html = connection.inputStream.use { stream ->
                val buffer = StringBuilder()
                val reader = stream.bufferedReader()
                val chunk = CharArray(8 * 1024)
                var totalChars = 0L
                while (true) {
                    val read = reader.read(chunk)
                    if (read == -1) break
                    totalChars += read
                    buffer.append(chunk, 0, read)
                    if (totalChars >= MAX_HTML_BYTES) break
                }
                buffer.toString()
            }

            // 匹配 rel 为 icon, shortcut icon 或 apple-touch-icon 的 link 标签
            val patterns = listOf(
                """<link[^>]*rel=["'](?:shortcut )?icon["'][^>]*href=["']([^"']+)["']""",
                """<link[^>]*href=["']([^"']+)["'][^>]*rel=["'](?:shortcut )?icon["']""",
                """<link[^>]*rel=["']apple-touch-icon["'][^>]*href=["']([^"']+)["']"""
            )

            for (pattern in patterns) {
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                val match = regex.find(html)
                if (match != null) {
                    val href = match.groupValues[1]
                    return@withContext resolveUrl(url, href)?.takeIf(::isAllowedRemoteUrl)
                }
            }
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to parse HTML for favicon: $domain", e)
        }
        null
    }

    private fun resolveUrl(baseUrl: String, href: String): String? {
        // 仅允许 http(s) 与协议相对链接，过滤 javascript:/data:/file: 等危险 scheme。
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        if (href.contains(':') && !href.startsWith("/")) return null

        val uri = java.net.URI(baseUrl)
        if (href.startsWith("/")) {
            return "${uri.scheme}://${uri.host}$href"
        }

        val path = uri.path
        val base = if (path.isEmpty() || path == "/") {
            "${uri.scheme}://${uri.host}/"
        } else {
            val lastSlash = path.lastIndexOf('/')
            "${uri.scheme}://${uri.host}${path.substring(0, lastSlash + 1)}"
        }
        return base + href
    }

    private fun isAllowedRemoteUrl(url: String): Boolean {
        return try {
            val parsed = java.net.URI(url)
            val scheme = parsed.scheme?.lowercase()
            val host = parsed.host
            (scheme == "http" || scheme == "https") && !host.isNullOrBlank() && !isRestrictedHost(host)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 阻止本地回环、私有 / 链路本地 IP 与无效主机名，避免 SSRF 把内部资源拽出来。
     */
    private fun isRestrictedHost(host: String): Boolean {
        val h = host.trim().lowercase().removeSurrounding("[", "]")
        if (h.isBlank() || h == "localhost") return true
        if (h.endsWith(".local") || h.endsWith(".internal") || h.endsWith(".lan")) return true
        // IPv6（含 zone id）一律拒绝：能命中合法外网的场景极少，攻击面更大
        if (h.contains(':')) return true
        // IPv4 字面量
        val ipv4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""").matchEntire(h)
        if (ipv4 != null) {
            val parts = ipv4.groupValues.drop(1).map { it.toIntOrNull() ?: -1 }
            if (parts.any { it < 0 || it > 255 }) return true
            val a = parts[0]; val b = parts[1]
            if (a == 0 || a == 10 || a == 127) return true
            if (a == 169 && b == 254) return true
            if (a == 172 && b in 16..31) return true
            if (a == 192 && b == 168) return true
            if (a >= 224) return true // 组播 / 保留段
        }
        return false
    }

    suspend fun downloadAndSaveFavicon(input: String, context: Context): DownloadOutcome = withContext(Dispatchers.IO) {
        if (input.isBlank()) return@withContext DownloadOutcome(DownloadResult.EMPTY_INPUT)

        Logcat.d(TAG, "Trying to download favicon from: $input")

        val isDirectUrl = input.startsWith("http://") || input.startsWith("https://")

        // 如果是直接 URL，直接下载
        if (isDirectUrl) {
            if (!isAllowedRemoteUrl(input)) {
                Logcat.w(TAG, "Reject favicon download for restricted url")
                return@withContext DownloadOutcome(DownloadResult.NETWORK_ERROR)
            }
            val bitmap = downloadFaviconWithCoil(input, context)
            if (bitmap != null) {
                val savedPath = saveBitmapToInternalStorage(context, bitmap)
                return@withContext if (savedPath != null) {
                    DownloadOutcome(DownloadResult.SUCCESS, savedPath)
                } else {
                    DownloadOutcome(DownloadResult.SAVE_ERROR)
                }
            }
            return@withContext DownloadOutcome(DownloadResult.NETWORK_ERROR)
        }

        val clean = cleanDomain(input)
        if (clean.isBlank() || isRestrictedHost(clean)) {
            Logcat.w(TAG, "Reject favicon download for restricted domain")
            return@withContext DownloadOutcome(DownloadResult.NETWORK_ERROR)
        }

        // 1. 尝试从 HTML 解析
        val htmlIconUrl = fetchFaviconUrlFromHtml(clean)
        if (htmlIconUrl != null) {
            val bitmap = downloadFaviconWithCoil(htmlIconUrl, context)
            if (bitmap != null) {
                val path = saveBitmapToInternalStorage(context, bitmap)
                if (path != null) return@withContext DownloadOutcome(DownloadResult.SUCCESS, path)
            }
        }

        // 2. 尝试默认路径
        val faviconUrls = listOf(
            "https://$clean/favicon.ico",
            "https://$clean/favicon.png",
            "https://$clean/apple-touch-icon.png"
        )

        for (url in faviconUrls) {
            try {
                Logcat.d(TAG, "Trying: $url")
                val bitmap = downloadFaviconWithCoil(url, context)
                if (bitmap != null) {
                    val savedPath = saveBitmapToInternalStorage(context, bitmap)
                    if (savedPath != null) {
                        Logcat.d(TAG, "Successfully downloaded favicon from: $url")
                        return@withContext DownloadOutcome(DownloadResult.SUCCESS, savedPath)
                    }
                }
            } catch (e: Exception) {
                Logcat.e(TAG, "Failed to download from $url", e)
            }
        }

        Logcat.w(TAG, "Failed to download favicon from: $input")
        DownloadOutcome(DownloadResult.NETWORK_ERROR)
    }

    fun cleanDomain(domain: String): String {
        var clean = domain.trim()
        clean = clean.removePrefix("http://")
        clean = clean.removePrefix("https://")
        clean = clean.split("/").firstOrNull() ?: ""
        clean = clean.split(":").firstOrNull() ?: ""
        return clean
    }

    private suspend fun downloadFaviconWithCoil(urlString: String, context: Context): Bitmap? {
        return try {
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()

            val request = ImageRequest.Builder(context)
                .data(urlString)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val result = imageLoader.execute(request)

            val drawable = result.drawable
            drawable?.toBitmap()
        } catch (e: Exception) {
            Logcat.e(TAG, "Error downloading favicon with Coil from $urlString", e)
            null
        }
    }

    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val directory = File(context.filesDir, "vault_images").apply {
                if (!exists()) mkdirs()
            }

            val fileName = "favicon_${UUID.randomUUID()}.png"
            val destFile = File(directory, fileName)

            FileOutputStream(destFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            Logcat.d(TAG, "Favicon saved to: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Logcat.e(TAG, "Error saving favicon to storage", e)
            null
        }
    }
}
