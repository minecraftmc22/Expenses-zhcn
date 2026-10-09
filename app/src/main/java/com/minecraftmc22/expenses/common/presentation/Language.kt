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
 *
 * [NEKO_ZHCN] is Chinese written in a catgirl voice. It lives in `values-zh-rXX`
 * because `XX` is the ISO 3166 code for an unassigned region: no real device
 * locale can ever collide with it, and the language stays `zh` so the folder is
 * still treated as Chinese.
 */
enum class Language(val languageTag: String?) {

    SYSTEM_DEFAULT(null),
    SIMPLIFIED_CHINESE("zh-CN"),
    TRADITIONAL_CHINESE("zh-TW"),
    NEKO_ZHCN("zh-XX"),
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
        NEKO_ZHCN -> R.string.language_neko_zhcn
        ENGLISH -> R.string.language_english
        SPANISH -> R.string.language_spanish
        POLISH -> R.string.language_polish
    }

    fun toDisplayName(context: Context): String = context.getString(toLabelResId())
}
