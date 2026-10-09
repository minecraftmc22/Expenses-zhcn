package com.minecraftmc22.expenses.util.extensions

import android.app.Activity
import com.minecraftmc22.expenses.Application

/**
 * Repaints this activity with the accent picked in settings.
 *
 * The theme has to be overlaid before the first view exists, so activities call this at the very
 * beginning of onCreate, before super.onCreate — otherwise the action bar and the already
 * inflated views would keep the old accent.
 */
fun Activity.applyThemeColor() {
    val themeColor = (applicationContext as Application).preferenceDataSource.getThemeColor(this)

    theme.applyStyle(themeColor.overlayResId, true)
}
