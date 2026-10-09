package com.minecraftmc22.expenses.splash

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.home.presentation.HomeActivity
import com.minecraftmc22.expenses.onboarding.OnboardingActivity
import com.minecraftmc22.expenses.util.extensions.applyThemeColor
import com.minecraftmc22.expenses.util.extensions.withSelectedLanguage

class SplashActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withSelectedLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeColor()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (isUserOnboarded()) {
            HomeActivity.start(this)
        } else {
            OnboardingActivity.start(this)
        }

        finish()
    }

    private fun isUserOnboarded(): Boolean =
        (application as Application).preferenceDataSource.getIsUserOnboarded(applicationContext)
}