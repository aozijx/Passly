package com.aozijx.passly.feature.settings.navigation

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.apppassword.AppPasswordAction
import com.aozijx.passly.feature.settings.apppassword.handleAppPasswordAction
import com.aozijx.passly.feature.settings.contract.SettingsEffect
import com.aozijx.passly.feature.settings.contract.SettingsIntent
import com.aozijx.passly.feature.settings.datamanagement.DataUiAction
import com.aozijx.passly.feature.settings.datamanagement.DataViewModel
import com.aozijx.passly.feature.settings.interaction.InteractionViewModel
import com.aozijx.passly.feature.settings.shell.SettingsScreenDialogsHost
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsActions
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsState
import com.aozijx.passly.feature.settings.shell.rememberSettingsScreenLocalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    onUpdateInteraction: () -> Unit,
    onOuterBack: () -> Unit,
    onAuthRequired: (title: String, subtitle: String, onSuccess: () -> Unit) -> Unit =
        { _, _, callback -> callback() }
) {
    val localState = rememberSettingsScreenLocalState()
    val context = LocalContext.current
    val interactionViewModel: InteractionViewModel = hiltViewModel()
    val interactionState by interactionViewModel.config.collectAsStateWithLifecycle()
    val dataViewModel: DataViewModel = hiltViewModel()
    val dataState by dataViewModel.config.collectAsStateWithLifecycle()

    val authDecryptTitle = stringResource(R.string.vault_auth_decrypt_title)
    val setAppPasswordSubtitle =
        stringResource(R.string.settings_auth_before_set_app_password)

    fun submitAppPasswordAction(action: AppPasswordAction) {
        handleAppPasswordAction(
            context = context,
            action = action,
            currentPassword = localState.appPasswordCurrent,
            newPassword = localState.appPasswordNew,
            confirmPassword = localState.appPasswordConfirm,
            settingsViewModel = settingsViewModel,
            onSuccess = localState::onAppPasswordSuccess
        )
    }

    LaunchedEffect(dataState.backupMessage) {
        dataState.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            dataViewModel.onAction(DataUiAction.ClearBackupMessage)
        }
    }
    LaunchedEffect(Unit) {
        settingsViewModel.effects.collect { effect ->
            val message = when (effect) {
                is SettingsEffect.ShowError -> effect.message
                is SettingsEffect.SettingsSaved -> "设置已保存"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(350)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(350)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(350)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(350)
            )
        }
    ) {
        registerCoreSettingsRoutes(
            navController = navController,
            context = context,
            localState = localState,
            settingsViewModel = settingsViewModel,
            onUpdateInteraction = onUpdateInteraction,
            onOuterBack = onOuterBack,
            authDecryptTitle = authDecryptTitle,
            setAppPasswordSubtitle = setAppPasswordSubtitle
        )
        registerDataSettingsRoutes(
            navController = navController,
            context = context,
            localState = localState,
            interactionViewModel = interactionViewModel,
            dataViewModel = dataViewModel,
            onAuthRequired = onAuthRequired
        )
    }

    SettingsScreenDialogsHost(
        state = buildSettingsDialogsState(
            localState = localState,
            swipeLeftAction = interactionState.swipeLeftAction,
            swipeRightAction = interactionState.swipeRightAction,
            backupDirectoryUri = dataState.directoryUri,
            context = context
        ),
        actions = buildSettingsDialogsActions(
            localState = localState,
            onSetSwipeRightAction = {
                settingsViewModel.handleIntent(SettingsIntent.SetSwipeRightAction(it))
            },
            onSetSwipeLeftAction = {
                settingsViewModel.handleIntent(SettingsIntent.SetSwipeLeftAction(it))
            },
            submitAppPasswordAction = ::submitAppPasswordAction,
            onClearBackupDirectory = {
                dataViewModel.onAction(DataUiAction.ClearBackupDirectory)
            }
        )
    )
}
