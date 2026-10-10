package com.minecraftmc22.expenses.common.presentation

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.minecraftmc22.expenses.R

/**
 * Accent the app is painted with, picked in settings.
 *
 * Each entry points at a theme overlay that rewrites `colorPrimary` — and the custom
 * `themeColor` attribute, which is what the toolbar overlay reads, because a theme overlay
 * cannot reference `colorPrimary` while it is the one defining it.
 */
enum class ThemeColor(
    @StringRes val labelResId: Int,
    @ColorRes val colorResId: Int,
    @StyleRes val overlayResId: Int
) {

    DEFAULT(
        R.string.theme_color_default,
        R.color.expenses_blue,
        R.style.ThemeOverlay_Expenses_ThemeColor_Default
    ),
    TEAL(
        R.string.theme_color_teal,
        R.color.theme_color_teal,
        R.style.ThemeOverlay_Expenses_ThemeColor_Teal
    ),
    GREEN(
        R.string.theme_color_green,
        R.color.theme_color_green,
        R.style.ThemeOverlay_Expenses_ThemeColor_Green
    ),
    ORANGE(
        R.string.theme_color_orange,
        R.color.theme_color_orange,
        R.style.ThemeOverlay_Expenses_ThemeColor_Orange
    ),
    PURPLE(
        R.string.theme_color_purple,
        R.color.theme_color_purple,
        R.style.ThemeOverlay_Expenses_ThemeColor_Purple
    ),
    PINK(
        R.string.theme_color_pink,
        R.color.theme_color_pink,
        R.style.ThemeOverlay_Expenses_ThemeColor_Pink
    ),

    /**
     * Any colour the user mixes themselves. It has no overlay of its own — a theme overlay has to
     * exist as a resource, so an arbitrary colour cannot become one. The default overlay is
     * applied instead and the views are repainted with the stored colour afterwards.
     */
    CUSTOM(
        R.string.theme_color_custom,
        R.color.expenses_blue,
        R.style.ThemeOverlay_Expenses_ThemeColor_Default
    );

    fun toDisplayName(context: Context): String = context.getString(labelResId)

    fun toColor(context: Context): Int = ContextCompat.getColor(context, colorResId)
}
