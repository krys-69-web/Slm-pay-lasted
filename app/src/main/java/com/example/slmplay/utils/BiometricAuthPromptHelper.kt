package com.example.slmplay.utils

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthPromptHelper {

    /**
     * Checks if biometric (Face Unlock, Fingerprint) OR device credential (PIN/Pattern/Password)
     * is available on this Android device.
     */
    fun isBiometricOrDeviceCredentialAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)

            // Check Strong Biometrics (3D Face, Fingerprint) + Device Credential (Phone Lock)
            val resStrongWithCred = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (resStrongWithCred == BiometricManager.BIOMETRIC_SUCCESS) return true

            // Check Weak Biometrics (2D Camera Face Unlock present on most Android phones)
            val resWeak = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (resWeak == BiometricManager.BIOMETRIC_SUCCESS) return true

            // Check Device Credential alone (Phone PIN, Pattern or Password)
            val resCred = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            resCred == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Backward-compatible alias for existing callers.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return isBiometricOrDeviceCredentialAvailable(context)
    }

    /**
     * Shows the official Android biometric prompt (Face Lock, Fingerprint, or Phone Screen Lock).
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Déverrouillage Sécurisé",
        subtitle: String = "Reconnaissance faciale (Face Lock), Empreinte ou Sécurité Android",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Visage ou empreinte non reconnu(e). Réessayez.")
            }
        }

        // Try primary system authenticators with Device Credential first
        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription("Authentification avec la sécurité du mobile")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
            return
        } catch (e: Exception) {
            // Fallback for devices supporting BIOMETRIC_WEAK (2D Face Unlock) requiring a negative button
            try {
                val fallbackPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle("Reconnaissance faciale (Face Lock) ou Empreinte")
                    .setNegativeButtonText("Utiliser le Code PIN")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                    .build()

                val biometricPrompt = BiometricPrompt(activity, executor, callback)
                biometricPrompt.authenticate(fallbackPromptInfo)
            } catch (ex2: Exception) {
                onError(ex2.localizedMessage ?: "Authentification système non disponible")
            }
        }
    }
}
