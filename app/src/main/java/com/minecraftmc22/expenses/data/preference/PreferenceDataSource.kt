package com.minecraftmc22.expenses.data.preference

import android.content.Context
import androidx.preference.PreferenceManager
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.common.presentation.BackgroundSettings
import com.minecraftmc22.expenses.common.presentation.Language
import com.minecraftmc22.expenses.common.presentation.Theme
import com.minecraftmc22.expenses.data.model.Currency
import com.minecraftmc22.expenses.home.presentation.DateRange

class PreferenceDataSource {

    fun getDefaultCurrency(context: Context): Currency {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getDefaultCurrencyKey(context)
        return preferences.getString(key, null)?.let { Currency.valueOf(it) } ?: Currency.USD
    }

    fun setDefaultCurrency(context: Context, defaultCurrency: Currency) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getDefaultCurrencyKey(context)
        preferences.edit().putString(key, defaultCurrency.name).apply()
    }

    fun getDateRange(context: Context): DateRange {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getDateRangeKey(context)
        return preferences.getString(key, null)?.let { DateRange.valueOf(it) } ?: DateRange.ALL_TIME
    }

    fun setDateRange(context: Context, dateRange: DateRange) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getDateRangeKey(context)
        preferences.edit().putString(key, dateRange.name).apply()
    }

    fun getIsUserOnboarded(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getIsUserOnboardedKey(context)
        return preferences.getBoolean(key, false)
    }

    fun setIsUserOnboarded(context: Context, isUserOnboarded: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getIsUserOnboardedKey(context)
        preferences.edit().putBoolean(key, isUserOnboarded).apply()
    }

    fun getTheme(context: Context): Theme {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getThemeKey(context)
        return preferences.getString(key, null)?.let { Theme.valueOf(it) } ?: Theme.SYSTEM_DEFAULT
    }

    fun setTheme(context: Context, theme: Theme) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getThemeKey(context)
        preferences.edit().putString(key, theme.name).apply()
    }

    fun getLanguage(context: Context): Language {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getLanguageKey(context)
        return preferences.getString(key, null)?.let { Language.valueOf(it) }
            ?: Language.SYSTEM_DEFAULT
    }

    fun setLanguage(context: Context, language: Language) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = getLanguageKey(context)
        preferences.edit().putString(key, language.name).apply()
    }

    fun getBackground(context: Context): BackgroundSettings {        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        return BackgroundSettings(
            imagePath = preferences.getString(getBackgroundImageKey(context), null),
            opacity = preferences.getInt(
                getBackgroundOpacityKey(context), BackgroundSettings.OPACITY_DEFAULT
            ),
            blur = preferences.getInt(
                getBackgroundBlurKey(context), BackgroundSettings.BLUR_DEFAULT
            ),
            brightness = preferences.getInt(
                getBackgroundBrightnessKey(context), BackgroundSettings.BRIGHTNESS_DEFAULT
            )
        )
    }

    fun setBackground(context: Context, background: BackgroundSettings) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit()
            .putString(getBackgroundImageKey(context), background.imagePath)
            .putInt(getBackgroundOpacityKey(context), background.opacity)
            .putInt(getBackgroundBlurKey(context), background.blur)
            .putInt(getBackgroundBrightnessKey(context), background.brightness)
            .apply()
    }

    private fun getDefaultCurrencyKey(context: Context) =
        context.getString(R.string.key_default_currency)

    private fun getDateRangeKey(context: Context) =
        context.getString(R.string.key_date_range)

    private fun getIsUserOnboardedKey(context: Context) =
        context.getString(R.string.key_is_user_onboarded)

    private fun getThemeKey(context: Context) =
        context.getString(R.string.key_theme)

    private fun getLanguageKey(context: Context) =
        context.getString(R.string.key_language)

    private fun getBackgroundImageKey(context: Context) =
        context.getString(R.string.key_background_image)

    private fun getBackgroundOpacityKey(context: Context) =
        context.getString(R.string.key_background_opacity)

    private fun getBackgroundBlurKey(context: Context) =
        context.getString(R.string.key_background_blur)

    private fun getBackgroundBrightnessKey(context: Context) =
        context.getString(R.string.key_background_brightness)

    // WebDAV

    fun getWebDavUrl(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(getWebDavUrlKey(context), "").orEmpty()

    fun setWebDavUrl(context: Context, url: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(getWebDavUrlKey(context), url).apply()
    }

    fun getWebDavUserName(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(getWebDavUserNameKey(context), "").orEmpty()

    fun setWebDavUserName(context: Context, userName: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(getWebDavUserNameKey(context), userName).apply()
    }

    fun getWebDavPassword(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(getWebDavPasswordKey(context), "").orEmpty()

    fun setWebDavPassword(context: Context, password: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(getWebDavPasswordKey(context), password).apply()
    }

    private fun getWebDavUrlKey(context: Context) =
        context.getString(R.string.key_webdav_url)

    private fun getWebDavUserNameKey(context: Context) =
        context.getString(R.string.key_webdav_user)

    private fun getWebDavPasswordKey(context: Context) =
        context.getString(R.string.key_webdav_password)
}