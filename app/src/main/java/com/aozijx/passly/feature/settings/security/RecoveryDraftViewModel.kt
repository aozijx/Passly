package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.security.KeyDerivation
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.domain.model.envelope.KdfAlgorithm
import com.aozijx.passly.domain.model.envelope.KeyEnvelope
import com.aozijx.passly.security.authentication.KdfRunner
import com.aozijx.passly.security.authentication.OwnedBytes
import com.aozijx.passly.security.authentication.SecretChars
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.EnvelopeCrypto
import com.aozijx.passly.security.envelope.BootstrapStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

sealed interface RecoveryDraftState {
    data object Empty : RecoveryDraftState
    data object Authenticating : RecoveryDraftState
    data object Generating : RecoveryDraftState
    data class Ready(val generationId: String) : RecoveryDraftState
    data object DraftExpired : RecoveryDraftState
    data object Committed : RecoveryDraftState
    data object Failed : RecoveryDraftState
}

fun RecoveryDraftState.messageOrNull(): String? = when (this) {
    RecoveryDraftState.DraftExpired -> "恢复码草稿已过期，请重新认证后生成。"
    else -> null
}

@HiltViewModel
class RecoveryDraftViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authenticationManager: AuthenticationManager,
    private val kdfRunner: KdfRunner,
    private val dekManager: DekManager,
    private val bootstrapStore: BootstrapStore
) : ViewModel() {
    private val random = SecureRandom()
    private val draft = EphemeralRecoveryDraft()
    private val _state = MutableStateFlow<RecoveryDraftState>(
        if (savedStateHandle.get<Boolean>(WAS_DISCLOSURE_OPEN) == true) RecoveryDraftState.DraftExpired
        else RecoveryDraftState.Empty
    )
    val state: StateFlow<RecoveryDraftState> = _state.asStateFlow()

    fun generate() {
        if (_state.value is RecoveryDraftState.Authenticating ||
            _state.value is RecoveryDraftState.Generating
        ) return
        viewModelScope.launch {
            _state.value = RecoveryDraftState.Authenticating
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.MANAGE_RECOVERY_CODE)
                )
            ) {
                is AuthenticationResult.Success -> createDraft()
                is AuthenticationResult.Cancelled -> _state.value = RecoveryDraftState.Empty
                is AuthenticationResult.Failure -> _state.value = RecoveryDraftState.Failed
            }
        }
    }

    fun revealCode(): CharArray? = draft.copyCode()

    fun confirmAndEnable() {
        viewModelScope.launch {
            val envelope = draft.copyEnvelope() ?: return@launch
            runCatching { bootstrapStore.save(envelope) }
                .onSuccess {
                    draft.clear()
                    savedStateHandle[WAS_DISCLOSURE_OPEN] = false
                    savedStateHandle[DRAFT_GENERATION_ID] = null as String?
                    _state.value = RecoveryDraftState.Committed
                }
                .onFailure { KeyEnvelope.destroy(envelope) }
        }
    }

    fun dismiss() {
        draft.clear()
        savedStateHandle[WAS_DISCLOSURE_OPEN] = false
        savedStateHandle[DRAFT_GENERATION_ID] = null as String?
        _state.value = RecoveryDraftState.Empty
    }

    private suspend fun createDraft() {
        _state.value = RecoveryDraftState.Generating
        val code = CharArray(CODE_LENGTH) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
        val salt = KeyDerivation.generateSalt()
        val secret = SecretChars.copyOf(code)
        try {
            val ownedKey = kdfRunner.execute(secret) { chars ->
                OwnedBytes(KeyDerivation.deriveKeyBytesArgon2id(chars, salt))
            }
            val rawKey = ownedKey.consume()
            val envelope = try {
                dekManager.withDek { dek ->
                    EnvelopeCrypto.wrapWithKey(
                        type = EnvelopeType.RECOVERY,
                        dek = dek,
                        wrappingKey = SecretKeySpec(rawKey, "AES"),
                        salt = salt,
                        algorithm = KdfAlgorithm.ARGON2ID
                    )
                }
            } finally {
                rawKey.fill(0)
                ownedKey.discard()
            }
            val generationId = UUID.randomUUID().toString()
            draft.replace(code, envelope)
            savedStateHandle[WAS_DISCLOSURE_OPEN] = true
            savedStateHandle[DRAFT_GENERATION_ID] = generationId
            _state.value = RecoveryDraftState.Ready(generationId)
        } catch (cancelled: CancellationException) {
            code.fill('\u0000')
            salt.fill(0)
            throw cancelled
        } catch (_: Throwable) {
            code.fill('\u0000')
            salt.fill(0)
            _state.value = RecoveryDraftState.Failed
        } finally {
            secret.close()
        }
    }

    override fun onCleared() {
        draft.clear()
    }

    private companion object {
        const val WAS_DISCLOSURE_OPEN = "wasRecoveryDisclosureOpen"
        const val DRAFT_GENERATION_ID = "recoveryDraftGenerationId"
        const val CODE_LENGTH = 20
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}

private class EphemeralRecoveryDraft {
    private var code: CharArray? = null
    private var envelope: KeyEnvelope? = null

    @Synchronized
    fun replace(newCode: CharArray, newEnvelope: KeyEnvelope) {
        clear()
        code = newCode.copyOf()
        newCode.fill('\u0000')
        envelope = newEnvelope
    }

    @Synchronized
    fun copyCode(): CharArray? = code?.copyOf()

    @Synchronized
    fun copyEnvelope(): KeyEnvelope? = envelope?.let {
        it.copy(
            ciphertext = it.ciphertext.copyOf(),
            iv = it.iv.copyOf(),
            salt = it.salt.copyOf()
        )
    }

    @Synchronized
    fun clear() {
        code?.fill('\u0000')
        code = null
        envelope?.let(KeyEnvelope::destroy)
        envelope = null
    }
}
