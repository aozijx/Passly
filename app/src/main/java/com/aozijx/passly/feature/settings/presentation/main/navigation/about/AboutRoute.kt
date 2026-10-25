package com.aozijx.passly.feature.settings.presentation.main.navigation.about

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.feature.settings.presentation.ui.about.AboutSettingsSection
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutRoute(onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val featurePendingMessage = stringResource(R.string.settings_about_feature_pending)

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.ABOUT.titleRes),
        onBack = onBack,
    ) {
        item {
            SettingsSection {
                AboutSettingsSection(
                    appVersion = BuildConfig.VERSION_NAME,
                    onAppDetailsClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                    onTermsClick = {
                        Toast.makeText(context, featurePendingMessage, Toast.LENGTH_SHORT).show()
                    },
                    onPrivacyPolicyClick = {
                        Toast.makeText(context, featurePendingMessage, Toast.LENGTH_SHORT).show()
                    },
                    onOpenSourceLicensesClick = {
                        Toast.makeText(context, featurePendingMessage, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
