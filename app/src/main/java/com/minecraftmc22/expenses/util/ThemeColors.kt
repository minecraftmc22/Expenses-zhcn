package com.minecraftmc22.expenses.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.R

/** Splits a colour into its three channels, used by the mixer. */
fun redOf(color: Int) = Color.red(color)

fun greenOf(color: Int) = Color.green(color)

fun blueOf(color: Int) = Color.blue(color)

fun colorOf(red: Int, green: Int, blue: Int) = Color.rgb(red, green, blue)

/**
 * Repaints a view tree from the primary colour of the theme to [customColor].
 *
 * A theme overlay is a resource, so an arbitrary colour cannot be turned into one. Everything
 * that draws itself with `?attr/colorPrimary` is therefore recoloured by hand: view backgrounds
 * (the headers, the summary card), text colours (the amount) and background tints (buttons).
 * The secondary colour, which is deliberately a different one, is left alone.
 */
fun View.tintThemeColor(customColor: Int, primaryColor: Int) {
    if (customColor == primaryColor) return

    (background as? ColorDrawable)?.let { drawable ->
        if (drawable.color == primaryColor) setBackgroundColor(customColor)
    }

    backgroundTintList?.let { tint ->
        if (tint.defaultColor == primaryColor) backgroundTintList = tintOf(customColor)
    }

    if (this is TextView && currentTextColor == primaryColor) {
        setTextColor(customColor)
    }

    if (this is ViewGroup) {
        for (index in 0 until childCount) {
            getChildAt(index).tintThemeColor(customColor, primaryColor)
        }
    }
}

private fun tintOf(color: Int) =
    android.content.res.ColorStateList.valueOf(color)

/** True when the user mixed their own colour and it should replace the primary one. */
fun Context.isCustomThemeColor(): Boolean =
    (applicationContext as? Application)
        ?.preferenceDataSource
        ?.getThemeColor(this) == com.minecraftmc22.expenses.common.presentation.ThemeColor.CUSTOM

/** The primary colour the current theme resolves to. */
fun Activity.resolvePrimaryColor(): Int {
    val value = android.util.TypedValue()

    return if (theme.resolveAttribute(R.attr.themeColor, value, true) && value.data != 0) {
        value.data
    } else {
        ContextCompat.getColor(this, R.color.expenses_blue)
    }
}
