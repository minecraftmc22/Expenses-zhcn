package com.minecraftmc22.expenses.settings.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.common.presentation.Language
import com.minecraftmc22.expenses.common.presentation.Theme
import com.minecraftmc22.expenses.currencyselection.CurrencySelectionActivity
import com.minecraftmc22.expenses.data.model.Currency
import com.minecraftmc22.expenses.data.webdav.SyncSummary
import com.minecraftmc22.expenses.onboarding.OnboardingActivity
import com.minecraftmc22.expenses.splash.SplashActivity
import com.minecraftmc22.expenses.util.extensions.application
import com.minecraftmc22.expenses.util.extensions.plusAssign
import com.minecraftmc22.expenses.util.extensions.startActivitySafely
import io.reactivex.disposables.CompositeDisposable

class SettingsFragment : Fragment() {
    private val compositeDisposable = CompositeDisposable()

    private lateinit var containerLayout: ViewGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SettingsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var model: SettingsFragmentModel

    // Lifecycle start

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindWidgets(view)
        setupActionBar()
        setupRecyclerView()
        setupModel()
        bindModel()
    }

    private fun bindWidgets(view: View) {
        containerLayout = view.findViewById(R.id.layout_container)
        recyclerView = view.findViewById(R.id.recycler_view)
    }

    private fun setupActionBar() {
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar ?: return
        actionBar.setTitle(R.string.settings)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp)
        setHasOptionsMenu(true)
    }

    private fun setupRecyclerView() {
        adapter = SettingsAdapter()
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
    }

    private fun setupModel() {
        val factory = SettingsFragmentModel.Factory(requireContext().application)
        model = ViewModelProviders.of(this, factory).get(SettingsFragmentModel::class.java)
    }

    private fun bindModel() {
        compositeDisposable += model.itemModels.subscribe(adapter::submitList)
        compositeDisposable += model.selectDefaultCurrency.subscribe(::selectDefaultCurrency)
        compositeDisposable += model.navigateToOnboarding.subscribe(::navigateToOnboarding)
        compositeDisposable += model.showMessage.subscribe(::showMessage)
        compositeDisposable += model.showActivity.subscribe(::showActivity)
        compositeDisposable += model.showThemeSelectionDialog.subscribe(::showThemeSelectionDialog)
        compositeDisposable += model.applyTheme.subscribe(::applyTheme)
        compositeDisposable += model.showLanguageSelectionDialog.subscribe(::showLanguageSelectionDialog)
        compositeDisposable += model.restartApplication.subscribe(::restartApplication)
        compositeDisposable += model.navigateToBackground.subscribe(::navigateToBackground)
        compositeDisposable += model.exportRequested.subscribe(::exportBackup)
        compositeDisposable += model.showImportConfirmation.subscribe(::confirmImport)
        compositeDisposable += model.showWebDavSettings.subscribe(::showWebDavSettings)
        compositeDisposable += model.syncNowRequested.subscribe { model.syncNow() }
        compositeDisposable += model.showSyncResult.subscribe(::showSyncResult)
    }

    private fun selectDefaultCurrency() {
        CurrencySelectionActivity.start(this, REQUEST_CODE_SELECT_DEFAULT_CURRENCY)
    }

    private fun showMessage(messageId: Int) {
        Snackbar.make(containerLayout, messageId, Snackbar.LENGTH_LONG).show()
    }

    private fun showActivity(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        requireActivity().startActivitySafely(intent)
    }

    private fun showThemeSelectionDialog(currentTheme: Theme) {
        val dialogFragment = ThemeSelectionDialogFragment.newInstance(currentTheme)
        dialogFragment.onThemeSelected = { model.themeSelected(it) }
        dialogFragment.show(requireFragmentManager(), ThemeSelectionDialogFragment.TAG)
    }

    private fun applyTheme(theme: Theme) {
        // Naively add short delay to make sure that all animations are finished.
        Handler().postDelayed({
            AppCompatDelegate.setDefaultNightMode(theme.toNightMode())
        }, NIGHT_MODE_APPLICATION_DELAY)
    }

    private fun navigateToOnboarding() {
        OnboardingActivity.start(requireContext())
        requireActivity().finishAffinity()
    }

    private fun showLanguageSelectionDialog(currentLanguage: Language) {
        val dialogFragment = LanguageSelectionDialogFragment.newInstance(currentLanguage)
        dialogFragment.onLanguageSelected = { model.languageSelected(it) }
        dialogFragment.show(requireFragmentManager(), LanguageSelectionDialogFragment.TAG)
    }

    /**
     * A new language only takes effect once the activities are created again,
     * so the whole task is restarted on top of the splash activity.
     */
    private fun restartApplication() {
        val intent = Intent(requireContext(), SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        requireActivity().startActivitySafely(intent)
    }

    private fun navigateToBackground() {
        BackgroundActivity.start(requireContext())
    }

    // Backup

    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = JSON_MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, DEFAULT_BACKUP_FILE_NAME)
        }

        startActivityForResult(intent, REQUEST_CODE_EXPORT_BACKUP)
    }

    private fun confirmImport() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.import_configuration_confirmation)
            .setPositiveButton(R.string.yes) { _, _ -> pickBackupToImport() }
            .setNegativeButton(R.string.no) { _, _ -> }
            .show()
    }

    private fun pickBackupToImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = JSON_MIME_TYPE
        }

        startActivityForResult(intent, REQUEST_CODE_IMPORT_BACKUP)
    }

    // WebDAV

    private fun showWebDavSettings() {
        val view = layoutInflater.inflate(R.layout.dialog_webdav, null)
        val urlField = view.findViewById<EditText>(R.id.editTextWebDavUrl)
        val userNameField = view.findViewById<EditText>(R.id.editTextWebDavUser)
        val passwordField = view.findViewById<EditText>(R.id.editTextWebDavPassword)

        urlField.setText(model.webDavUrl())
        userNameField.setText(model.webDavUserName())
        passwordField.setText(model.webDavPassword())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.webdav_settings)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                model.saveWebDavSettings(
                    urlField.text.toString().trim(),
                    userNameField.text.toString().trim(),
                    passwordField.text.toString()
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSyncResult(summary: SyncSummary) {
        val message = getString(
            R.string.webdav_sync_success, summary.added, summary.updated, summary.total
        )

        Snackbar.make(containerLayout, message, Snackbar.LENGTH_LONG).show()
    }

    // Lifecycle end

    override fun onDestroyView() {
        super.onDestroyView()
        unbindModel()
    }

    private fun unbindModel() {
        compositeDisposable.clear()
    }

    // Options

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> backSelected()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun backSelected(): Boolean {
        requireActivity().onBackPressed()
        return true
    }

    // Results

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_CODE_SELECT_DEFAULT_CURRENCY -> {
                val currency: Currency? =
                    data?.getParcelableExtra(CurrencySelectionActivity.EXTRA_CURRENCY)
                currency?.let { model.defaultCurrencySelected(it) }
            }
            REQUEST_CODE_EXPORT_BACKUP -> {
                data?.data?.let { model.exportTo(it) }
            }
            REQUEST_CODE_IMPORT_BACKUP -> {
                data?.data?.let { model.importFrom(it) }
            }
        }
    }

    companion object {

        private const val REQUEST_CODE_SELECT_DEFAULT_CURRENCY = 1
        private const val REQUEST_CODE_EXPORT_BACKUP = 2
        private const val REQUEST_CODE_IMPORT_BACKUP = 3
        private const val NIGHT_MODE_APPLICATION_DELAY = 500L

        private const val JSON_MIME_TYPE = "application/json"
        private const val DEFAULT_BACKUP_FILE_NAME = "expenses-backup.json"
    }
}