package com.aozijx.passly.app.observability

import android.content.Context
import android.os.Build
import com.aozijx.passly.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject

data class ExportContext(
    val appName: String,
    val appVersionName: String,
    val appVersionCode: Long,
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val osRelease: String,
    val locales: List<String>,
    val timezone: String,
)

fun interface ExportContextProvider {
    fun current(): ExportContext
}

fun interface ExportClock {
    fun nowMs(): Long
}

class ExportManifestFactory @Inject constructor(
    private val contextProvider: ExportContextProvider,
    private val clock: ExportClock,
) {
    fun encode(recordCount: Int): String {
        require(recordCount >= 0)
        val context = contextProvider.current()
        return values(
            "manifest",
            SCHEMA_VERSION,
            clock.nowMs(),
            recordCount,
            context.appName,
            context.appVersionName,
            context.appVersionCode,
            context.manufacturer,
            context.model,
            context.sdkInt,
            context.osRelease,
            context.locales.joinToString(","),
            context.timezone,
        )
    }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

internal class AndroidExportContextProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExportContextProvider {
    override fun current(): ExportContext = ExportContext(
        appName = "Passly",
        appVersionName = BuildConfig.VERSION_NAME,
        appVersionCode = BuildConfig.VERSION_CODE.toLong(),
        manufacturer = Build.MANUFACTURER.orEmpty().bounded(),
        model = Build.MODEL.orEmpty().bounded(),
        sdkInt = Build.VERSION.SDK_INT,
        osRelease = Build.VERSION.RELEASE.orEmpty().bounded(),
        locales = currentLocales(),
        timezone = TimeZone.getDefault().id.bounded(),
    )

    private fun currentLocales(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locales = context.resources.configuration.locales
        (0 until locales.size()).map { locales[it].toLanguageTag().bounded() }.take(MAX_LOCALES)
    } else {
        @Suppress("DEPRECATION")
        listOf(context.resources.configuration.locale.toLanguageTag().bounded())
    }

    private fun String.bounded(): String = take(MAX_CONTEXT_CHARS)

    private companion object {
        const val MAX_CONTEXT_CHARS = 128
        const val MAX_LOCALES = 8
    }
}
