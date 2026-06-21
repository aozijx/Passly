package com.aozijx.passly.ui.features.verification

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.ui.features.verification.components.BiometricUnlockButton
import com.aozijx.passly.ui.features.verification.components.PasswordUnlockSection
import com.aozijx.passly.ui.features.verification.components.SetPasswordDialogSection
import com.aozijx.passly.ui.features.verification.components.SetPasswordEntrySection

private enum class AuthChannel {
    Biometric,
    Password,
    SetPassword
}

@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel,
    activity: FragmentActivity
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appPasswordEnabled by viewModel.isAppPasswordEnabled.collectAsStateWithLifecycle()

    val biometricAvailable = BiometricManager.from(activity)
        .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    val title = stringResource(R.string.vault_auth_decrypt_title)
    val subtitle = stringResource(R.string.vault_auth_subtitle)

    val mainChannel = when {
        biometricAvailable -> AuthChannel.Biometric
        appPasswordEnabled -> AuthChannel.Password
        else -> AuthChannel.SetPassword
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.vault_locked_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (mainChannel) {
                AuthChannel.Biometric -> {
                    BiometricUnlockButton(state.authInProgress) {
                        viewModel.verifyWithBiometric(activity, title, subtitle)
                    }
                    if (appPasswordEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PasswordUnlockSection(
                            appPassword = state.appPassword,
                            showPasswordInput = state.showPasswordInput,
                            authInProgress = state.authInProgress,
                            onPasswordChange = viewModel::onPasswordChange,
                            onExpandInput = viewModel::onShowPasswordInput,
                            onUnlockRequest = viewModel::verifyWithAppPassword
                        )
                    }
                }

                AuthChannel.Password -> {
                    PasswordUnlockSection(
                        appPassword = state.appPassword,
                        showPasswordInput = state.showPasswordInput,
                        authInProgress = state.authInProgress,
                        onPasswordChange = viewModel::onPasswordChange,
                        onExpandInput = viewModel::onShowPasswordInput,
                        onUnlockRequest = viewModel::verifyWithAppPassword
                    )
                }

                AuthChannel.SetPassword -> {
                    SetPasswordEntrySection(state.authInProgress) {
                        viewModel.onShowSetPasswordDialog()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.auth_biometric_unavailable_password_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (state.showSetPasswordDialog) {
        SetPasswordDialogSection(
            activity = activity,
            viewModel = viewModel,
            appPassword = state.appPassword,
            appPasswordConfirm = state.appPasswordConfirm,
            passwordMismatchMessage = stringResource(R.string.auth_password_mismatch),
            emptyPasswordMessage = stringResource(R.string.auth_password_empty),
            passwordSetSuccessMessage = stringResource(R.string.auth_password_set_success),
            passwordSetFailedMessage = stringResource(R.string.auth_password_set_failed)
        )
    }
}