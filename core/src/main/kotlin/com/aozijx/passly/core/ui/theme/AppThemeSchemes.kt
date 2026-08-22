package com.aozijx.passly.core.ui.theme

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.aozijx.passly.core.R

/** References to the three coordinated seed colors of a manual scheme. */
data class ThemeSeedResources(
    @param:ColorRes val primary: Int,
    @param:ColorRes val secondary: Int,
    @param:ColorRes val tertiary: Int,
)

/** One complete manual theme option: a stable key, localized name, and coordinated seeds. */
data class ThemeSchemeDefinition(
    val key: String,
    @param:StringRes val nameRes: Int,
    val seedResources: ThemeSeedResources,
)

/** Single source of truth for manual themes. Add a theme here; consumers require no mapping code. */
object AppThemeSchemes {
    val all = listOf(
        ThemeSchemeDefinition(
            "horizon",
            R.string.theme_scheme_horizon,
            ThemeSeedResources(
                R.color.theme_seed_horizon_primary,
                R.color.theme_seed_horizon_secondary,
                R.color.theme_seed_horizon_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "grove",
            R.string.theme_scheme_grove,
            ThemeSeedResources(
                R.color.theme_seed_grove_primary,
                R.color.theme_seed_grove_secondary,
                R.color.theme_seed_grove_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "ember",
            R.string.theme_scheme_ember,
            ThemeSeedResources(
                R.color.theme_seed_ember_primary,
                R.color.theme_seed_ember_secondary,
                R.color.theme_seed_ember_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "orchid",
            R.string.theme_scheme_orchid,
            ThemeSeedResources(
                R.color.theme_seed_orchid_primary,
                R.color.theme_seed_orchid_secondary,
                R.color.theme_seed_orchid_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "solstice",
            R.string.theme_scheme_solstice,
            ThemeSeedResources(
                R.color.theme_seed_solstice_primary,
                R.color.theme_seed_solstice_secondary,
                R.color.theme_seed_solstice_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "tideline",
            R.string.theme_scheme_tideline,
            ThemeSeedResources(
                R.color.theme_seed_tideline_primary,
                R.color.theme_seed_tideline_secondary,
                R.color.theme_seed_tideline_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "rosewood",
            R.string.theme_scheme_rosewood,
            ThemeSeedResources(
                R.color.theme_seed_rosewood_primary,
                R.color.theme_seed_rosewood_secondary,
                R.color.theme_seed_rosewood_tertiary,
            ),
        ),
    )

    fun find(key: String): ThemeSchemeDefinition? = all.firstOrNull { it.key == key }
}
