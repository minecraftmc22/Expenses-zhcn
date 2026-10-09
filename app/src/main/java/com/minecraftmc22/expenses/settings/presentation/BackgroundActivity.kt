package com.minecraftmc22.expenses.settings.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.SeekBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.common.presentation.BackgroundSettings
import com.minecraftmc22.expenses.common.presentation.BaseActivity
import com.minecraftmc22.expenses.util.BackgroundRenderer
import com.minecraftmc22.expenses.util.extensions.application
import java.io.File
import java.io.IOException

/**
 * Lets the user pick the picture drawn behind every screen and adjust it.
 *
 * The picture is copied into the app's private storage, so no storage permission and no
 * persistable URI grant is needed and the file cannot disappear behind the app's back.
 */
class BackgroundActivity : BaseActivity() {

    private lateinit var previewImageView: ImageView
    private lateinit var chooseImageButton: MaterialButton
    private lateinit var clearBackgroundButton: MaterialButton
    private lateinit var opacityLabelTextView: MaterialTextView
    private lateinit var blurLabelTextView: MaterialTextView
    private lateinit var brightnessLabelTextView: MaterialTextView
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var blurSeekBar: SeekBar
    private lateinit var brightnessSeekBar: SeekBar

    private var settings = BackgroundSettings.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background)

        bindWidgets()
        setupActionBar()
        setupListeners()

        settings = application.preferenceDataSource.getBackground(this)

        bindSettings()
        updatePreview()
    }

    private fun bindWidgets() {
        previewImageView = findViewById(R.id.previewImageView)
        chooseImageButton = findViewById(R.id.chooseImageButton)
        clearBackgroundButton = findViewById(R.id.clearBackgroundButton)
        opacityLabelTextView = findViewById(R.id.opacityLabelTextView)
        blurLabelTextView = findViewById(R.id.blurLabelTextView)
        brightnessLabelTextView = findViewById(R.id.brightnessLabelTextView)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)
        blurSeekBar = findViewById(R.id.blurSeekBar)
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
    }

    private fun setupActionBar() {
        supportActionBar?.setTitle(R.string.custom_background)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListeners() {
        chooseImageButton.setOnClickListener { chooseImage() }
        clearBackgroundButton.setOnClickListener { clearBackground() }

        opacitySeekBar.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    updateOpacity(progress)
                }
            }
        )

        blurSeekBar.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    updateBlur(progress)
                }
            }
        )

        brightnessSeekBar.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    updateBrightness(progress)
                }
            }
        )
    }

    private fun bindSettings() {
        opacitySeekBar.progress = settings.opacity
        blurSeekBar.progress = settings.blur
        brightnessSeekBar.progress = settings.brightness

        updateLabels()
    }

    private fun updateLabels() {
        opacityLabelTextView.text = getString(R.string.background_opacity, settings.opacity)
        blurLabelTextView.text = getString(R.string.background_blur, settings.blur)
        brightnessLabelTextView.text = getString(R.string.background_brightness, settings.brightness)
    }

    // Adjustments

    private fun updateOpacity(progress: Int) {
        settings = settings.withOpacity(progress)
        persistAndPreview()
    }

    private fun updateBlur(progress: Int) {
        settings = settings.withBlur(progress)
        persistAndPreview()
    }

    private fun updateBrightness(progress: Int) {
        settings = settings.withBrightness(progress)
        persistAndPreview()
    }

    private fun updatePreview() {
        updateLabels()

        val bitmap = BackgroundRenderer.render(
            settings,
            PREVIEW_WIDTH,
            PREVIEW_HEIGHT
        )

        previewImageView.setImageBitmap(bitmap)
        previewImageView.imageAlpha = settings.alpha
        clearBackgroundButton.isEnabled = settings.isSet
    }

    private fun persistAndPreview() {
        application.preferenceDataSource.setBackground(this, settings)
        updatePreview()
    }

    // Picking the picture

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = IMAGE_MIME_TYPE
        }

        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    private fun clearBackground() {
        settings = settings.withImagePath(null)
        deleteStoredImage()
        persistAndPreview()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CODE_PICK_IMAGE) return
        if (resultCode != Activity.RESULT_OK) return

        val uri: Uri = data?.data ?: return
        val storedPath = storeImage(uri) ?: return

        settings = settings.withImagePath(storedPath)
        persistAndPreview()
    }

    /** Copies the picked picture into the app's own storage and returns its path. */
    private fun storeImage(uri: Uri): String? {
        return try {
            val target = File(filesDir, IMAGE_FILE_NAME)

            contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            target.absolutePath
        } catch (error: IOException) {
            Log.w(TAG, "Failed to store the picked background picture.", error)
            null
        } catch (error: SecurityException) {
            Log.w(TAG, "No permission to read the picked background picture.", error)
            null
        }
    }

    private fun deleteStoredImage() {
        val target = File(filesDir, IMAGE_FILE_NAME)
        if (target.exists() && !target.delete()) {
            Log.w(TAG, "Failed to delete the stored background picture.")
        }
    }

    // Lifecycle end

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> backSelected()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun backSelected(): Boolean {
        onBackPressed()
        return true
    }

    /** Only the progress callback matters here, the touch callbacks are unused. */
    private abstract class SimpleSeekBarListener : SeekBar.OnSeekBarChangeListener {

        override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

        override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
    }

    companion object {

        private const val TAG = "BackgroundActivity"

        private const val REQUEST_CODE_PICK_IMAGE = 1

        private const val IMAGE_MIME_TYPE = "image/*"

        private const val IMAGE_FILE_NAME = "background_image"

        /** Preview is rendered at the size it is shown at, scaled for high density screens. */
        private const val PREVIEW_WIDTH = 720
        private const val PREVIEW_HEIGHT = 360

        fun start(context: Context) {
            context.startActivity(Intent(context, BackgroundActivity::class.java))
        }
    }
}
