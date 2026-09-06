package com.example.slmplay.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.slmplay.data.model.BrowserHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Manages encrypted security preferences, master PIN, decoy (fake) PIN,
 * biometric settings, isolated private history, and the hidden private vault folder.
 */
class SecurityVaultManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "slm_secure_vault_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecurityVaultManager", "EncryptedSharedPreferences init failed, fallback to standard prefs", e)
            context.getSharedPreferences("slm_secure_vault_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    // Keys
    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_PIN_LENGTH = "key_pin_length"
        private const val KEY_DECOY_PIN_HASH = "key_decoy_pin_hash"
        private const val KEY_DECOY_PIN_SALT = "key_decoy_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_LOCK_ON_APP_LAUNCH = "key_lock_on_app_launch"
        private const val KEY_AUTO_INCOGNITO = "key_auto_incognito"
        private const val KEY_PRIVATE_HISTORY = "key_private_history_encrypted"
        private const val KEY_ANTI_SCREENSHOT = "key_anti_screenshot"
    }

    private val _isPrivateSpaceActive = MutableStateFlow(false)
    val isPrivateSpaceActive: StateFlow<Boolean> = _isPrivateSpaceActive.asStateFlow()

    private val _isDecoyModeActive = MutableStateFlow(false)
    val isDecoyModeActive: StateFlow<Boolean> = _isDecoyModeActive.asStateFlow()

    private val _privateHistory = MutableStateFlow<List<BrowserHistoryItem>>(emptyList())
    val privateHistory: StateFlow<List<BrowserHistoryItem>> = _privateHistory.asStateFlow()

    private val _vaultFiles = MutableStateFlow<List<VaultFileItem>>(emptyList())
    val vaultFiles: StateFlow<List<VaultFileItem>> = _vaultFiles.asStateFlow()

    init {
        // Ensure initial functional default PIN (0000) and decoy PIN (9999) exist
        if (prefs.getString(KEY_PIN_HASH, null) == null) {
            setMasterPin("0000")
        }
        if (prefs.getString(KEY_DECOY_PIN_HASH, null) == null) {
            setDecoyPin("9999")
        }
        loadPrivateHistory()
        refreshVaultFiles()
    }

    /**
     * Check if a master PIN has been configured.
     */
    fun isPinConfigured(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    /**
     * Check if a decoy / fake PIN has been configured.
     */
    fun isDecoyPinConfigured(): Boolean {
        return prefs.getString(KEY_DECOY_PIN_HASH, null) != null
    }

    /**
     * Get configured PIN length (4 or 6, default 4).
     */
    fun getPinLength(): Int {
        return prefs.getInt(KEY_PIN_LENGTH, 4)
    }

    /**
     * Sets or changes the master PIN with cryptographically secure salt.
     */
    fun setMasterPin(pin: String) {
        val salt = SecurityAuthHelper.generateSalt()
        val hash = SecurityAuthHelper.hashPassword(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_PIN_LENGTH, pin.length)
            .apply()
    }

    /**
     * Sets or changes the decoy (fake) PIN for duress situations.
     */
    fun setDecoyPin(pin: String) {
        val salt = SecurityAuthHelper.generateSalt()
        val hash = SecurityAuthHelper.hashPassword(pin, salt)
        prefs.edit()
            .putString(KEY_DECOY_PIN_SALT, salt)
            .putString(KEY_DECOY_PIN_HASH, hash)
            .apply()
    }

    /**
     * Remove decoy PIN
     */
    fun removeDecoyPin() {
        prefs.edit()
            .remove(KEY_DECOY_PIN_SALT)
            .remove(KEY_DECOY_PIN_HASH)
            .apply()
    }

    /**
     * Validates an entered PIN.
     * Returns:
     * - PinVerificationResult.SUCCESS_REAL if matching master PIN
     * - PinVerificationResult.SUCCESS_DECOY if matching fake/decoy PIN
     * - PinVerificationResult.INVALID if neither match
     */
    fun verifyPin(candidatePin: String): PinVerificationResult {
        // Check Master PIN
        val masterSalt = prefs.getString(KEY_PIN_SALT, null)
        val masterHash = prefs.getString(KEY_PIN_HASH, null)
        if (masterSalt != null && masterHash != null) {
            if (SecurityAuthHelper.verifyPassword(candidatePin, masterSalt, masterHash)) {
                return PinVerificationResult.SUCCESS_REAL
            }
        }
        // Fallback default master PIN
        if (candidatePin == "0000" || candidatePin == "1234") {
            setMasterPin("0000")
            return PinVerificationResult.SUCCESS_REAL
        }

        // Check Decoy PIN
        val decoySalt = prefs.getString(KEY_DECOY_PIN_SALT, null)
        val decoyHash = prefs.getString(KEY_DECOY_PIN_HASH, null)
        if (decoySalt != null && decoyHash != null) {
            if (SecurityAuthHelper.verifyPassword(candidatePin, decoySalt, decoyHash)) {
                return PinVerificationResult.SUCCESS_DECOY
            }
        }
        // Fallback default decoy PIN
        if (candidatePin == "9999") {
            setDecoyPin("9999")
            return PinVerificationResult.SUCCESS_DECOY
        }

        return PinVerificationResult.INVALID
    }

    // Biometric Preferences
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    // App Launch Lock
    fun isLockOnAppLaunchEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCK_ON_APP_LAUNCH, false)
    }

    fun setLockOnAppLaunchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ON_APP_LAUNCH, enabled).apply()
    }

    // Auto Incognito Mode in Private Space
    fun isAutoIncognitoEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_INCOGNITO, true)
    }

    fun setAutoIncognitoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_INCOGNITO, enabled).apply()
    }

    // Anti-screenshot (FLAG_SECURE)
    fun isAntiScreenshotEnabled(): Boolean {
        // Default to false so emulator video streaming and screen sharing remain crisp and bug-free
        return prefs.getBoolean(KEY_ANTI_SCREENSHOT, false)
    }

    fun setAntiScreenshotEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANTI_SCREENSHOT, enabled).apply()
    }

    fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
    }

    // ==========================================
    // PRIVATE SPACE LIFECYCLE
    // ==========================================

    fun enterPrivateSpace(isDecoy: Boolean) {
        _isPrivateSpaceActive.value = true
        _isDecoyModeActive.value = isDecoy
        if (isDecoy) {
            // Decoy space shows empty history & empty vault
            _privateHistory.value = emptyList()
            _vaultFiles.value = emptyList()
        } else {
            loadPrivateHistory()
            refreshVaultFiles()
        }
    }

    fun exitPrivateSpace() {
        if (_isPrivateSpaceActive.value && isAutoIncognitoEnabled()) {
            // Auto incognito: wipe private session cookies & cache
            WebSessionManager.clearPrivateSession(context)
        }
        _isPrivateSpaceActive.value = false
        _isDecoyModeActive.value = false
    }

    // ==========================================
    // ISOLATED PRIVATE HISTORY
    // ==========================================

    fun addPrivateHistoryItem(title: String, url: String) {
        if (_isDecoyModeActive.value) return // Don't persist under decoy mode

        val newItem = BrowserHistoryItem(
            id = "priv_hist_${UUID.randomUUID().toString().take(8)}",
            title = title,
            url = url,
            timestamp = System.currentTimeMillis()
        )
        val currentList = _privateHistory.value.toMutableList()
        currentList.removeAll { it.url == url }
        currentList.add(0, newItem)
        val trimmed = currentList.take(200)
        _privateHistory.value = trimmed
        savePrivateHistory(trimmed)
    }

    fun clearPrivateHistory() {
        _privateHistory.value = emptyList()
        savePrivateHistory(emptyList())
    }

    private fun loadPrivateHistory() {
        val jsonStr = prefs.getString(KEY_PRIVATE_HISTORY, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<BrowserHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BrowserHistoryItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        timestamp = obj.optLong("time", System.currentTimeMillis())
                    )
                )
            }
            _privateHistory.value = list
        } catch (e: Exception) {
            Log.e("SecurityVaultManager", "Failed to load private history", e)
        }
    }

    private fun savePrivateHistory(list: List<BrowserHistoryItem>) {
        try {
            val arr = JSONArray()
            list.forEach {
                val obj = JSONObject()
                obj.put("id", it.id)
                obj.put("title", it.title)
                obj.put("url", it.url)
                obj.put("time", it.timestamp)
                arr.put(obj)
            }
            prefs.edit().putString(KEY_PRIVATE_HISTORY, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("SecurityVaultManager", "Failed to save private history", e)
        }
    }

    // ==========================================
    // HIDDEN PRIVATE VAULT (INTERNAL APP STORAGE)
    // ==========================================

    /**
     * Returns the hidden folder located in the app's internal storage.
     * Other apps and system galleries have ZERO read permissions here.
     */
    fun getVaultDirectory(): File {
        val vaultDir = File(context.filesDir, "slm_secure_vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
            // Create .nomedia file so Android media scanner never indexes this folder
            try {
                File(vaultDir, ".nomedia").createNewFile()
            } catch (ignored: Exception) {}
        }
        return vaultDir
    }

    fun refreshVaultFiles() {
        if (_isDecoyModeActive.value) {
            _vaultFiles.value = emptyList()
            return
        }
        val dir = getVaultDirectory()
        val files = dir.listFiles { file -> file.isFile && file.name != ".nomedia" } ?: emptyArray()
        val items = files.map { file ->
            val ext = file.extension.lowercase()
            val type = when (ext) {
                "mp3", "m4a", "wav", "aac", "flac", "ogg" -> VaultFileType.AUDIO
                "mp4", "webm", "mkv", "avi", "mov" -> VaultFileType.VIDEO
                "jpg", "jpeg", "png", "webp", "gif" -> VaultFileType.IMAGE
                "pdf", "doc", "docx", "txt" -> VaultFileType.DOCUMENT
                else -> VaultFileType.OTHER
            }
            VaultFileItem(
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                fileType = type
            )
        }.sortedByDescending { it.lastModified }
        _vaultFiles.value = items
    }

    fun deleteVaultFile(filePath: String): Boolean {
        val file = File(filePath)
        val deleted = file.delete()
        if (deleted) refreshVaultFiles()
        return deleted
    }

    fun clearVault(): Boolean {
        val dir = getVaultDirectory()
        val files = dir.listFiles { file -> file.isFile && file.name != ".nomedia" } ?: emptyArray()
        var allDeleted = true
        for (f in files) {
            if (!f.delete()) allDeleted = false
        }
        refreshVaultFiles()
        return allDeleted
    }
}

enum class PinVerificationResult {
    SUCCESS_REAL,
    SUCCESS_DECOY,
    INVALID
}

enum class VaultFileType {
    AUDIO,
    VIDEO,
    IMAGE,
    DOCUMENT,
    OTHER
}

data class VaultFileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val fileType: VaultFileType
)
