package com.minecraftmc22.expenses.settings.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.common.presentation.BaseActivity
import com.minecraftmc22.expenses.common.presentation.ThemeColor
import com.minecraftmc22.expenses.splash.SplashActivity
import com.minecraftmc22.expenses.util.blueOf
import com.minecraftmc22.expenses.util.colorOf
import com.minecraftmc22.expenses.util.greenOf
import com.minecraftmc22.expenses.util.redOf
import com.minecraftmc22.expenses.util.resolvePrimaryColor
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage

/**
 * Mixes the accent colour out of its three channels.
 *
 * It is a screen rather than a dialog on purpose: the background screen uses exactly this shape
 * — three seek bars and a preview — and that is the pattern known to work on the devices this app
 * runs on. The colour is written when the user confirms, because applying it restarts the app.
 */
class CustomColorActivity : BaseActivity() {

    private lateinit var preview: View
    private lateinit var redLabel: TextView
    private lateinit var greenLabel: TextView
    private lateinit var blueLabel: TextView
    private lateinit var redBar: SeekBar
    private lateinit var greenBar: SeekBar
    private lateinit var blueBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_color)

        bindWidgets()
        setupActionBar()
        setupChannels()

        findViewById<MaterialButton>(R.id.buttonApplyColor).setOnClickListener { applyColor() }
    }

    private fun bindWidgets() {
        preview = findViewById(R.id.customColorPreview)
        redLabel = findViewById(R.id.textRedChannel)
        greenLabel = findViewById(R.id.textGreenChannel)
        blueLabel = findViewById(R.id.textBlueChannel)
        redBar = findViewById(R.id.seekBarRed)
        greenBar = findViewById(R.id.seekBarGreen)
        blueBar = findViewById(R.id.seekBarBlue)
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar ?: return
        actionBar.setTitle(R.string.custom_color_title)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp)
    }

    private fun setupChannels() {
        // Zero means nothing was mixed yet, so start from the accent in use.
        val start = app.preferenceDataSource.getCustomThemeColor(this).takeIf { it != 0 }
            ?: resolvePrimaryColor()

        redBar.progress = redOf(start)
        greenBar.progress = greenOf(start)
        blueBar.progress = blueOf(start)

        val listener = ChannelListener()
        redBar.setOnSeekBarChangeListener(listener)
        greenBar.setOnSeekBarChangeListener(listener)
        blueBar.setOnSeekBarChangeListener(listener)

        updatePreview()
    }

    private fun mixedColor() = colorOf(redBar.progress, greenBar.progress, blueBar.progress)

    private fun updatePreview() {
        preview.setBackgroundColor(mixedColor())

        val localized = withSelectedLanguage()

        redLabel.text = channelLabel(localized, R.string.color_red, redBar.progress)
        greenLabel.text = channelLabel(localized, R.string.color_green, greenBar.progress)
        blueLabel.text = channelLabel(localized, R.string.color_blue, blueBar.progress)
    }

    private fun channelLabel(localized: Context, labelResId: Int, value: Int) =
        "${localized.getString(labelResId)}  $value"

    private fun applyColor() {
        app.preferenceDataSource.setCustomThemeColor(this, mixedColor())
        app.preferenceDataSource.setThemeColor(this, ThemeColor.CUSTOM)

        // The accent is picked up when an activity is created, so the app starts over. The intent
        // is started directly: asking the package manager first can silently skip the start.
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private inner class ChannelListener : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            updatePreview()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, CustomColorActivity::class.java))
        }
    }
}
