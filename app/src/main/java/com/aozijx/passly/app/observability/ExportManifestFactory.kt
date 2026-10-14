package com.aozijx.passly.app.observability

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import com.aozijx.passly.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
    val legacyAutofillEnabled: Boolean,
    val credentialProviderAvailable: Boolean,
    val inlineSuggestionsSupported: Boolean,
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
    fun create(recordCount: Int): JsonObject {
        require(recordCount >= 0)
        val context = contextProvider.current()
        return buildJsonObject {
            put("record_type", "manifest")
            put("schema_version", SCHEMA_VERSION)
            put("export", buildJsonObject {
                put("generated_at_ms", clock.nowMs())
                put("record_count", recordCount)
            })
            put("app", buildJsonObject {
                put("name", context.appName)
                put("version_name", context.appVersionName)
                put("version_code", context.appVersionCode)
            })
            put("device", buildJsonObject {
                put("manufacturer", context.manufacturer)
                put("model", context.model)
                put("sdk_int", context.sdkInt)
                put("os_release", context.osRelease)
                put("locales", JsonArray(context.locales.map(::JsonPrimitive)))
                put("timezone", context.timezone)
            })
            put("autofill", buildJsonObject {
                put("legacy_service_enabled", context.legacyAutofillEnabled)
                put("credential_provider_available", context.credentialProviderAvailable)
                put("inline_suggestions_supported", context.inlineSuggestionsSupported)
            })
        }
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
        legacyAutofillEnabled = isLegacyAutofillEnabled(),
        credentialProviderAvailable = isCredentialProviderAvailable(),
        inlineSuggestionsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            context.getSystemService(AutofillManager::class.java)?.isEnabled == true,
    )

    private fun currentLocales(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locales = context.resources.configuration.locales
        (0 until locales.size()).map { locales[it].toLanguageTag().bounded() }.take(MAX_LOCALES)
    } else {
        @Suppress("DEPRECATION")
        listOf(context.resources.configuration.locale.toLanguageTag().bounded())
    }

    private fun isLegacyAutofillEnabled(): Boolean {
        val selected = Settings.Secure.getString(
            context.contentResolver,
            AUTOFILL_SERVICE_SETTING,
        ) ?: return false
        return ComponentName.unflattenFromString(selected)?.packageName == context.packageName
    }

    private fun isCredentialProviderAvailable(): Boolean = runCatching {
        val component = ComponentName(
            context.packageName,
            "${context.packageName}.feature.autofill.credential.service.ModernCredentialService",
        )
        context.packageManager.getServiceInfo(component, 0).enabled
    }.getOrDefault(false)

    private fun String.bounded(): String = take(MAX_CONTEXT_CHARS)

    private companion object {
        const val MAX_CONTEXT_CHARS = 128
        const val MAX_LOCALES = 8
        const val AUTOFILL_SERVICE_SETTING = "autofill_service"
    }
}
