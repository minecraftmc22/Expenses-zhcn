package com.minecraftmc22.expenses.settings.presentation

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.common.presentation.ThemeColor
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage

/**
 * Lets the user pick the accent of the app. Every row shows the colour itself, so the choice is
 * visible before it is applied.
 */
class ThemeColorSelectionDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    var onThemeColorSelected: ((ThemeColor) -> Unit)? = null

    /** Raised when the user wants to mix a colour instead of taking one of the presets. */
    var onCustomColorRequested: (() -> Unit)? = null

    private val themeColors by lazy { ThemeColor.values() }

    private lateinit var currentThemeColor: ThemeColor

    private var selectedThemeColor: ThemeColor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentThemeColor = arguments?.getString(ARGUMENT_CURRENT_THEME_COLOR)
            ?.let { ThemeColor.valueOf(it) } ?: ThemeColor.DEFAULT
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The dialog is built on the activity context, which carries the Material theme; only
        // the texts come from the language aware context, which is a theme-less wrapper.
        val localized = requireContext().withSelectedLanguage()

        selectedThemeColor = currentThemeColor

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(localized.getString(R.string.select_theme_color))
            .setSingleChoiceItems(
                ThemeColorAdapter(localized),
                themeColors.indexOf(currentThemeColor),
                this
            )
            .setNegativeButton(localized.getString(R.string.cancel)) { _, _ -> }
            .create()
    }

    /**
     * A tap applies the colour straight away. There is nothing else to confirm, and waiting for a
     * button made a tap look like it did nothing at all.
     */
    override fun onClick(dialog: DialogInterface, which: Int) {
        val themeColor = themeColors[which]

        Log.d(TAG, "Theme colour picked: ${themeColor.name}")

        // Closed before anything is applied: applying restarts the app, and closing performs a
        // fragment transaction that would be refused once the instance state has been saved.
        closeSafely()

        if (themeColor == ThemeColor.CUSTOM) {
            onCustomColorRequested?.invoke()
            return
        }

        selectedThemeColor = themeColor

        onThemeColorSelected?.invoke(themeColor)
    }

    private fun closeSafely() {
        try {
            dismiss()
        } catch (error: IllegalStateException) {
            Log.w(TAG, "The picker was already going away.", error)
        }
    }

    private inner class ThemeColorAdapter(private val context: Context) : BaseAdapter() {

        override fun getCount() = themeColors.size

        override fun getItem(position: Int): Any = themeColors[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // Rows are inflated with the context of the list they end up in — the one created by
            // the Material dialog, which carries the theme. The language aware context handed to
            // this adapter is a theme-less wrapper, and inflating with it makes every `?attr/...`
            // lookup of item_theme_color fail, which takes the dialog down before it can show.
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_theme_color, parent, false)

            val themeColor = themeColors[position]

            view.findViewById<View>(R.id.colorSwatch).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(swatchColorOf(themeColor, context))
            }

            view.findViewById<TextView>(R.id.colorName).text = themeColor.toDisplayName(context)

            view.findViewById<TextView>(R.id.colorCheck).visibility =
                if (themeColor == selectedThemeColor) View.VISIBLE else View.INVISIBLE

            return view
        }

        /** The mixed colour lives in the preferences, so the row shows what was mixed. */
        private fun swatchColorOf(themeColor: ThemeColor, context: Context): Int {
            if (themeColor != ThemeColor.CUSTOM) return themeColor.toColor(context)

            val stored = (context.applicationContext as Application)
                .preferenceDataSource
                .getCustomThemeColor(context)

            return if (stored != 0) stored else themeColor.toColor(context)
        }
    }

    companion object {
        const val TAG = "ThemeColorSelectionDialogFragment"

        private const val ARGUMENT_CURRENT_THEME_COLOR =
            "com.minecraftmc22.expenses.ARGUMENT_CURRENT_THEME_COLOR"

        fun newInstance(currentThemeColor: ThemeColor): ThemeColorSelectionDialogFragment {
            val arguments = Bundle().apply {
                putString(ARGUMENT_CURRENT_THEME_COLOR, currentThemeColor.name)
            }

            return ThemeColorSelectionDialogFragment().apply { this.arguments = arguments }
        }
    }
}
