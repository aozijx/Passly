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
            "default",
            R.string.theme_scheme_default,
            ThemeSeedResources(
                R.color.theme_seed_default_primary,
                R.color.theme_seed_default_secondary,
                R.color.theme_seed_default_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "aurora",
            R.string.theme_scheme_aurora,
            ThemeSeedResources(
                R.color.theme_seed_aurora_primary,
                R.color.theme_seed_aurora_secondary,
                R.color.theme_seed_aurora_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "sakura",
            R.string.theme_scheme_sakura,
            ThemeSeedResources(
                R.color.theme_seed_sakura_primary,
                R.color.theme_seed_sakura_secondary,
                R.color.theme_seed_sakura_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "ocean",
            R.string.theme_scheme_ocean,
            ThemeSeedResources(
                R.color.theme_seed_ocean_primary,
                R.color.theme_seed_ocean_secondary,
                R.color.theme_seed_ocean_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "forest",
            R.string.theme_scheme_forest,
            ThemeSeedResources(
                R.color.theme_seed_forest_primary,
                R.color.theme_seed_forest_secondary,
                R.color.theme_seed_forest_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "sunset",
            R.string.theme_scheme_sunset,
            ThemeSeedResources(
                R.color.theme_seed_sunset_primary,
                R.color.theme_seed_sunset_secondary,
                R.color.theme_seed_sunset_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "lavender",
            R.string.theme_scheme_lavender,
            ThemeSeedResources(
                R.color.theme_seed_lavender_primary,
                R.color.theme_seed_lavender_secondary,
                R.color.theme_seed_lavender_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "mint",
            R.string.theme_scheme_mint,
            ThemeSeedResources(
                R.color.theme_seed_mint_primary,
                R.color.theme_seed_mint_secondary,
                R.color.theme_seed_mint_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "graphite",
            R.string.theme_scheme_graphite,
            ThemeSeedResources(
                R.color.theme_seed_graphite_primary,
                R.color.theme_seed_graphite_secondary,
                R.color.theme_seed_graphite_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "amber",
            R.string.theme_scheme_amber,
            ThemeSeedResources(
                R.color.theme_seed_amber_primary,
                R.color.theme_seed_amber_secondary,
                R.color.theme_seed_amber_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "ruby",
            R.string.theme_scheme_ruby,
            ThemeSeedResources(
                R.color.theme_seed_ruby_primary,
                R.color.theme_seed_ruby_secondary,
                R.color.theme_seed_ruby_tertiary,
            ),
        ),
        ThemeSchemeDefinition(
            "indigo",
            R.string.theme_scheme_indigo,
            ThemeSeedResources(
                R.color.theme_seed_indigo_primary,
                R.color.theme_seed_indigo_secondary,
                R.color.theme_seed_indigo_tertiary,
            ),
        ),
    )

    fun find(key: String): ThemeSchemeDefinition? = all.firstOrNull { it.key == key }
}
