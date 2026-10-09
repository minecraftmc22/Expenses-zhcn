package com.minecraftmc22.expenses.onboarding

import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthException
import com.minecraftmc22.expenses.Application
import com.minecraftmc22.expenses.R
import com.minecraftmc22.expenses.authentication.AuthenticationManager
import com.minecraftmc22.expenses.data.preference.PreferenceDataSource
import com.minecraftmc22.expenses.util.extensions.plusAssign
import com.minecraftmc22.expenses.util.reactive.DataEvent
import com.minecraftmc22.expenses.util.reactive.Event
import com.minecraftmc22.expenses.util.reactive.Variable
import io.reactivex.disposables.CompositeDisposable

class OnboardingFragmentModel(
    application: Application,
    private val authenticationManager: AuthenticationManager,
    private val preferenceDataSource: PreferenceDataSource
) : AndroidViewModel(application) {

    val isLoading = Variable(false)

    val requestGoogleSignIn = DataEvent<Intent>()
    val navigateToHome = Event()
    val showGoogleSignInError = DataEvent<String>()

    private val disposables = CompositeDisposable()

    fun continueWithoutSigningInRequested() {
        finishOnboardingAndNavigateHome()
    }

    fun continueWithGoogleRequested() {
        val request = authenticationManager.getGoogleSignInRequest()

        if (request == null) {
            showGoogleSignInError.next(
                getApplication<Application>().getString(R.string.google_sign_in_not_configured)
            )
        } else {
            requestGoogleSignIn.next(request)
        }
    }

    fun handleGoogleSignInResult(result: Intent) {
        isLoading.value = true

        disposables += authenticationManager.handleGoogleSignInResult(result)
            .subscribe({
                Log.d(TAG, "Succeeded to sign in with Google.")

                isLoading.value = false

                DataMigrationWorker.enqueue(getApplication())
                finishOnboardingAndNavigateHome()
            }, { error ->
                isLoading.value = false

                Log.w(TAG, "Failed to sign in with Google.", error)

                val message = getApplication<Application>().getString(
                    R.string.google_sign_in_failed,
                    describeSignInError(error)
                )

                showGoogleSignInError.next(message)
            })
    }

    /**
     * Turns the failure into the short code a developer can look up, because the
     * message of [ApiException] is often empty and the code is the only useful part.
     */
    private fun describeSignInError(error: Throwable): String {
        val code = when (error) {
            is ApiException -> error.statusCode.toString()
            is FirebaseAuthException -> error.errorCode.orEmpty()
            else -> ""
        }

        return if (code.isNotEmpty()) code else error.localizedMessage ?: error.javaClass.simpleName
    }

    private fun finishOnboardingAndNavigateHome() {
        preferenceDataSource.setIsUserOnboarded(getApplication(), true)
        navigateToHome.next()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return OnboardingFragmentModel(
                application,
                application.authenticationManager,
                application.preferenceDataSource
            ) as T
        }
    }

    companion object {
        private const val TAG = "OnboardingFragmentModel"
    }
}