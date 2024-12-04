package com.example.raptor

import android.content.Context
import android.widget.Button
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

enum class BiometricAuthenticationStatus(val id: Int) {
    READY(1),
    NOT_AVAILABLE(-1),
    TEMPORARY_NOT_AVAILABLE(-2),
    AVAILABLE_BUT_NOT_ENROLLED(-3)
}

class BiometricAuthenticator(
    private val context: Context
) {
    private lateinit var promptinfo: BiometricPrompt.PromptInfo
    private val biometricmanager = BiometricManager.from(context)
    private lateinit var biometricPrompt: BiometricPrompt

    fun isBiometricAuthAvailable(): BiometricAuthenticationStatus {
        return when(biometricmanager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAuthenticationStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAuthenticationStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAuthenticationStatus.TEMPORARY_NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAuthenticationStatus.AVAILABLE_BUT_NOT_ENROLLED
            else -> BiometricAuthenticationStatus.NOT_AVAILABLE
        }
    }
    fun PromptBiometricAuth(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        fragmentActivity: FragmentActivity,
        onSuccess:(result: BiometricPrompt.AuthenticationResult) -> Unit,
        onFailed:()-> Unit,
        onError:(errorCode: Int, errorString: String) -> Unit,
    ) {
        when(isBiometricAuthAvailable()) {
            BiometricAuthenticationStatus.NOT_AVAILABLE -> {
                onError(BiometricAuthenticationStatus.NOT_AVAILABLE.id,"Not available on this device")
                return
            }
            BiometricAuthenticationStatus.TEMPORARY_NOT_AVAILABLE -> {
                onError(BiometricAuthenticationStatus.TEMPORARY_NOT_AVAILABLE.id, "Not available at this moment")
                return
            }
            BiometricAuthenticationStatus.AVAILABLE_BUT_NOT_ENROLLED -> {
                onError(BiometricAuthenticationStatus.AVAILABLE_BUT_NOT_ENROLLED.id, "Add a fingerprint")
                return
            }
            else -> Unit
        }
        biometricPrompt = BiometricPrompt(
            fragmentActivity,
            object: BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )
        promptinfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()
        biometricPrompt.authenticate(promptinfo)
    }
}