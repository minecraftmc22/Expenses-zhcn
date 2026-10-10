package com.minecraftmc22.expenses.settings.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.BuildConfig
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.authentication.AuthenticationManager
import com.minecraftmc22.expenses.common.presentation.Language
import com.minecraftmc22.expenses.common.presentation.Theme
import com.minecraftmc22.expenses.common.presentation.ThemeColor
import com.minecraftmc22.expenses.data.model.Currency
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.data.webdav.SyncSummary
import com.minecraftmc22.expenses.util.extensions.plusAssign
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage
import com.minecraftmc22.expenses.util.reactive.DataEvent
import com.minecraftmc22.expenses.util.reactive.Event
import com.minecraftmc22.expenses.util.reactive.Variable
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.io
import java.io.IOException

class SettingsFragmentModel(
    application: Application,
    private val preferenceDataSource: PreferenceDataSource,
    private val authenticationManager: AuthenticationManager
) : AndroidViewModel(application) {
    val itemModels = Variable(emptyList<SettingItemModel>())
    val selectDefaultCurrency = Event()
    val navigateToOnboarding = Event()
    val showMessage = DataEvent<Int>()
    val showActivity = DataEvent<Uri>()
    val showThemeSelectionDialog = DataEvent<Theme>()
    val applyTheme = DataEvent<Theme>()
    val showLanguageSelectionDialog = DataEvent<Language>()
    val restartApplication = Event()
    val navigateToBackground = Event()
    val exportRequested = Event()
    val showImportConfirmation = Event()
    val showWebDavSettings = Event()
    val syncNowRequested = Event()
    val showSyncResult = DataEvent<SyncSummary>()
    val showThemeColorSelectionDialog = DataEvent<ThemeColor>()

    private val disposables = CompositeDisposable()

    // Lifecycle start

    init {
        loadItemModels()
    }

    /**
     * The list is built from strings of the application context, whose resources keep the locale
     * they had when the process started. This context is localized to the chosen language on
     * demand, so the list still follows a language change when the platform refused to
     * re-localize the application resources.
     */
    private val localizedContext: Context
        get() = getApplication<Application>().withSelectedLanguage()

    private fun loadItemModels() {
        itemModels.value =
            createAccountSection() + createThemeSection() + createApplicationSection() +
                createBackupSection() + createWebDavSection() + createPrivacySection()
    }

    // Theme section

    private fun createThemeSection(): List<SettingItemModel> {
        val context = localizedContext

        val itemModels = mutableListOf<SettingItemModel>()
        itemModels += SettingsHeaderModel(context.getString(R.string.theme))
        itemModels += createDarkMode(context)
        itemModels += createThemeColor(context)

        return itemModels
    }

    private fun createThemeColor(context: Context): SettingItemModel {
        val title = context.getString(R.string.theme_color)
        val themeColor = preferenceDataSource.getThemeColor(context)

        return SummaryActionSettingItemModel(title, themeColor.toDisplayName(context)).apply {
            click = { showThemeColorSelectionDialog.next(themeColor) }
        }
    }

    // Account section

    private fun createAccountSection(): List<SettingItemModel> {
        val context = localizedContext

        val itemModels = mutableListOf<SettingItemModel>()
        itemModels += createAccountHeader(context)
        itemModels += if (authenticationManager.isUserSignedIn()) {
            createSignOut(context)
        } else {
            signUpOrSignIn(context)
        }

        return itemModels
    }

    private fun createAccountHeader(context: Context): SettingItemModel =
        SettingsHeaderModel(context.getString(R.string.account))

    private fun createSignOut(context: Context): SettingItemModel {
        val title = context.getString(R.string.sign_out)

        return ActionSettingItemModel(title).apply {
            click = {
                authenticationManager.signOut()
                preferenceDataSource.setIsUserOnboarded(getApplication(), false)
                navigateToOnboarding.next()
            }
        }
    }

    private fun signUpOrSignIn(context: Context): SettingItemModel {
        val title = context.getString(R.string.sign_up_or_sign_in)

        return ActionSettingItemModel(title).apply {
            click = {
                preferenceDataSource.setIsUserOnboarded(getApplication(), false)
                navigateToOnboarding.next()
            }
        }
    }

    // Application section

    private fun createApplicationSection(): List<SettingItemModel> {
        val context = localizedContext

        val itemModels = mutableListOf<SettingItemModel>()
        itemModels += createApplicationHeader(context)
        itemModels += createDefaultCurrency(context)
        itemModels += createLanguage(context)
        itemModels += createBackground(context)

        return itemModels
    }

    private fun createApplicationHeader(context: Context): SettingItemModel =
        SettingsHeaderModel(context.getString(R.string.application))

    private fun createDefaultCurrency(context: Context): SettingItemModel {
        val defaultCurrency = preferenceDataSource.getDefaultCurrency(context)

        val title = context.getString(R.string.default_currency)
        val summary = context.getString(
            R.string.default_currency_summary,
            defaultCurrency.flag,
            defaultCurrency.title,
            defaultCurrency.code
        )

        return SummaryActionSettingItemModel(title, summary).apply {
            click = { selectDefaultCurrency.next() }
        }
    }

    private fun createDarkMode(context: Context): SettingItemModel {
        val title = context.getString(R.string.theme)

        val darkMode = preferenceDataSource.getTheme(context)

        val summary = when (darkMode) {
            Theme.LIGHT -> context.getString(R.string.light)
            Theme.DARK -> context.getString(R.string.dark)
            Theme.SYSTEM_DEFAULT -> context.getString(R.string.system_default)
        }

        return SummaryActionSettingItemModel(title, summary).apply {
            click = { showThemeSelectionDialog.next(darkMode) }
        }
    }

    private fun createLanguage(context: Context): SettingItemModel {
        val title = context.getString(R.string.language)

        val language = preferenceDataSource.getLanguage(context)

        return SummaryActionSettingItemModel(title, language.toDisplayName(context)).apply {
            click = { showLanguageSelectionDialog.next(language) }
        }
    }

    private fun createBackground(context: Context): SettingItemModel {
        val title = context.getString(R.string.custom_background)

        val summary = if (preferenceDataSource.getBackground(context).isSet) {
            context.getString(R.string.custom_background_summary)
        } else {
            context.getString(R.string.not_set)
        }

        return SummaryActionSettingItemModel(title, summary).apply {
            click = { navigateToBackground.next() }
        }
    }

    // Backup section

    private fun createBackupSection(): List<SettingItemModel> {
        val context = localizedContext

        val itemModels = mutableListOf<SettingItemModel>()
        itemModels += SettingsHeaderModel(context.getString(R.string.backup_and_restore))
        itemModels += createExportConfiguration(context)
        itemModels += createImportConfiguration(context)

        return itemModels
    }

    private fun createExportConfiguration(context: Context): SettingItemModel {
        val title = context.getString(R.string.export_configuration)

        return ActionSettingItemModel(title).apply {
            click = { exportRequested.next() }
        }
    }

    private fun createImportConfiguration(context: Context): SettingItemModel {
        val title = context.getString(R.string.import_configuration)

        return ActionSettingItemModel(title).apply {
            click = { showImportConfirmation.next() }
        }
    }

    // WebDAV section

    private fun createWebDavSection(): List<SettingItemModel> {
        val context = localizedContext

        val itemModels = mutableListOf<SettingItemModel>()
        itemModels += SettingsHeaderModel(context.getString(R.string.webdav_sync))
        itemModels += createWebDavSettings(context)
        itemModels += createSyncNow(context)

        return itemModels
    }

    private fun createWebDavSettings(context: Context): SettingItemModel {
        val title = context.getString(R.string.webdav_settings)
        val url = preferenceDataSource.getWebDavUrl(context)

        val summary = if (url.isEmpty()) context.getString(R.string.not_set) else url

        return SummaryActionSettingItemModel(title, summary).apply {
            click = { showWebDavSettings.next() }
        }
    }

    private fun createSyncNow(context: Context): SettingItemModel {
        val title = context.getString(R.string.webdav_sync_now)

        return ActionSettingItemModel(title).apply {
            click = { syncNowRequested.next() }
        }
    }

    fun webDavUrl() = preferenceDataSource.getWebDavUrl(getApplication())

    fun webDavUserName() = preferenceDataSource.getWebDavUserName(getApplication())

    fun webDavPassword() = preferenceDataSource.getWebDavPassword(getApplication())

    fun saveWebDavSettings(url: String, userName: String, password: String) {
        val application = getApplication<Application>()

        preferenceDataSource.setWebDavUrl(application, url)
        preferenceDataSource.setWebDavUserName(application, userName)
        preferenceDataSource.setWebDavPassword(application, password)

        loadItemModels()
    }

    /** Merges with the file on the server: newer wins, records missing on either side travel. */
    fun syncNow() {
        val application = getApplication<Application>()

        if (preferenceDataSource.getWebDavUrl(application).isEmpty()) {
            showMessage.next(R.string.webdav_not_configured)
            return
        }

        showMessage.next(R.string.webdav_syncing)

        disposables += application.syncManager.sync()
            .observeOn(mainThread())
            .subscribe({ summary ->
                showSyncResult.next(summary)
            }, { error ->
                Log.w(TAG, "WebDAV sync failed.", error)
                showMessage.next(R.string.webdav_sync_failure)
            })
    }

    // About section

    private fun createPrivacySection(): List<SettingItemModel> {
        val context = localizedContext

        val itemModels = mutableListOf<SettingItemModel>()
        itemModels += createPrivacyHeader(context)
        itemModels += createPrivacyPolicy(context)

        return itemModels
    }

    private fun createPrivacyHeader(context: Context): SettingItemModel =
        SettingsHeaderModel(context.getString(R.string.privacy))

    private fun createPrivacyPolicy(context: Context): SettingItemModel {
        val title = context.getString(R.string.privacy_policy)

        return ActionSettingItemModel(title).apply {
            click = { showActivity.next(PRIVACY_POLICY_URI) }
        }
    }
    // Lifecycle end

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    // Public

    fun defaultCurrencySelected(defaultCurrency: Currency) {
        getApplication<Application>().let {
            preferenceDataSource.setDefaultCurrency(it, defaultCurrency)
        }

        loadItemModels()
    }

    fun themeSelected(theme: Theme) {
        getApplication<Application>().let {
            preferenceDataSource.setTheme(it, theme)
        }

        loadItemModels()

        applyTheme.next(theme)
    }

    fun themeColorSelected(themeColor: ThemeColor) {
        Log.d(TAG, "Theme colour selected: ${themeColor.name}")

        preferenceDataSource.setThemeColor(getApplication(), themeColor)

        loadItemModels()

        // Every activity overlays its theme at the start of onCreate, so they all have to be
        // created again for the accent to reach the action bars and the inflated views.
        restartApplication.next()
    }

    /** Colour mixed by the user, zero while nothing has been mixed yet. */
    fun customThemeColor() = preferenceDataSource.getCustomThemeColor(getApplication())

    fun customThemeColorSelected(color: Int) {
        val application = getApplication<Application>()

        Log.d(TAG, "Custom theme colour selected.")

        preferenceDataSource.setCustomThemeColor(application, color)
        preferenceDataSource.setThemeColor(application, ThemeColor.CUSTOM)

        loadItemModels()

        restartApplication.next()
    }

    fun languageSelected(language: Language) {
        val application = getApplication<Application>()

        preferenceDataSource.setLanguage(application, language)
        // The process is not recreated by the restart below, so the strings owned by
        // the application context have to be re-localized explicitly.
        application.refreshLanguage()

        loadItemModels()

        restartApplication.next()
    }

    // Backup

    fun exportTo(uri: Uri) {
        val application = getApplication<Application>()

        disposables += application.backupManager.export()
            .flatMapCompletable { json -> writeBackup(application, uri, json) }
            .observeOn(mainThread())
            .subscribe({
                showMessage.next(R.string.backup_export_success)
            }, { error ->
                Log.w(TAG, "Failed to export the backup.", error)
                showMessage.next(R.string.backup_failure)
            })
    }

    fun importFrom(uri: Uri) {
        val application = getApplication<Application>()

        disposables += readBackup(application, uri)
            .flatMapCompletable { json -> application.backupManager.import(json) }
            .observeOn(mainThread())
            .subscribe({
                showMessage.next(R.string.backup_import_success)
                // The imported settings, the language above all, need a fresh start.
                restartApplication.next()
            }, { error ->
                Log.w(TAG, "Failed to import the backup.", error)
                showMessage.next(R.string.backup_failure)
            })
    }

    private fun writeBackup(application: Application, uri: Uri, json: String): Completable {
        return Completable.fromAction {
            application.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("Cannot open the destination document.")
        }.subscribeOn(io())
    }

    private fun readBackup(application: Application, uri: Uri): Single<String> {
        return Single.fromCallable {
            application.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Cannot open the backup document.")
        }.subscribeOn(io())
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SettingsFragmentModel(
                application,
                application.preferenceDataSource,
                application.authenticationManager
            ) as T
        }
    }

    companion object {
        private const val TAG = "SettingsFragmentModel"

        private val PRIVACY_POLICY_URI =
            Uri.parse("https://raw.githubusercontent.com/minecraftmc22/expenses-zhcn/master/resources/privacy_policy.md")
    }
}