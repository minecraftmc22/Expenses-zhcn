package com.minecraftmc22.expenses.util.extensions

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.common.presentation.Language
import java.util.Locale

val Context.application: Application
    get() = applicationContext as Application

/**
 * Language picked in settings, or [Language.SYSTEM_DEFAULT] while the
 * [Application] instance is not reachable yet.
 */
val Context.selectedLanguage: Language
    get() = (applicationContext as? Application)
        ?.preferenceDataSource
        ?.getLanguage(this)
        ?: Language.SYSTEM_DEFAULT

/**
 * Returns a context whose resources are localized to [language].
 *
 * [Language.SYSTEM_DEFAULT] resolves to the device locale, which is also what
 * makes switching back from a fixed language work inside a reused process.
 */
fun Context.withLanguage(language: Language): Context {
    val locale = language.toLocale() ?: systemLocale()
    Locale.setDefault(locale)

    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)

    return createConfigurationContext(configuration)
}

fun Context.withSelectedLanguage(): Context = withLanguage(selectedLanguage)

@Suppress("DEPRECATION")
private fun systemLocale(): Locale =
    Resources.getSystem().configuration.locale ?: Locale.getDefault()