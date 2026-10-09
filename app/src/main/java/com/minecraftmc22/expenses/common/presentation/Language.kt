package com.minecraftmc22.expenses.common.presentation

import android.content.Context
import androidx.annotation.StringRes
import com.minecraftmc22.expenses.R
import java.util.Locale

/**
 * Language the user picked in settings.
 *
 * [languageTag] is a BCP 47 tag whose language and region have to match the
 * corresponding `values-*` resource folder, for example `zh-CN` for
 * `values-zh-rCN`. [SYSTEM_DEFAULT] carries no tag and follows the device.
 */
enum class Language(val languageTag: String?) {

    SYSTEM_DEFAULT(null),
    SIMPLIFIED_CHINESE("zh-CN"),
    TRADITIONAL_CHINESE("zh-TW"),
    ENGLISH("en"),
    SPANISH("es"),
    POLISH("pl-PL");

    fun toLocale(): Locale? = languageTag?.let { Locale.forLanguageTag(it) }

    /**
     * Every language is listed in its own language, so these labels are the
     * same in every locale and therefore not translatable.
     */
    @StringRes
    fun toLabelResId(): Int = when (this) {
        SYSTEM_DEFAULT -> R.string.system_default
        SIMPLIFIED_CHINESE -> R.string.language_simplified_chinese
        TRADITIONAL_CHINESE -> R.string.language_traditional_chinese
        ENGLISH -> R.string.language_english
        SPANISH -> R.string.language_spanish
        POLISH -> R.string.language_polish
    }

    fun toDisplayName(context: Context): String = context.getString(toLabelResId())
}
