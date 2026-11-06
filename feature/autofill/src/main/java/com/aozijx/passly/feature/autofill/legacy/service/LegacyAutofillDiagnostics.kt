package com.aozijx.passly.feature.autofill.legacy.service

import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.SaveRequest
import com.aozijx.passly.core.observability.AutofillLogSnapshot
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseBuildResult
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import javax.inject.Inject
import javax.inject.Singleton

/** Bridges Android Autofill callbacks to privacy-safe application diagnostics. */
@Singleton
internal class LegacyAutofillDiagnostics
    @Inject
    constructor(
        private val logger: Logger,
    ) {
        fun fillRequestReceived(request: FillRequest) {
            logger.log(
                LogStatements.Autofill.Legacy.fillRequestReceived(
                    targetPackage = request.fillContexts.targetPackage(),
                    request =
                        AutofillLogSnapshot.Legacy.FillRequest(
                            fillContextCount = request.fillContexts.size,
                            inlineRequestAvailable = request.inlineSuggestionsRequest != null,
                            requestFlags = request.flags,
                        ),
                ),
            )
        }

        fun structureParsed(
            parsed: ParsedStructure,
            fillContextCount: Int,
            stage: AutofillLogSnapshot.Legacy.Structure.Stage,
        ) {
            logger.log(
                LogStatements.Autofill.Legacy.structureParsed(
                    targetPackage = parsed.packageName,
                    snapshot = parsed.diagnostics(fillContextCount, stage),
                ),
            )
        }

        fun beginDiscovery(packageName: String): Discovery {
            val fields = LogStatements.Autofill.context(AutofillInteractionSource.LEGACY, packageName)
            logger.log(LogStatements.Autofill.discoveryStarted(fields))
            return Discovery(logger, fields)
        }

        fun presentationSelected(
            presentation: AutofillPresentation,
            inlineAvailable: Boolean,
        ) {
            logger.log(LogStatements.Autofill.presentationSelected(presentation, inlineAvailable))
        }

        fun responseBuilt(
            targetPackage: String,
            result: LegacyResponseBuildResult,
        ) {
            logger.log(
                LogStatements.Autofill.Legacy.responseBuilt(
                    targetPackage,
                    AutofillLogSnapshot.Legacy.Response(
                        present = result.response != null,
                        datasetCount = result.summary.datasetCount,
                        hasSaveInfo = result.summary.hasSaveInfo,
                    ),
                ),
            )
        }

        fun discoveryFailed(
            error: Exception,
            discovery: Discovery?,
        ) {
            if (discovery == null) {
                logger.log(LogStatements.Autofill.discoveryFailed(error))
            } else {
                discovery.failed(error)
            }
        }

        fun saveRequestReceived(request: SaveRequest) {
            logger.log(
                LogStatements.Autofill.Legacy.saveRequestReceived(
                    targetPackage = request.fillContexts.targetPackage(),
                    fillContextCount = request.fillContexts.size,
                ),
            )
        }

        fun saveStarted() = logger.log(LogStatements.Autofill.saveStarted())

        fun saveCompleted() = logger.log(LogStatements.Autofill.saveCompleted())

        fun saveStaged() = logger.log(LogStatements.Autofill.saveStaged())

        fun saveUnavailable(reason: ReasonCode) = logger.log(LogStatements.Autofill.saveUnavailable(reason))

        fun saveFailed(error: Exception) = logger.log(LogStatements.Autofill.saveFailed(error))

        class Discovery internal constructor(
            private val logger: Logger,
            private val fields: LogFields,
        ) {
            fun completed(elapsedMs: Long) {
                logger.log(LogStatements.Autofill.discoveryCompleted(elapsedMs, fields))
            }

            fun unavailable(reason: ReasonCode) {
                logger.log(LogStatements.Autofill.discoveryUnavailable(reason, fields))
            }

            fun failed(error: Exception) {
                logger.log(LogStatements.Autofill.discoveryFailed(error, fields = fields))
            }
        }

        private fun ParsedStructure.diagnostics(
            fillContextCount: Int,
            stage: AutofillLogSnapshot.Legacy.Structure.Stage,
        ) =
            AutofillLogSnapshot.Legacy.Structure(
                stage = stage,
                fillContextCount = fillContextCount,
                traversal =
                    AutofillLogSnapshot.Legacy.Structure.Traversal(
                        windowCount = traversal.windowCount,
                        visitedNodeCount = traversal.visitedNodeCount,
                        truncated = traversal.truncated,
                    ),
                fields =
                    AutofillLogSnapshot.Legacy.Structure.Fields(
                        editableCount = editableFields.size,
                        hintedCount = editableFields.count { it.autofillHints.isNotEmpty() },
                        focusedCount = editableFields.count { it.isFocused },
                        passwordInputTypeCount = editableFields.count { it.inputType?.contains("PASSWORD") == true },
                    ),
                target =
                    AutofillLogSnapshot.Legacy.Structure.Target(
                        hasPackage = !packageName.isNullOrBlank(),
                        hasWebDomain = !webDomain.isNullOrBlank(),
                    ),
            )

        private fun List<FillContext>.targetPackage(): String? =
            lastOrNull()?.structure?.activityComponent?.packageName
    }
