package com.aozijx.passly.feature.vault.presentation.common

sealed interface AppPackagePickerUiState<out T> {
    companion object {
        fun <T> fromInstalledApps(installedApps: List<T>): AppPackagePickerUiState<T> =
            installedApps
                .takeIf { it.isNotEmpty() }
                ?.let { Matches(it, query = "", matchingApps = it) }
                ?: NoApps()
    }

    val query: String
        get() = ""

    val catalogLoaded: Boolean
        get() =
            when (this) {
                is NoApps,
                is NoMatches,
                is Matches,
                -> true

                NotRequested,
                Loading,
                Failed,
                -> false
            }

    data object NotRequested : AppPackagePickerUiState<Nothing>

    data object Loading : AppPackagePickerUiState<Nothing>

    data object Failed : AppPackagePickerUiState<Nothing>

    data class NoApps(
        override val query: String = "",
    ) : AppPackagePickerUiState<Nothing>

    data class NoMatches<T>(
        val installedApps: List<T>,
        override val query: String,
    ) : AppPackagePickerUiState<T> {
        init {
            require(installedApps.isNotEmpty()) { "NoMatches requires a non-empty app catalog" }
            require(query.isNotBlank()) { "NoMatches requires a non-blank query" }
        }
    }

    data class Matches<T>(
        val installedApps: List<T>,
        override val query: String,
        val matchingApps: List<T>,
    ) : AppPackagePickerUiState<T> {
        init {
            require(installedApps.isNotEmpty()) { "Matches requires a non-empty app catalog" }
            require(matchingApps.isNotEmpty()) { "Matches requires at least one matching app" }
            require(installedApps.containsAll(matchingApps)) { "Matching apps must come from the app catalog" }
        }
    }
}

fun <T> AppPackagePickerUiState<T>.search(
    query: String,
    label: (T) -> String,
    packageName: (T) -> String,
): AppPackagePickerUiState<T> {
    val apps =
        when (this) {
            is AppPackagePickerUiState.NoApps -> return copy(query = query)
            is AppPackagePickerUiState.NoMatches -> installedApps
            is AppPackagePickerUiState.Matches -> installedApps
            AppPackagePickerUiState.NotRequested,
            AppPackagePickerUiState.Loading,
            AppPackagePickerUiState.Failed,
            -> return this
        }
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return AppPackagePickerUiState.Matches(apps, query, apps)

    val matchingApps =
        apps.filter { app ->
            label(app).contains(normalizedQuery, ignoreCase = true) ||
                packageName(app).contains(normalizedQuery, ignoreCase = true)
        }
    return matchingApps
        .takeIf { it.isNotEmpty() }
        ?.let { AppPackagePickerUiState.Matches(apps, query, it) }
        ?: AppPackagePickerUiState.NoMatches(apps, query)
}

fun <T, R> AppPackagePickerUiState<T>.mapItems(transform: (T) -> R): AppPackagePickerUiState<R> =
    when (this) {
        AppPackagePickerUiState.NotRequested -> AppPackagePickerUiState.NotRequested
        AppPackagePickerUiState.Loading -> AppPackagePickerUiState.Loading
        AppPackagePickerUiState.Failed -> AppPackagePickerUiState.Failed
        is AppPackagePickerUiState.NoApps -> AppPackagePickerUiState.NoApps(query)
        is AppPackagePickerUiState.NoMatches -> AppPackagePickerUiState.NoMatches(installedApps.map(transform), query)
        is AppPackagePickerUiState.Matches ->
            AppPackagePickerUiState.Matches(
                installedApps = installedApps.map(transform),
                query = query,
                matchingApps = matchingApps.map(transform),
            )
    }
