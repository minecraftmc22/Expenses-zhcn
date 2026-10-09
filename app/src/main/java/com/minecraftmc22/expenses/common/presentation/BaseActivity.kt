package com.minecraftmc22.expenses.common.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.findNavController
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.util.BackgroundRenderer
import com.minecraftmc22.expenses.util.extensions.application
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    companion object {
        const val ANIMATION_DEFAULT = 0
        const val ANIMATION_SLIDE_FROM_RIGHT = 1
        const val ANIMATION_SLIDE_FROM_BOTTOM = 2
    }

    protected open var animationKind = ANIMATION_DEFAULT

    /** Picture owned by the current screen, null to use the one from settings. */
    private var screenBackgroundPath: String? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withSelectedLanguage())
    }

    override fun onStart() {
        super.onStart()
        applyBackground()
    }

    /**
     * Screens that own a picture of their own — the expense detail screen — call this to replace
     * the global background. Passing null falls back to the global one.
     */
    fun setScreenBackground(path: String?) {
        if (screenBackgroundPath == path) return

        screenBackgroundPath = path
        applyBackground()
    }

    /**
     * Draws the chosen picture behind everything. The layouts of this app have no opaque
     * background of their own on purpose, so the window background stays visible; when no
     * picture is set it falls back to the one from the theme, which looks unchanged.
     */
    private fun applyBackground() {
        val fallback = themeWindowBackground()
        val settings = application.preferenceDataSource.getBackground(this)
        val effective = settings.withImagePath(screenBackgroundPath ?: settings.imagePath)

        if (!effective.isSet) {
            window.setBackgroundDrawable(fallback)
            return
        }

        val metrics = resources.displayMetrics
        val bitmap = BackgroundRenderer.render(
            effective, metrics.widthPixels, metrics.heightPixels
        )

        if (bitmap == null) {
            window.setBackgroundDrawable(fallback)
            return
        }

        val drawable = BitmapDrawable(resources, bitmap).apply {
            gravity = Gravity.FILL
            alpha = effective.alpha
        }

        // The picture has to sit on top of the theme colour: a semi transparent window
        // background would otherwise be drawn over the blank window and darken everything.
        window.setBackgroundDrawable(
            LayerDrawable(
                arrayOf<Drawable>(fallback ?: ColorDrawable(Color.TRANSPARENT), drawable)
            )
        )
    }

    private fun themeWindowBackground(): Drawable? {
        val value = TypedValue()

        if (!theme.resolveAttribute(android.R.attr.windowBackground, value, true)) return null

        return if (value.resourceId != 0) {
            AppCompatResources.getDrawable(this, value.resourceId)
        } else {
            ColorDrawable(value.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingEnterTransition()
    }

    private fun overridePendingEnterTransition() {
        when (animationKind) {
            ANIMATION_SLIDE_FROM_RIGHT ->
                overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)
            ANIMATION_SLIDE_FROM_BOTTOM ->
                overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingExitTransition()
    }

    private fun overridePendingExitTransition() {
        when (animationKind) {
            ANIMATION_SLIDE_FROM_RIGHT ->
                overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right)
            ANIMATION_SLIDE_FROM_BOTTOM ->
                overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_bottom)
        }
    }

    override fun onSupportNavigateUp() =
        findNavController(R.id.fragment_navigation_host).navigateUp()
}