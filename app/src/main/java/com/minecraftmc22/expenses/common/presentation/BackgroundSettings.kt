package com.minecraftmc22.expenses.common.presentation

/**
 * Background picture together with the adjustments applied to it.
 *
 * [opacity] and [brightness] are percentages — 100 means "unchanged" — while [blur] is the
 * radius used by the box blur. [imagePath] points at a copy inside the app's private
 * storage, so it stays valid without any storage permission.
 */
data class BackgroundSettings(
    val imagePath: String?,
    val opacity: Int,
    val blur: Int,
    val brightness: Int
) {

    val isSet: Boolean get() = imagePath != null

    /** Alpha for drawing the picture, derived from [opacity]. */
    val alpha: Int get() = opacity.coerceIn(0, OPACITY_MAX) * 255 / OPACITY_MAX

    fun withImagePath(path: String?): BackgroundSettings = copy(imagePath = path)

    fun withOpacity(value: Int): BackgroundSettings =
        copy(opacity = value.coerceIn(0, OPACITY_MAX))

    fun withBlur(value: Int): BackgroundSettings =
        copy(blur = value.coerceIn(0, BLUR_MAX))

    fun withBrightness(value: Int): BackgroundSettings =
        copy(brightness = value.coerceIn(0, BRIGHTNESS_MAX))

    companion object {
        const val OPACITY_DEFAULT = 100
        const val OPACITY_MAX = 100

        const val BLUR_DEFAULT = 0
        const val BLUR_MAX = 25

        const val BRIGHTNESS_DEFAULT = 100
        const val BRIGHTNESS_MAX = 200

        val NONE = BackgroundSettings(
            imagePath = null,
            opacity = OPACITY_DEFAULT,
            blur = BLUR_DEFAULT,
            brightness = BRIGHTNESS_DEFAULT
        )
    }
}
