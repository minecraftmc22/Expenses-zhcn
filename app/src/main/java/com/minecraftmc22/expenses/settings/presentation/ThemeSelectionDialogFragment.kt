package com.minecraftmc22.expenses.settings.presentation

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.common.presentation.Theme
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage

class ThemeSelectionDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    var onThemeSelected: ((Theme) -> Unit)? = null

    private val themes by lazy { Theme.values() }

    private lateinit var currentTheme: Theme

    private var selectedTheme: Theme? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentTheme = arguments?.getString(ARGUMENT_CURRENT_THEME)
            ?.let { Theme.valueOf(it) } ?: Theme.SYSTEM_DEFAULT
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Same split as the language dialog: the dialog itself is built on the activity context,
        // because that is the one carrying the Material theme, while the texts come from the
        // language aware context, so they cannot fall back to a stale locale.
        val localized = requireContext().withSelectedLanguage()

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(localized.getString(R.string.select_theme))
            .setSingleChoiceItems(getItems(localized), getCheckedItem(), this)
            .setPositiveButton(localized.getString(R.string.ok)) { _, _ ->
                selectedTheme?.let { onThemeSelected?.invoke(it) }
            }
            .setNegativeButton(localized.getString(R.string.cancel)) { _, _ -> }
            .create()
    }

    private fun getItems(context: Context): Array<String> {
        return themes.map { theme ->
            when (theme) {
                Theme.LIGHT -> context.getString(R.string.light)
                Theme.DARK -> context.getString(R.string.dark)
                Theme.SYSTEM_DEFAULT -> context.getString(R.string.system_default)
            }
        }.toTypedArray()
    }

    private fun getCheckedItem(): Int = themes.indexOf(currentTheme)

    override fun onClick(dialog: DialogInterface, which: Int) {
        selectedTheme = themes[which]
    }

    companion object {
        const val TAG = "ThemeSelectionDialogFragment"

        private const val ARGUMENT_CURRENT_THEME =
            "com.minecraftmc22.expenses.ARGUMENT_CURRENT_THEME"

        fun newInstance(currentTheme: Theme): ThemeSelectionDialogFragment {
            val arguments = Bundle().apply {
                putString(ARGUMENT_CURRENT_THEME, currentTheme.name)
            }

            return ThemeSelectionDialogFragment().apply { this.arguments = arguments }
        }
    }
}