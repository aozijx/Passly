package com.aozijx.passly.core.media

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.diagnostics.AppLog
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FaviconUtils {

    private const val TAG = "FaviconUtils"
    private const val MAX_HTML_BYTES = 512 * 1024L // HTML 解析上限 512 KB
    private val PINNED_FAVICON_HOSTS = setOf(
        "www.google.com",
        "icons.duckduckgo.com"
    )

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
    suspend fun fetchFaviconUrlFromHtml(
        domain: String,
        whitelist: Set<String> = emptySet()
    ): String? = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            AppLog.w(
                TAG,
                "Favicon HTML fetch is disabled in release build; release uses pinned favicon providers only"
            )
            return@withContext null
        }

        try {
            val clean = cleanDomain(domain)
            if (clean.isBlank()) return@withContext null
            if (isRestrictedHost(clean)) {
                AppLog.w(TAG, "Reject favicon fetch for restricted host: $clean")
                return@withContext null
            }

            val url = "https://$clean"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            val html = try {
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                // 关闭自动跳转：防止目标站点把我们引向内网或非 HTTP(S) 资源。
                connection.instanceFollowRedirects = false
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                )

                connection.inputStream.use { stream ->
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
            } finally {
                connection.disconnect()
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
                    return@withContext resolveUrl(url, href)?.takeIf {
                        isAllowedRemoteUrl(
                            it,
                            whitelist
                        )
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to parse HTML for favicon: $domain", e)
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
            "${uri.scheme}://${uri.host}${path.take(lastSlash + 1)}"
        }
        return base + href
    }

    private fun isAllowedRemoteUrl(url: String, whitelist: Set<String> = emptySet()): Boolean {
        return try {
            val parsed = java.net.URI(url)
            val scheme = parsed.scheme?.lowercase()
            val host = parsed.host?.lowercase()
            if (scheme != "http" && scheme != "https") return false
            if (host.isNullOrBlank() || isRestrictedHost(host)) return false
            if (whitelist.isNotEmpty()) {
                whitelist.any {
                    host.equals(it, ignoreCase = true) || host.endsWith(
                        ".$it",
                        ignoreCase = true
                    )
                }
            } else {
                BuildConfig.DEBUG || host in PINNED_FAVICON_HOSTS
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun buildPinnedProviderUrls(domain: String): List<String> {
        return listOf(
            "https://www.google.com/s2/favicons?sz=256&domain=$domain",
            "https://icons.duckduckgo.com/ip3/$domain.ico"
        )
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
            if (parts.any { it !in 0..255 }) return true
            val a = parts[0]; val b = parts[1]
            if (a == 0 || a == 10 || a == 127) return true
            if (a == 169 && b == 254) return true
            if (a == 172 && b in 16..31) return true
            if (a == 192 && b == 168) return true
            if (a >= 224) return true // 组播 / 保留段
        }
        return false
    }

    suspend fun downloadAndSaveFavicon(
        input: String,
        context: Context,
        whitelist: Set<String> = emptySet()
    ): DownloadOutcome = withContext(Dispatchers.IO) {
        if (input.isBlank()) return@withContext DownloadOutcome(DownloadResult.EMPTY_INPUT)

        AppLog.d(TAG, "Trying to download favicon from: $input")

        val isDirectUrl = input.startsWith("http://") || input.startsWith("https://")

        if (isDirectUrl) {
            if (!isAllowedRemoteUrl(input, whitelist)) {
                AppLog.w(TAG, "Reject favicon download for restricted url")
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
            AppLog.w(TAG, "Reject favicon download for restricted domain")
            return@withContext DownloadOutcome(DownloadResult.NETWORK_ERROR)
        }

        if (!BuildConfig.DEBUG) {
            for (url in buildPinnedProviderUrls(clean)) {
                try {
                    AppLog.d(TAG, "Trying pinned provider: $url")
                    val bitmap = downloadFaviconWithCoil(url, context)
                    if (bitmap != null) {
                        val savedPath = saveBitmapToInternalStorage(context, bitmap)
                        if (savedPath != null) {
                            AppLog.d(TAG, "Successfully downloaded favicon from pinned provider")
                            return@withContext DownloadOutcome(DownloadResult.SUCCESS, savedPath)
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e(TAG, "Pinned provider favicon download failed: $url", e)
                }
            }

            AppLog.w(TAG, "Failed to download favicon from pinned providers for: $input")
            return@withContext DownloadOutcome(DownloadResult.NETWORK_ERROR)
        }

        val htmlIconUrl = fetchFaviconUrlFromHtml(clean, whitelist)
        if (htmlIconUrl != null) {
            val bitmap = downloadFaviconWithCoil(htmlIconUrl, context)
            if (bitmap != null) {
                val path = saveBitmapToInternalStorage(context, bitmap)
                if (path != null) return@withContext DownloadOutcome(DownloadResult.SUCCESS, path)
            }
        }

        val faviconUrls = listOf(
            "https://$clean/favicon.ico",
            "https://$clean/favicon.png",
            "https://$clean/apple-touch-icon.png"
        )

        for (url in faviconUrls) {
            try {
                if (!isAllowedRemoteUrl(url, whitelist)) {
                    AppLog.w(TAG, "Reject favicon download for restricted url: $url")
                    continue
                }
                AppLog.d(TAG, "Trying: $url")
                val bitmap = downloadFaviconWithCoil(url, context)
                if (bitmap != null) {
                    val savedPath = saveBitmapToInternalStorage(context, bitmap)
                    if (savedPath != null) {
                        AppLog.d(TAG, "Successfully downloaded favicon from: $url")
                        return@withContext DownloadOutcome(DownloadResult.SUCCESS, savedPath)
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to download from $url", e)
            }
        }

        AppLog.w(TAG, "Failed to download favicon from: $input")
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
            AppLog.e(TAG, "Error downloading favicon with Coil from $urlString", e)
            null
        }
    }

    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val directory = File(context.filesDir, "vault_images").apply {
                if (!exists()) mkdirs()
            }

            val fileName = "favicon_${UuidCreator.getTimeOrderedEpoch()}.png"
            val destFile = File(directory, fileName)

            FileOutputStream(destFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            AppLog.d(TAG, "Favicon saved to: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            AppLog.e(TAG, "Error saving favicon to storage", e)
            null
        }
    }
}