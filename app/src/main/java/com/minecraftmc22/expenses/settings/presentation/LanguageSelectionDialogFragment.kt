package com.minecraftmc22.expenses.settings.presentation

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.common.presentation.Language
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage

class LanguageSelectionDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    var onLanguageSelected: ((Language) -> Unit)? = null

    private val languages by lazy { Language.values() }

    private lateinit var currentLanguage: Language

    private var selectedLanguage: Language? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentLanguage = arguments?.getString(ARGUMENT_CURRENT_LANGUAGE)
            ?.let { Language.valueOf(it) } ?: Language.SYSTEM_DEFAULT
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Built on a context forced to the selected language, so the title and the
        // system-default entry cannot fall back to a stale locale.
        val context = requireContext().withSelectedLanguage()

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(getItems(context), getCheckedItem(), this)
            .setPositiveButton(R.string.ok) { _, _ ->
                selectedLanguage?.let { onLanguageSelected?.invoke(it) }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .create()
    }

    private fun getItems(context: Context): Array<String> {
        return languages
            .map { it.toDisplayName(context) }
            .toTypedArray()
    }

    private fun getCheckedItem(): Int = languages.indexOf(currentLanguage)

    override fun onClick(dialog: DialogInterface, which: Int) {
        selectedLanguage = languages[which]
    }

    companion object {
        const val TAG = "LanguageSelectionDialogFragment"

        private const val ARGUMENT_CURRENT_LANGUAGE =
            "com.minecraftmc22.expenses.ARGUMENT_CURRENT_LANGUAGE"

        fun newInstance(currentLanguage: Language): LanguageSelectionDialogFragment {
            val arguments = Bundle().apply {
                putString(ARGUMENT_CURRENT_LANGUAGE, currentLanguage.name)
            }

            return LanguageSelectionDialogFragment().apply { this.arguments = arguments }
        }
    }
}
