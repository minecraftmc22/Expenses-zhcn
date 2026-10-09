package com.minecraftmc22.expenses

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import com.minecraftmc22.expenses.authentication.AuthenticationManager
import com.minecraftmc22.expenses.configuration.Configuration
import com.minecraftmc22.expenses.configuration.FirebaseConfiguration
import com.minecraftmc22.expenses.data.attachment.AttachmentStore
import com.minecraftmc22.expenses.data.backup.BackupManager
import com.minecraftmc22.expenses.data.firebase.FirebaseDataStore
import com.minecraftmc22.expenses.data.webdav.SyncManager
import com.minecraftmc22.expenses.data.webdav.WebDavClient
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.data.room.ApplicationDatabase
import com.minecraftmc22.expenses.data.room.RoomDataStore
import com.minecraftmc22.expenses.data.store.DataStore
import com.minecraftmc22.expenses.util.extensions.applyLanguageToResources
import com.minecraftmc22.expenses.util.extensions.withLanguage

class Application : android.app.Application() {

    /* Singletons - should be replaced with some dependency injection tool. */

    val authenticationManager: AuthenticationManager by lazy {
        AuthenticationManager(this, FirebaseAuth.getInstance())
    }

    val defaultDataStore: DataStore
        get() {
            return if (authenticationManager.isUserSignedIn()) {
                cloudDataStore
            } else {
                localDataStore
            }
        }

    private val database: ApplicationDatabase by lazy {
        ApplicationDatabase.build(this)
    }

    val localDataStore: DataStore by lazy {
        RoomDataStore(
            database.expenseDao(),
            database.tagDao(),
            database.expenseTagJoinDao()
        )
    }

    /** Attachments and their files stay on this device, they never go to Firestore. */
    val attachmentStore: AttachmentStore by lazy {
        AttachmentStore(this, database.attachmentDao())
    }

    /**
     * Backs up whichever store is active, so a signed in user exports the cloud data and an
     * offline user the local one. Recreated per access on purpose.
     */
    val backupManager: BackupManager
        get() = BackupManager(this, defaultDataStore, preferenceDataSource)

    /** Recreated per access so it always uses the WebDAV settings currently stored. */
    val syncManager: SyncManager
        get() = SyncManager(
            defaultDataStore,
            backupManager,
            WebDavClient(
                preferenceDataSource.getWebDavUrl(this),
                preferenceDataSource.getWebDavUserName(this),
                preferenceDataSource.getWebDavPassword(this)
            )
        )

    val cloudDataStore: DataStore by lazy {
        FirebaseDataStore(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
    }

    val preferenceDataSource: PreferenceDataSource by lazy {
        PreferenceDataSource()
    }

    val configuration: Configuration by lazy {
        FirebaseConfiguration(FirebaseRemoteConfig.getInstance())
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withLanguage(preferenceDataSource.getLanguage(base)))
    }

    /**
     * The process survives a language change, so [attachBaseContext] is not called again.
     * Re-applying the configuration to the resources of this context makes the strings it
     * owns — the settings list, the about screen, everything the view models read from
     * `getApplication()` — follow the new language immediately. The activities are
     * recreated separately by the restart.
     */
    fun refreshLanguage() {
        applyLanguageToResources(preferenceDataSource.getLanguage(this))
    }

    override fun onCreate() {
        super.onCreate()
        initializeThreeTeen()
        enqueueConfigurationSync()
        applyTheme()
        cleanUpAttachmentFiles()
    }

    /**
     * Drops attachment files that no row refers to any more, for example after the user picked
     * something and then left the screen without saving.
     */
    private fun cleanUpAttachmentFiles() {
        attachmentStore.deleteOrphanFiles()
            .subscribe({}, { error ->
                Log.w("Application", "Failed to clean up attachment files.", error)
            })
    }

    private fun initializeThreeTeen() {
        AndroidThreeTen.init(this)
    }

    private fun enqueueConfigurationSync() {
        configuration.enqueueSync()
    }

    private fun applyTheme() {
        val theme = preferenceDataSource.getTheme(applicationContext)
        AppCompatDelegate.setDefaultNightMode(theme.toNightMode())
    }
}
