package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.apppassword.AppPasswordAction
import com.aozijx.passly.feature.settings.apppassword.handleAppPasswordAction
import com.aozijx.passly.feature.settings.contract.SettingsEffect
import com.aozijx.passly.feature.settings.contract.SettingsIntent
import com.aozijx.passly.feature.settings.contract.SettingsUiState
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsAction
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsViewModel
import com.aozijx.passly.feature.settings.interaction.InteractionSettingsViewModel
import com.aozijx.passly.feature.settings.shell.SettingsMainPage
import com.aozijx.passly.feature.settings.shell.SettingsScreenDialogsHost
import com.aozijx.passly.feature.settings.shell.SettingsScreenLocalState
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsActions
import com.aozijx.passly.feature.settings.shell.buildSettingsDialogsState
import com.aozijx.passly.feature.settings.shell.rememberSettingsScreenLocalState

/**
 * 宽屏（MEDIUM 及以上）时使用左右两栏布局：左侧为设置列表，右侧显示选中的设置页；
 * 窄屏保持原有单栏推入式导航。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    onOuterBack: () -> Unit
) {
    val localState = rememberSettingsScreenLocalState()
    val context = LocalContext.current
    val interactionViewModel: InteractionSettingsViewModel = hiltViewModel()
    val interactionState by interactionViewModel.config.collectAsStateWithLifecycle()
    val dataViewModel: DataManagementSettingsViewModel = hiltViewModel()
    val dataState by dataViewModel.config.collectAsStateWithLifecycle()
    val backupViewModel: BackupViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val adaptiveLayout = LocalPasslyAdaptiveLayout.current
    val isTwoPane = adaptiveLayout.isAtLeastMedium
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val authDecryptTitle = stringResource(R.string.auth_title)
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

    fun navigateToGroup(route: SettingsRoute) {
        if (currentRoute == route.route) return
        navController.navigate(route.route) {
            popUpTo(SettingsRoute.Main.route)
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.effects.collect { effect ->
            val message = when (effect) {
                is SettingsEffect.ShowError -> effect.message
                is SettingsEffect.SettingsSaved -> "设置已保存"
                is SettingsEffect.DatabaseCleared -> "保险库数据库已永久清除"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val navHostContent: @Composable (Modifier) -> Unit = { modifier ->
        SettingsNavHost(
            navController = navController,
            isTwoPane = isTwoPane,
            context = context,
            localState = localState,
            settingsViewModel = settingsViewModel,
            onOuterBack = onOuterBack,
            onGroupClick = ::navigateToGroup,
            authDecryptTitle = authDecryptTitle,
            setAppPasswordSubtitle = setAppPasswordSubtitle,
            interactionViewModel = interactionViewModel,
            dataViewModel = dataViewModel,
            backupViewModel = backupViewModel,
            settingsState = settingsState,
            modifier = modifier
        )
    }

    if (isTwoPane) {
        Row(modifier = Modifier.fillMaxSize()) {
            SettingsMainPage(
                onBack = onOuterBack,
                onGroupClick = ::navigateToGroup,
                selectedRouteKey = currentRoute,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            )
            VerticalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            navHostContent(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        navHostContent(Modifier.fillMaxSize())
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
                dataViewModel.onAction(DataManagementSettingsAction.ClearBackupDirectory)
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsNavHost(
    navController: NavHostController,
    isTwoPane: Boolean,
    context: Context,
    localState: SettingsScreenLocalState,
    settingsViewModel: SettingsViewModel,
    onOuterBack: () -> Unit,
    onGroupClick: (SettingsRoute) -> Unit,
    authDecryptTitle: String,
    setAppPasswordSubtitle: String,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    backupViewModel: BackupViewModel,
    settingsState: SettingsUiState,
    modifier: Modifier = Modifier
) {
    val motionScheme = MaterialTheme.motionScheme

    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = motionScheme.defaultSpatialSpec()
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = motionScheme.defaultSpatialSpec()
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = motionScheme.defaultSpatialSpec()
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = motionScheme.defaultSpatialSpec()
            )
        }
    ) {
        registerCoreSettingsRoutes(
            navController = navController,
            context = context,
            localState = localState,
            settingsViewModel = settingsViewModel,
            onOuterBack = onOuterBack,
            onGroupClick = onGroupClick,
            isTwoPane = isTwoPane
        )
        registerDataSettingsRoutes(
            navController = navController,
            context = context,
            localState = localState,
            interactionViewModel = interactionViewModel,
            dataViewModel = dataViewModel,
            backupViewModel = backupViewModel,
            settingsViewModel = settingsViewModel,
            settingsState = settingsState
        )
    }
}
