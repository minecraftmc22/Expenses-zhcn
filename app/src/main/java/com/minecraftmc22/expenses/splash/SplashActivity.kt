package com.minecraftmc22.expenses.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.home.presentation.HomeActivity
import com.minecraftmc22.expenses.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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