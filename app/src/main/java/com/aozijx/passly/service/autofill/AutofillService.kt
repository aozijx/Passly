package com.aozijx.passly.service.autofill

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.platform.PackageUtils
import com.aozijx.passly.domain.usecase.autofill.AutofillUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.service.autofill.builder.AutofillResponseBuilder
import com.aozijx.passly.service.autofill.parser.AutofillStructureParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutofillService : android.service.autofill.AutofillService() {

    @Inject
    lateinit var autofillUseCases: AutofillUseCases

    @Inject
    lateinit var systemSettingsUseCases: SystemSettingsUseCases

    @Inject
    lateinit var passphraseManager: DatabasePassphraseManager

    @Inject
    lateinit var packageUtils: PackageUtils

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "PasslyAutofill"
    private val slowFillTotalMs = 250L
    private val slowRepositoryMs = 120L
    private val slowDatasetBuildMs = 120L
    private val slowSaveMs = 180L

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(
        request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback
    ) {
        val structure = request.fillContexts.last().structure

        serviceScope.launch {
            try {
                val fillStart = System.currentTimeMillis()
                val parser = AutofillStructureParser(structure)
                val autofillUiMode = systemSettingsUseCases.autofillUiMode.first()
                Logcat.d(
                    tag, "onFillRequest: pkg=${parser.packageName}, domain=${parser.webDomain}"
                )

                val availableIds =
                    listOfNotNull(parser.usernameId, parser.passwordId, parser.otpId)
                Logcat.d(
                    tag,
                    "parsed ids: username=${parser.usernameId != null}, password=${parser.passwordId != null}, otp=${parser.otpId != null}, available=${availableIds.size}"
                )
                if (availableIds.isEmpty()) {
                    Logcat.w(tag, "No autofill ids found; skip suggestions for this request")
                    callback.onSuccess(null)
                    return@launch
                }

                if (passphraseManager.isLocked) {
                    Logcat.i(tag, "Database is locked, suggesting unlock via AuthActivity")
                    val response = AutofillResponseBuilder.buildUnlockResponse(
                        applicationContext, parser, availableIds.toTypedArray(), autofillUiMode
                    )
                    callback.onSuccess(response)
                    return@launch
                }

                val repositoryStart = System.currentTimeMillis()
                val candidates = autofillUseCases.findMatchingCandidates(
                    packageName = parser.normalizedPackageName,
                    webDomain = parser.normalizedWebDomain
                )
                val repositoryCost = System.currentTimeMillis() - repositoryStart
                if (repositoryCost >= slowRepositoryMs) {
                    Logcat.w(
                        tag,
                        "onFillRequest repository slow: ${repositoryCost}ms, entries=${candidates.size}"
                    )
                }

                val buildStart = System.currentTimeMillis()
                val response = AutofillResponseBuilder.buildFillResponse(
                    applicationContext,
                    candidates,
                    parser,
                    autofillUiMode,
                    availableIds,
                    packageUtils
                )
                val buildCost = System.currentTimeMillis() - buildStart
                if (buildCost >= slowDatasetBuildMs) {
                    Logcat.w(
                        tag,
                        "onFillRequest dataset build slow: ${buildCost}ms, entries=${candidates.size}"
                    )
                }

                val saveIds = listOfNotNull(parser.usernameId, parser.passwordId)
                callback.onSuccess(
                    if (candidates.isNotEmpty() || saveIds.isNotEmpty()) response else null
                )

                val totalCost = System.currentTimeMillis() - fillStart
                if (totalCost >= slowFillTotalMs) {
                    Logcat.w(
                        tag,
                        "onFillRequest slow total: ${totalCost}ms, entries=${candidates.size}, saveIds=${saveIds.size}"
                    )
                }

            } catch (e: Exception) {
                Logcat.e(tag, "Fill request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        var username = ""
        var password = ""
        var pkg: String? = null
        var domain: String? = null
        var title: String? = null

        request.fillContexts.forEach { context ->
            val p = AutofillStructureParser(context.structure)
            if (pkg == null) pkg = p.packageName
            if (domain == null) domain = p.webDomain
            if (title == null) title = p.pageTitle
            if (!p.usernameValue.isNullOrBlank()) username = p.usernameValue!!
            if (!p.passwordValue.isNullOrBlank()) password = p.passwordValue!!
        }

        Logcat.d(
            tag,
            "onSaveRequest: captured user=$username, hasPwd=${password.isNotBlank()}, pkg=$pkg"
        )

        if (password.isBlank()) {
            Logcat.w(tag, "onSaveRequest: password is blank, ignore save")
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            try {
                if (passphraseManager.isLocked) {
                    Logcat.w(tag, "onSaveRequest: DB locked, cannot save")
                    return@launch callback.onFailure(getString(R.string.autofill_locked))
                }

                val saveStart = System.currentTimeMillis()
                val success = autofillUseCases.saveOrUpdateEntry(
                    packageName = pkg,
                    webDomain = domain,
                    pageTitle = title,
                    usernameValue = username,
                    passwordValue = password
                )
                if (success) {
                    callback.onSuccess()
                } else {
                    Logcat.e(tag, "Failed to save credentials")
                    callback.onFailure(getString(R.string.autofill_save_failed_repository))
                }
                val saveCost = System.currentTimeMillis() - saveStart
                if (saveCost >= slowSaveMs) Logcat.w(tag, "onSaveRequest slow: ${saveCost}ms")
            } catch (e: Exception) {
                Logcat.e(tag, "Exception during save", e)
                callback.onFailure(e.message)
            }
        }
    }
}