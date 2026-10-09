package com.minecraftmc22.expenses

import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import com.minecraftmc22.expenses.authentication.AuthenticationManager
import com.minecraftmc22.expenses.configuration.Configuration
import com.minecraftmc22.expenses.configuration.FirebaseConfiguration
import com.minecraftmc22.expenses.data.firebase.FirebaseDataStore
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.data.room.ApplicationDatabase
import com.minecraftmc22.expenses.data.room.RoomDataStore
import com.minecraftmc22.expenses.data.store.DataStore

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

    val localDataStore: DataStore by lazy {
        val database = ApplicationDatabase.build(this)

        RoomDataStore(
            database.expenseDao(),
            database.tagDao(),
            database.expenseTagJoinDao()
        )
    }

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

    override fun onCreate() {
        super.onCreate()
        initializeThreeTeen()
        enqueueConfigurationSync()
        applyTheme()
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
