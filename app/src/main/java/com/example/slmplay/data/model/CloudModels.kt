package com.example.slmplay.data.model

import java.util.UUID

data class CloudAccount(
    val cloudUserId: String = UUID.randomUUID().toString(),
    val username: String = "Utilisateur SLM",
    val email: String = "",
    val avatarUri: String? = null,
    val cloudSessionToken: String = UUID.randomUUID().toString(),
    val refreshToken: String = UUID.randomUUID().toString(),
    val isVerified: Boolean = true,
    val cloudPlan: String = "SLM Cloud Pro 5Go",
    val authProvider: String = "LOCAL", // LOCAL, GOOGLE_OAUTH, APPLE_ID
    val usedStorageBytes: Long = 204800L, // approx 200 KB
    val totalQuotaBytes: Long = 5368709120L, // 5 GB
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

data class CloudUserServerRecord(
    val cloudUserId: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val avatarUri: String? = null,
    val authProvider: String = "LOCAL",
    val oauthIdToken: String? = null,
    val isDeleted: Boolean = false,
    val deletionTimestamp: Long? = null,
    val cloudPlan: String = "SLM Cloud Pro 5Go",
    val usedStorageBytes: Long = 204800L,
    val totalQuotaBytes: Long = 5368709120L,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)

data class AuthSession(
    val sessionToken: String,
    val refreshToken: String,
    val userId: String,
    val deviceId: String,
    val issuedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L) // 30 days
)

sealed class AuthResult {
    data class Success(val account: CloudAccount, val session: AuthSession, val message: String) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
}

data class CloudBackupSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val cloudUserId: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val playlistsCount: Int = 0,
    val tracksCount: Int = 0,
    val favoritesCount: Int = 0,
    val bookmarksCount: Int = 0,
    val snapshotSizeBytes: Long = 102400L,
    val jsonPayload: String = "{}"
)

enum class CloudConnectionStatus(val label: String, val colorHex: Long) {
    CONNECTED("Connecté au Cloud SLM ☁️", 0xFF34C759),
    SYNCING("Synchronisation en cours...", 0xFF007AFF),
    OFFLINE("Mode Hors-Ligne (Données locales protégées)", 0xFFFF9500),
    ERROR("Erreur de connexion Cloud", 0xFFFF3B30)
}

