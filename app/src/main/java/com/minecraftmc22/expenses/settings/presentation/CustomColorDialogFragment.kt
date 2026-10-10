package com.minecraftmc22.expenses.settings.presentation

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.util.blueOf
import com.minecraftmc22.expenses.util.colorOf
import com.minecraftmc22.expenses.util.greenOf
import com.minecraftmc22.expenses.util.redOf
import com.minecraftmc22.expenses.util.resolvePrimaryColor
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage

/**
 * Mixes an accent colour out of its three channels, showing the result while it is being mixed.
 *
 * The view is inflated with the fragment's own layout inflater, which carries the activity theme;
 * the texts come from the language aware context, which does not.
 */
class CustomColorDialogFragment : DialogFragment() {

    var onColorMixed: ((Int) -> Unit)? = null

    private lateinit var preview: View
    private lateinit var redChannel: TextView
    private lateinit var greenChannel: TextView
    private lateinit var blueChannel: TextView
    private lateinit var redBar: SeekBar
    private lateinit var greenBar: SeekBar
    private lateinit var blueBar: SeekBar

    /** Language aware context for the texts, resolved once when the dialog is built. */
    private lateinit var localizedContext: Context

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "Building the colour mixer.")

        // Resolved once: doing it per slider frame would create a context on every drag step.
        localizedContext = requireContext().withSelectedLanguage()

        val view = layoutInflater.inflate(R.layout.dialog_custom_color, null)

        bindWidgets(view)

        // Zero means nothing has been mixed yet, so start from the colour in use.
        val start = arguments?.getInt(ARGUMENT_COLOR)?.takeIf { it != 0 }
            ?: requireActivity().resolvePrimaryColor()

        redBar.progress = redOf(start)
        greenBar.progress = greenOf(start)
        blueBar.progress = blueOf(start)

        val listener = ChannelListener()
        redBar.setOnSeekBarChangeListener(listener)
        greenBar.setOnSeekBarChangeListener(listener)
        blueBar.setOnSeekBarChangeListener(listener)

        updatePreview()

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(localizedContext.getString(R.string.custom_color_title))
            .setView(view)
            .setPositiveButton(localizedContext.getString(R.string.ok)) { _, _ ->
                val color = mixedColor()

                // Closed before the change is applied: applying restarts the app, and the
                // automatic dismissal that follows would then be refused after the state was
                // saved, which takes the app down.
                closeSafely()

                onColorMixed?.invoke(color)
            }
            .setNegativeButton(localizedContext.getString(R.string.cancel)) { _, _ -> }
            .create()
    }

    private fun closeSafely() {
        try {
            dismiss()
        } catch (error: IllegalStateException) {
            Log.w(TAG, "The mixer was already going away.", error)
        }
    }

    private fun bindWidgets(view: View) {
        preview = view.findViewById(R.id.customColorPreview)
        redChannel = view.findViewById(R.id.textRedChannel)
        greenChannel = view.findViewById(R.id.textGreenChannel)
        blueChannel = view.findViewById(R.id.textBlueChannel)
        redBar = view.findViewById(R.id.seekBarRed)
        greenBar = view.findViewById(R.id.seekBarGreen)
        blueBar = view.findViewById(R.id.seekBarBlue)
    }

    private fun mixedColor() = colorOf(redBar.progress, greenBar.progress, blueBar.progress)

    private fun updatePreview() {
        preview.setBackgroundColor(mixedColor())

        redChannel.text = channelLabel(R.string.color_red, redBar.progress)
        greenChannel.text = channelLabel(R.string.color_green, greenBar.progress)
        blueChannel.text = channelLabel(R.string.color_blue, blueBar.progress)
    }

    private fun channelLabel(labelResId: Int, value: Int) =
        "${localizedContext.getString(labelResId)}  $value"

    private inner class ChannelListener : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            updatePreview()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    companion object {
        const val TAG = "CustomColorDialogFragment"

        private const val ARGUMENT_COLOR =
            "com.minecraftmc22.expenses.ARGUMENT_CUSTOM_COLOR"

        fun newInstance(color: Int): CustomColorDialogFragment {
            val arguments = Bundle().apply { putInt(ARGUMENT_COLOR, color) }

            return CustomColorDialogFragment().apply { this.arguments = arguments }
        }
    }
}
