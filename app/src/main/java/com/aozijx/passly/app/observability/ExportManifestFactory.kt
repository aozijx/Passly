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

data class ExportManifest(
    val schemaVersion: Int,
    val generatedAtMs: Long,
    val recordCount: Int,
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
    fun create(recordCount: Int): ExportManifest {
        require(recordCount >= 0)
        val context = contextProvider.current()
        return ExportManifest(
            schemaVersion = SCHEMA_VERSION,
            generatedAtMs = clock.nowMs(),
            recordCount = recordCount,
            appName = context.appName,
            appVersionName = context.appVersionName,
            appVersionCode = context.appVersionCode,
            manufacturer = context.manufacturer,
            model = context.model,
            sdkInt = context.sdkInt,
            osRelease = context.osRelease,
            locales = context.locales,
            timezone = context.timezone,
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

    private fun currentLocales(): List<String> {
        val locales = context.resources.configuration.locales
        return (0 until locales.size()).map { locales[it].toLanguageTag().bounded() }.take(MAX_LOCALES)
    }

    private fun String.bounded(): String = take(MAX_CONTEXT_CHARS)

    private companion object {
        const val MAX_CONTEXT_CHARS = 128
        const val MAX_LOCALES = 8
    }
}
