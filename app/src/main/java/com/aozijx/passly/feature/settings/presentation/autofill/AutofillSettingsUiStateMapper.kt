package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.AutofillSettings

/** Inputs emitted by the Android Autofill service-status source. */
internal sealed interface AutofillSystemServiceObservation {
    data object Checking : AutofillSystemServiceObservation

    data class EnabledChanged(
        val isEnabled: Boolean,
    ) : AutofillSystemServiceObservation

    data object Unavailable : AutofillSystemServiceObservation
}

/** Pure projections from settings and platform observations to the screen's renderable state. */
internal object AutofillSettingsUiStateMapper {
    fun preferences(state: RecoverableReadState<AutofillSettings>): AutofillPreferencesState =
        when (state) {
            RecoverableReadState.Loading -> AutofillPreferencesState.Loading
            is RecoverableReadState.Ready -> AutofillPreferencesState.Ready(state.value)
            is RecoverableReadState.Failed -> AutofillPreferencesState.Failed
        }

    fun systemServiceStatus(
        previous: AutofillSystemServiceStatus,
        observation: AutofillSystemServiceObservation,
    ): AutofillSystemServiceStatus =
        when (observation) {
            AutofillSystemServiceObservation.Checking -> {
                previous.lastKnownEnabled?.let { AutofillSystemServiceStatus.Refreshing(it) }
                    ?: AutofillSystemServiceStatus.Checking
            }

            is AutofillSystemServiceObservation.EnabledChanged -> {
                AutofillSystemServiceStatus.Available(observation.isEnabled)
            }

            AutofillSystemServiceObservation.Unavailable -> {
                AutofillSystemServiceStatus.Unavailable(previous.lastKnownEnabled)
            }
        }
}
