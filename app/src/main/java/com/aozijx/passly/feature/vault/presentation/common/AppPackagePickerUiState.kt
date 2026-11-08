package com.aozijx.passly.feature.vault.presentation.common

sealed interface AppPackagePickerUiState<out T> {
    data object NotRequested : AppPackagePickerUiState<Nothing>

    data object Loading : AppPackagePickerUiState<Nothing>

    data class Ready<T>(
        val apps: List<T>,
        val query: String,
        val results: AppPackagePickerResults<T>,
    ) : AppPackagePickerUiState<T> {
        init {
            when (results) {
                AppPackagePickerResults.NoApps -> require(apps.isEmpty())
                AppPackagePickerResults.NoMatches -> require(apps.isNotEmpty() && query.isNotBlank())
                is AppPackagePickerResults.Matches -> {
                    require(apps.isNotEmpty())
                    require(results.apps.isNotEmpty() && apps.containsAll(results.apps))
                }
            }
        }
    }

    data object Failed : AppPackagePickerUiState<Nothing>
}

sealed interface AppPackagePickerResults<out T> {
    data object NoApps : AppPackagePickerResults<Nothing>

    data object NoMatches : AppPackagePickerResults<Nothing>

    data class Matches<T>(
        val apps: List<T>,
    ) : AppPackagePickerResults<T> {
        init {
            require(apps.isNotEmpty())
        }
    }
}

fun <T> AppPackagePickerUiState.Ready<T>.search(
    query: String,
    label: (T) -> String,
    packageName: (T) -> String,
): AppPackagePickerUiState.Ready<T> {
    val normalizedQuery = query.trim()
    val results =
        when {
            apps.isEmpty() -> AppPackagePickerResults.NoApps
            normalizedQuery.isEmpty() -> AppPackagePickerResults.Matches(apps)
            else -> {
                val matches =
                    apps.filter { app ->
                        label(app).contains(normalizedQuery, ignoreCase = true) ||
                            packageName(app).contains(normalizedQuery, ignoreCase = true)
                    }
                matches
                    .takeIf { it.isNotEmpty() }
                    ?.let { AppPackagePickerResults.Matches(it) }
                    ?: AppPackagePickerResults.NoMatches
            }
        }
    return copy(query = query, results = results)
}

fun <T> appPackagePickerReady(apps: List<T>): AppPackagePickerUiState.Ready<T> {
    val results =
        apps
            .takeIf { it.isNotEmpty() }
            ?.let { AppPackagePickerResults.Matches(it) }
            ?: AppPackagePickerResults.NoApps
    return AppPackagePickerUiState.Ready(apps = apps, query = "", results = results)
}

fun <T, R> AppPackagePickerUiState.Ready<T>.mapItems(transform: (T) -> R): AppPackagePickerUiState.Ready<R> =
    AppPackagePickerUiState.Ready(
        apps = apps.map(transform),
        query = query,
        results =
            when (val currentResults = results) {
                AppPackagePickerResults.NoApps -> AppPackagePickerResults.NoApps
                AppPackagePickerResults.NoMatches -> AppPackagePickerResults.NoMatches
                is AppPackagePickerResults.Matches -> AppPackagePickerResults.Matches(currentResults.apps.map(transform))
            },
    )
