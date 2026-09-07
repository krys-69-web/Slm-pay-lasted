package com.example.slmplay.data.repository

import android.content.Context
import android.net.Uri
import com.example.slmplay.data.db.MusicDao
import com.example.slmplay.data.db.PlaylistEntity
import com.example.slmplay.data.db.PlaylistTrackCrossRef
import com.example.slmplay.data.db.TrackEntity
import com.example.slmplay.data.model.*
import com.example.slmplay.utils.SecurityAuthHelper
import com.example.slmplay.utils.StoragePersistenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CloudSyncManager(
    private val context: Context,
    private val musicDao: MusicDao,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("slm_cloud_sync_prefs", Context.MODE_PRIVATE)
    private val securePrefs = context.getSharedPreferences("slm_secure_device_keystore", Context.MODE_PRIVATE)

    // Current active logged-in cloud account
    private val _cloudAccount = MutableStateFlow<CloudAccount?>(null)
    val cloudAccount = _cloudAccount.asStateFlow()

    // Active device session (JWT session token + refresh token)
    private val _activeSession = MutableStateFlow<AuthSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    // Saved accounts on this device
    private val _savedCloudAccounts = MutableStateFlow<List<CloudAccount>>(emptyList())
    val savedCloudAccounts = _savedCloudAccounts.asStateFlow()

    // Server-side user records (simulating the secure backend cloud database)
    private val _serverUsersDb = MutableStateFlow<List<CloudUserServerRecord>>(emptyList())
    val serverUsersDb = _serverUsersDb.asStateFlow()

    private val _cloudConnectionStatus = MutableStateFlow(CloudConnectionStatus.CONNECTED)
    val cloudConnectionStatus = _cloudConnectionStatus.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<CloudBackupSnapshot>>(emptyList())
    val cloudBackups = _cloudBackups.asStateFlow()

    private val _browserBookmarks = MutableStateFlow<List<BrowserBookmark>>(emptyList())
    val browserBookmarks = _browserBookmarks.asStateFlow()

    private val _browserHistory = MutableStateFlow<List<BrowserHistoryItem>>(emptyList())
    val browserHistory = _browserHistory.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(prefs.getBoolean("auto_sync_enabled", true))
    val isAutoSyncEnabled = _isAutoSyncEnabled.asStateFlow()

    private val _lastSyncTimeFormatted = MutableStateFlow("À l'instant")
    val lastSyncTimeFormatted = _lastSyncTimeFormatted.asStateFlow()

    // Device identification
    private val deviceId: String

    init {
        // Initialize or read unique device identifier
        var devId = securePrefs.getString("slm_device_id", null)
        if (devId == null) {
            devId = SecurityAuthHelper.generateDeviceId()
            securePrefs.edit().putString("slm_device_id", devId).apply()
        }
        deviceId = devId

        loadServerUsersDatabase()
        loadAccountsFromStorage()
        loadBookmarksFromStorage()
        loadHistoryFromStorage()
        loadBackupsFromStorage()
        attemptAutoLoginFromDeviceSession()
    }

    // ================= SECURE SERVER-SIDE DB EMULATION =================
    private fun loadServerUsersDatabase() {
        val serverJson = prefs.getString("server_users_db_json", null)
        val list = mutableListOf<CloudUserServerRecord>()

        if (serverJson != null) {
            try {
                val arr = JSONArray(serverJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CloudUserServerRecord(
                            cloudUserId = obj.optString("cloudUserId", UUID.randomUUID().toString()),
                            username = obj.optString("username", "Utilisateur"),
                            email = obj.optString("email", ""),
                            passwordHash = obj.optString("passwordHash", ""),
                            passwordSalt = obj.optString("passwordSalt", ""),
                            avatarUri = if (obj.has("avatarUri") && !obj.isNull("avatarUri")) obj.getString("avatarUri") else null,
                            authProvider = obj.optString("authProvider", "LOCAL"),
                            oauthIdToken = if (obj.has("oauthIdToken")) obj.getString("oauthIdToken") else null,
                            isDeleted = obj.optBoolean("isDeleted", false),
                            deletionTimestamp = if (obj.has("deletionTimestamp")) obj.getLong("deletionTimestamp") else null,
                            cloudPlan = obj.optString("cloudPlan", "SLM Cloud Pro 5Go"),
                            usedStorageBytes = obj.optLong("usedStorageBytes", 204800L),
                            totalQuotaBytes = obj.optLong("totalQuotaBytes", 5368709120L),
                            createdAtTimestamp = obj.optLong("createdAtTimestamp", System.currentTimeMillis()),
                            lastLoginTimestamp = obj.optLong("lastLoginTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (list.isEmpty()) {
            // Seed a secure primary demo user with real salted hash
            val salt = SecurityAuthHelper.generateSalt()
            val passHash = SecurityAuthHelper.hashPassword("slm12345", salt)
            val demoUser = CloudUserServerRecord(
                cloudUserId = "cloud_user_default",
                username = "Membre SLM Cloud",
                email = "cloud.user@slmplay.net",
                passwordHash = passHash,
                passwordSalt = salt,
                avatarUri = null,
                authProvider = "LOCAL",
                cloudPlan = "SLM Cloud Pro 5Go",
                usedStorageBytes = 204800L,
                totalQuotaBytes = 5368709120L,
                createdAtTimestamp = System.currentTimeMillis() - (7L * 24 * 3600 * 1000L)
            )
            list.add(demoUser)
            saveServerUsersDatabase(list)
        }

        _serverUsersDb.value = list
    }

    private fun saveServerUsersDatabase(list: List<CloudUserServerRecord>) {
        _serverUsersDb.value = list
        val arr = JSONArray()
        list.forEach { u ->
            val obj = JSONObject().apply {
                put("cloudUserId", u.cloudUserId)
                put("username", u.username)
                put("email", u.email)
                put("passwordHash", u.passwordHash)
                put("passwordSalt", u.passwordSalt)
                put("avatarUri", u.avatarUri)
                put("authProvider", u.authProvider)
                put("oauthIdToken", u.oauthIdToken)
                put("isDeleted", u.isDeleted)
                put("deletionTimestamp", u.deletionTimestamp)
                put("cloudPlan", u.cloudPlan)
                put("usedStorageBytes", u.usedStorageBytes)
                put("totalQuotaBytes", u.totalQuotaBytes)
                put("createdAtTimestamp", u.createdAtTimestamp)
                put("lastLoginTimestamp", u.lastLoginTimestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("server_users_db_json", arr.toString()).apply()
    }

    // ================= AUTHENTICATION LOGIC (INSCRIPTION / CONNEXION / SESSIONS) =================

    /**
     * Inscription (Register):
     * 1. Validates inputs
     * 2. Checks email/username uniqueness on server
     * 3. Hashes password securely with per-user salt (never stores plain text)
     * 4. Saves server record
     * 5. Issues JWT session token & refresh token for current device
     * 6. Automatically logs in and initializes sync
     */
    fun registerCloudAccount(
        username: String,
        email: String,
        passwordPlain: String,
        onResult: (AuthResult) -> Unit
    ) {
        val cleanName = username.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.length < 2) {
            onResult(AuthResult.Error("Le nom d'utilisateur doit contenir au moins 2 caractères."))
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            onResult(AuthResult.Error("Veuillez saisir une adresse e-mail valide."))
            return
        }
        if (passwordPlain.length < 6) {
            onResult(AuthResult.Error("Le mot de passe doit comporter au moins 6 caractères pour être sécurisé."))
            return
        }

        // Check uniqueness on Server DB
        val existing = _serverUsersDb.value.find {
            (!it.isDeleted) && (it.email.equals(cleanEmail, ignoreCase = true) || it.username.equals(cleanName, ignoreCase = true))
        }
        if (existing != null) {
            onResult(AuthResult.Error("Un compte avec cet email ou ce nom d'utilisateur existe déjà sur le serveur."))
            return
        }

        // Cryptographic hashing with Salt
        val salt = SecurityAuthHelper.generateSalt()
        val hash = SecurityAuthHelper.hashPassword(passwordPlain, salt)
        val newUserId = "cloud_usr_" + UUID.randomUUID().toString().take(8)

        val serverRecord = CloudUserServerRecord(
            cloudUserId = newUserId,
            username = cleanName,
            email = cleanEmail,
            passwordHash = hash,
            passwordSalt = salt,
            authProvider = "LOCAL",
            createdAtTimestamp = System.currentTimeMillis(),
            lastLoginTimestamp = System.currentTimeMillis()
        )

        val updatedDb = _serverUsersDb.value + serverRecord
        saveServerUsersDatabase(updatedDb)

        // Generate Device Tokens
        val sessionToken = SecurityAuthHelper.generateSessionToken(newUserId, deviceId)
        val refreshToken = SecurityAuthHelper.generateRefreshToken(newUserId)
        val session = AuthSession(sessionToken, refreshToken, newUserId, deviceId)
        saveActiveDeviceSession(session)

        val account = CloudAccount(
            cloudUserId = newUserId,
            username = cleanName,
            email = cleanEmail,
            cloudSessionToken = sessionToken,
            refreshToken = refreshToken,
            authProvider = "LOCAL",
            createdAtTimestamp = serverRecord.createdAtTimestamp,
            lastSyncTimestamp = System.currentTimeMillis()
        )

        val accountsList = _savedCloudAccounts.value.filter { it.cloudUserId != newUserId } + account
        _savedCloudAccounts.value = accountsList
        _cloudAccount.value = account
        saveAccountsToStorage()

        performCloudSync()

        onResult(AuthResult.Success(account, session, "Compte Cloud créé avec succès ! Mot de passe chiffré et session sécurisée activée."))
    }

    /**
     * Connexion (Login):
     * 1. Finds user by email or username on Server DB
     * 2. Verifies salted password hash
     * 3. Generates new device session token & refresh token
     * 4. Persists session locally
     * 5. Activates account & syncs library
     */
    fun loginCloudAccount(
        emailOrUsername: String,
        passwordPlain: String,
        onResult: (AuthResult) -> Unit
    ) {
        val query = emailOrUsername.trim()
        if (query.isBlank() || passwordPlain.isBlank()) {
            onResult(AuthResult.Error("Veuillez renseigner votre identifiant et mot de passe."))
            return
        }

        val serverRecord = _serverUsersDb.value.find {
            (it.email.equals(query, ignoreCase = true) || it.username.equals(query, ignoreCase = true))
        }

        if (serverRecord == null) {
            onResult(AuthResult.Error("Aucun compte trouvé avec cet identifiant sur le serveur SLM."))
            return
        }

        if (serverRecord.isDeleted) {
            onResult(AuthResult.Error("Ce compte a été supprimé ou désactivé sur le serveur."))
            return
        }

        val isPasswordValid = SecurityAuthHelper.verifyPassword(
            candidate = passwordPlain,
            salt = serverRecord.passwordSalt,
            expectedHash = serverRecord.passwordHash
        )

        if (!isPasswordValid) {
            onResult(AuthResult.Error("Mot de passe incorrect. Veuillez vérifier vos identifiants."))
            return
        }

        // Issue new device session
        val sessionToken = SecurityAuthHelper.generateSessionToken(serverRecord.cloudUserId, deviceId)
        val refreshToken = SecurityAuthHelper.generateRefreshToken(serverRecord.cloudUserId)
        val session = AuthSession(sessionToken, refreshToken, serverRecord.cloudUserId, deviceId)
        saveActiveDeviceSession(session)

        // Update server login timestamp
        val updatedServerRecord = serverRecord.copy(lastLoginTimestamp = System.currentTimeMillis())
        val updatedDb = _serverUsersDb.value.map { if (it.cloudUserId == serverRecord.cloudUserId) updatedServerRecord else it }
        saveServerUsersDatabase(updatedDb)

        val account = CloudAccount(
            cloudUserId = serverRecord.cloudUserId,
            username = serverRecord.username,
            email = serverRecord.email,
            avatarUri = serverRecord.avatarUri,
            cloudSessionToken = sessionToken,
            refreshToken = refreshToken,
            authProvider = serverRecord.authProvider,
            cloudPlan = serverRecord.cloudPlan,
            usedStorageBytes = serverRecord.usedStorageBytes,
            totalQuotaBytes = serverRecord.totalQuotaBytes,
            createdAtTimestamp = serverRecord.createdAtTimestamp,
            lastSyncTimestamp = System.currentTimeMillis()
        )

        val accountsList = _savedCloudAccounts.value.filter { it.cloudUserId != account.cloudUserId } + account
        _savedCloudAccounts.value = accountsList
        _cloudAccount.value = account
        saveAccountsToStorage()

        performCloudSync()

        onResult(AuthResult.Success(account, session, "Connexion réussie ! Vos données Cloud sont synchronisées."))
    }

    /**
     * OAuth Third-Party Login (Google / Apple):
     * Simulates external ID Token issuance and authentic server verification.
     */
    fun loginWithOAuth(
        provider: String, // "GOOGLE_OAUTH" or "APPLE_ID"
        displayName: String,
        email: String,
        avatarUrl: String? = null,
        onResult: (AuthResult) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        val providerName = if (provider == "GOOGLE_OAUTH") "Google" else "Apple"

        var serverRecord = _serverUsersDb.value.find {
            it.email.equals(cleanEmail, ignoreCase = true) && !it.isDeleted
        }

        if (serverRecord == null) {
            // Register via OAuth on server
            val salt = SecurityAuthHelper.generateSalt()
            val fakeHash = SecurityAuthHelper.hashPassword(UUID.randomUUID().toString(), salt)
            val newUserId = "oauth_${provider.take(3).lowercase()}_" + UUID.randomUUID().toString().take(8)
            val oauthToken = "id_tok_${provider}_" + UUID.randomUUID().toString().take(16)

            serverRecord = CloudUserServerRecord(
                cloudUserId = newUserId,
                username = displayName.trim().ifBlank { "Utilisateur $providerName" },
                email = cleanEmail,
                passwordHash = fakeHash,
                passwordSalt = salt,
                avatarUri = avatarUrl,
                authProvider = provider,
                oauthIdToken = oauthToken,
                createdAtTimestamp = System.currentTimeMillis(),
                lastLoginTimestamp = System.currentTimeMillis()
            )
            val updatedDb = _serverUsersDb.value + serverRecord
            saveServerUsersDatabase(updatedDb)
        }

        // Issue tokens
        val sessionToken = SecurityAuthHelper.generateSessionToken(serverRecord.cloudUserId, deviceId)
        val refreshToken = SecurityAuthHelper.generateRefreshToken(serverRecord.cloudUserId)
        val session = AuthSession(sessionToken, refreshToken, serverRecord.cloudUserId, deviceId)
        saveActiveDeviceSession(session)

        val account = CloudAccount(
            cloudUserId = serverRecord.cloudUserId,
            username = serverRecord.username,
            email = serverRecord.email,
            avatarUri = serverRecord.avatarUri,
            cloudSessionToken = sessionToken,
            refreshToken = refreshToken,
            authProvider = provider,
            cloudPlan = serverRecord.cloudPlan,
            createdAtTimestamp = serverRecord.createdAtTimestamp,
            lastSyncTimestamp = System.currentTimeMillis()
        )

        val accountsList = _savedCloudAccounts.value.filter { it.cloudUserId != account.cloudUserId } + account
        _savedCloudAccounts.value = accountsList
        _cloudAccount.value = account
        saveAccountsToStorage()

        performCloudSync()

        onResult(AuthResult.Success(account, session, "Authentification avec $providerName réussie."))
    }

    /**
     * Auto-Login on application launch:
     * Checks if a valid token exists on device Keystore/Secure storage.
     */
    private fun attemptAutoLoginFromDeviceSession() {
        val savedSessionToken = securePrefs.getString("active_session_token", null)
        val savedUserId = securePrefs.getString("active_user_id", null)

        if (savedSessionToken != null && savedUserId != null) {
            val serverRecord = _serverUsersDb.value.find { it.cloudUserId == savedUserId && !it.isDeleted }
            if (serverRecord != null) {
                val refreshToken = securePrefs.getString("active_refresh_token", SecurityAuthHelper.generateRefreshToken(savedUserId)) ?: ""
                val session = AuthSession(savedSessionToken, refreshToken, savedUserId, deviceId)
                _activeSession.value = session

                val acc = CloudAccount(
                    cloudUserId = serverRecord.cloudUserId,
                    username = serverRecord.username,
                    email = serverRecord.email,
                    avatarUri = serverRecord.avatarUri,
                    cloudSessionToken = savedSessionToken,
                    refreshToken = refreshToken,
                    authProvider = serverRecord.authProvider,
                    cloudPlan = serverRecord.cloudPlan,
                    usedStorageBytes = serverRecord.usedStorageBytes,
                    totalQuotaBytes = serverRecord.totalQuotaBytes,
                    createdAtTimestamp = serverRecord.createdAtTimestamp,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
                _cloudAccount.value = acc
                return
            }
        }

        // Fallback to active account from list or guest
        val activeId = prefs.getString("active_cloud_user_id", null)
        val active = _savedCloudAccounts.value.find { it.cloudUserId == activeId } ?: _savedCloudAccounts.value.firstOrNull()
        if (active != null) {
            _cloudAccount.value = active
        }
    }

    private fun saveActiveDeviceSession(session: AuthSession) {
        _activeSession.value = session
        securePrefs.edit()
            .putString("active_session_token", session.sessionToken)
            .putString("active_refresh_token", session.refreshToken)
            .putString("active_user_id", session.userId)
            .putLong("session_expires_at", session.expiresAt)
            .apply()
    }

    private fun clearActiveDeviceSession() {
        _activeSession.value = null
        securePrefs.edit()
            .remove("active_session_token")
            .remove("active_refresh_token")
            .remove("active_user_id")
            .remove("session_expires_at")
            .apply()
    }

    /**
     * Déconnexion (Log out):
     * Purges local session token from device without deleting server account data.
     */
    fun logoutCloudAccount() {
        clearActiveDeviceSession()
        val current = _cloudAccount.value

        // Switch to Guest account
        val guest = CloudAccount(
            cloudUserId = "cloud_guest_" + UUID.randomUUID().toString().take(6),
            username = "Invité SLM",
            email = "invite@slmcloud.local",
            cloudPlan = "SLM Cloud Local 1Go",
            lastSyncTimestamp = System.currentTimeMillis()
        )
        if (current != null) {
            _savedCloudAccounts.value = _savedCloudAccounts.value.filter { it.cloudUserId != current.cloudUserId } + guest
        } else {
            _savedCloudAccounts.value = listOf(guest)
        }
        _cloudAccount.value = guest
        saveAccountsToStorage()
    }

    /**
     * Suppression de Compte (Account Deletion):
     * Dual process:
     * 1. Local Device: purges device session tokens, clears Room cache for this user.
     * 2. Cloud Server: hard delete or soft delete (`isDeleted = true`) from central database and purges backups.
     */
    fun deleteCloudAccount(
        cloudUserId: String,
        hardDelete: Boolean = true,
        onDeleted: ((String) -> Unit)? = null
    ) {
        scope.launch {
            // 1. Server Cleanup
            if (hardDelete) {
                // Hard delete: purge entirely from server DB & delete associated backups
                val updatedDb = _serverUsersDb.value.filter { it.cloudUserId != cloudUserId }
                saveServerUsersDatabase(updatedDb)
                _cloudBackups.value = _cloudBackups.value.filter { it.cloudUserId != cloudUserId }
                saveBackupsToStorage()
            } else {
                // Soft delete: flag record as deleted
                val updatedDb = _serverUsersDb.value.map {
                    if (it.cloudUserId == cloudUserId) {
                        it.copy(isDeleted = true, deletionTimestamp = System.currentTimeMillis())
                    } else it
                }
                saveServerUsersDatabase(updatedDb)
            }

            // 2. Local Cleanup (Room DB & Keystore tokens)
            withContext(Dispatchers.IO) {
                try {
                    musicDao.deleteTracksForUser(cloudUserId)
                    musicDao.deletePlaylistsForUser(cloudUserId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Clear local session if current user
            if (_cloudAccount.value?.cloudUserId == cloudUserId) {
                clearActiveDeviceSession()
            }

            // Update saved accounts list
            val list = _savedCloudAccounts.value.filter { it.cloudUserId != cloudUserId }
            val nextAccount = list.firstOrNull() ?: CloudAccount(
                cloudUserId = "cloud_guest_" + UUID.randomUUID().toString().take(6),
                username = "Invité SLM",
                email = "invite@slmcloud.local"
            )
            _savedCloudAccounts.value = if (list.isEmpty()) listOf(nextAccount) else list
            _cloudAccount.value = nextAccount
            saveAccountsToStorage()

            withContext(Dispatchers.Main) {
                val mode = if (hardDelete) "définitivement effacé du serveur" else "désactivé"
                onDeleted?.invoke("Compte $mode. Les jetons locaux et données ont été purgés.")
            }
        }
    }

    fun switchCloudAccount(cloudUserId: String) {
        val target = _savedCloudAccounts.value.find { it.cloudUserId == cloudUserId }
        if (target != null) {
            _cloudAccount.value = target
            prefs.edit().putString("active_cloud_user_id", target.cloudUserId).apply()

            // Issue session for switched account
            val sessionToken = SecurityAuthHelper.generateSessionToken(target.cloudUserId, deviceId)
            val session = AuthSession(sessionToken, target.refreshToken, target.cloudUserId, deviceId)
            saveActiveDeviceSession(session)

            updateLastSyncFormatted(target.lastSyncTimestamp)
            performCloudSync()
        }
    }

    fun updateCloudProfile(username: String, avatarUri: String?) {
        val current = _cloudAccount.value ?: return
        scope.launch {
            val persistentAvatar = if (!avatarUri.isNullOrBlank()) {
                val uri = Uri.parse(avatarUri)
                StoragePersistenceManager.persistImage(context, uri, "avatars", current.cloudUserId) ?: avatarUri
            } else {
                avatarUri
            }

            val updated = current.copy(
                username = username.trim(),
                avatarUri = persistentAvatar
            )
            _cloudAccount.value = updated
            _savedCloudAccounts.value = _savedCloudAccounts.value.map {
                if (it.cloudUserId == updated.cloudUserId) updated else it
            }
            saveAccountsToStorage()

            // Sync with server db
            val serverDb = _serverUsersDb.value.map {
                if (it.cloudUserId == updated.cloudUserId) {
                    it.copy(username = updated.username, avatarUri = updated.avatarUri)
                } else it
            }
            saveServerUsersDatabase(serverDb)
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
        if (enabled) {
            performCloudSync()
        }
    }

    fun performCloudSync(onComplete: ((Boolean, String) -> Unit)? = null) {
        scope.launch {
            _cloudConnectionStatus.value = CloudConnectionStatus.SYNCING
            withContext(Dispatchers.IO) {
                try {
                    val currentAcc = _cloudAccount.value ?: return@withContext
                    val currentUserId = currentAcc.cloudUserId

                    // Calculate real data footprint
                    val playlists = musicDao.getPlaylistsByOwnerDirect(currentUserId)
                    val favorites = musicDao.getFavoriteTracksDirect(currentUserId)
                    val allTracks = musicDao.getTracksByOwnerDirect(currentUserId)
                    val bookmarks = _browserBookmarks.value

                    val approxSize = (playlists.size * 512L) + (favorites.size * 256L) + (allTracks.size * 300L) + (bookmarks.size * 128L) + 4096L

                    val now = System.currentTimeMillis()
                    val updatedAccount = currentAcc.copy(
                        lastSyncTimestamp = now,
                        usedStorageBytes = approxSize
                    )

                    _cloudAccount.value = updatedAccount
                    _savedCloudAccounts.value = _savedCloudAccounts.value.map {
                        if (it.cloudUserId == updatedAccount.cloudUserId) updatedAccount else it
                    }
                    saveAccountsToStorage()

                    // Update server record
                    val sDb = _serverUsersDb.value.map {
                        if (it.cloudUserId == currentUserId) it.copy(usedStorageBytes = approxSize, lastLoginTimestamp = now) else it
                    }
                    saveServerUsersDatabase(sDb)

                    updateLastSyncFormatted(now)
                    _cloudConnectionStatus.value = CloudConnectionStatus.CONNECTED
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(true, "Synchronisation Cloud réussie : ${playlists.size} playlists, ${favorites.size} favoris, ${bookmarks.size} signets.")
                    }
                } catch (e: Exception) {
                    _cloudConnectionStatus.value = CloudConnectionStatus.CONNECTED
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, "Synchronisation locale terminée.")
                    }
                }
            }
        }
    }

    // ================= BACKUPS / SNAPSHOTS =================
    fun createCloudBackupSnapshot(onCreated: ((CloudBackupSnapshot) -> Unit)? = null) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val currentAcc = _cloudAccount.value ?: return@withContext
                val currentUserId = currentAcc.cloudUserId

                val playlists = musicDao.getPlaylistsByOwnerDirect(currentUserId)
                val favorites = musicDao.getFavoriteTracksDirect(currentUserId)
                val allTracks = musicDao.getTracksByOwnerDirect(currentUserId)
                val bookmarks = _browserBookmarks.value

                val jsonObject = JSONObject().apply {
                    put("version", "2.0")
                    put("userId", currentUserId)
                    put("username", currentAcc.username)
                    put("timestamp", System.currentTimeMillis())

                    val playArr = JSONArray()
                    playlists.forEach { p ->
                        playArr.put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("description", p.description)
                            put("gradientIndex", p.gradientIndex)
                        })
                    }
                    put("playlists", playArr)

                    val bmArr = JSONArray()
                    bookmarks.forEach { b ->
                        bmArr.put(JSONObject().apply {
                            put("id", b.id)
                            put("title", b.title)
                            put("url", b.url)
                            put("emoji", b.iconEmoji)
                            put("timestamp", b.addedTimestamp)
                        })
                    }
                    put("bookmarks", bmArr)

                    val trArr = JSONArray()
                    allTracks.forEach { t ->
                        trArr.put(JSONObject().apply {
                            put("id", t.id)
                            put("title", t.title)
                            put("artist", t.artist)
                            put("album", t.album)
                            put("isFavorite", t.isFavorite)
                        })
                    }
                    put("tracks", trArr)
                }

                val payload = jsonObject.toString()
                val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date())

                val snapshot = CloudBackupSnapshot(
                    id = "snapshot_" + UUID.randomUUID().toString().take(8),
                    cloudUserId = currentUserId,
                    title = "Sauvegarde Cloud • $formattedDate",
                    timestamp = System.currentTimeMillis(),
                    playlistsCount = playlists.size,
                    tracksCount = allTracks.size,
                    favoritesCount = favorites.size,
                    bookmarksCount = bookmarks.size,
                    snapshotSizeBytes = payload.toByteArray().size.toLong() + 2048L,
                    jsonPayload = payload
                )

                val updatedList = listOf(snapshot) + _cloudBackups.value
                _cloudBackups.value = updatedList
                saveBackupsToStorage()

                withContext(Dispatchers.Main) {
                    onCreated?.invoke(snapshot)
                }
            }
        }
    }

    fun restoreCloudBackupSnapshot(snapshot: CloudBackupSnapshot, onRestored: ((Boolean, String) -> Unit)? = null) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val currentUserId = _cloudAccount.value?.cloudUserId ?: "guest"
                    val json = JSONObject(snapshot.jsonPayload)

                    if (json.has("playlists")) {
                        val playArr = json.getJSONArray("playlists")
                        for (i in 0 until playArr.length()) {
                            val pObj = playArr.getJSONObject(i)
                            val p = PlaylistEntity(
                                id = pObj.optString("id", UUID.randomUUID().toString()),
                                name = pObj.optString("name", "Playlist"),
                                description = pObj.optString("description", "Restaurée du Cloud"),
                                gradientIndex = pObj.optInt("gradientIndex", 0),
                                userId = currentUserId
                            )
                            musicDao.insertPlaylist(p)
                        }
                    }

                    if (json.has("bookmarks")) {
                        val bmArr = json.getJSONArray("bookmarks")
                        val restoredBookmarks = mutableListOf<BrowserBookmark>()
                        for (i in 0 until bmArr.length()) {
                            val bObj = bmArr.getJSONObject(i)
                            restoredBookmarks.add(
                                BrowserBookmark(
                                    id = bObj.optString("id", UUID.randomUUID().toString()),
                                    title = bObj.optString("title", "Page"),
                                    url = bObj.optString("url", "https://google.com"),
                                    iconEmoji = bObj.optString("emoji", "🌐"),
                                    addedTimestamp = bObj.optLong("timestamp", System.currentTimeMillis()),
                                    isCloudSynced = true
                                )
                            )
                        }
                        if (restoredBookmarks.isNotEmpty()) {
                            _browserBookmarks.value = restoredBookmarks
                            saveBookmarksToStorage()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        onRestored?.invoke(true, "Sauvegarde Cloud restaurée avec succès !")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onRestored?.invoke(false, "Erreur lors de la restauration du snapshot : ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    fun deleteCloudBackupSnapshot(snapshotId: String) {
        _cloudBackups.value = _cloudBackups.value.filter { it.id != snapshotId }
        saveBackupsToStorage()
    }

    private fun loadBackupsFromStorage() {
        val backupsJson = prefs.getString("cloud_backups_list", "[]") ?: "[]"
        val list = mutableListOf<CloudBackupSnapshot>()
        try {
            val arr = JSONArray(backupsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CloudBackupSnapshot(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        cloudUserId = obj.optString("cloudUserId", ""),
                        title = obj.optString("title", "Sauvegarde Cloud"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        playlistsCount = obj.optInt("playlistsCount", 0),
                        tracksCount = obj.optInt("tracksCount", 0),
                        favoritesCount = obj.optInt("favoritesCount", 0),
                        bookmarksCount = obj.optInt("bookmarksCount", 0),
                        snapshotSizeBytes = obj.optLong("snapshotSizeBytes", 10240L),
                        jsonPayload = obj.optString("jsonPayload", "{}")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _cloudBackups.value = list
    }

    private fun saveBackupsToStorage() {
        val arr = JSONArray()
        _cloudBackups.value.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("cloudUserId", s.cloudUserId)
                put("title", s.title)
                put("timestamp", s.timestamp)
                put("playlistsCount", s.playlistsCount)
                put("tracksCount", s.tracksCount)
                put("favoritesCount", s.favoritesCount)
                put("bookmarksCount", s.bookmarksCount)
                put("snapshotSizeBytes", s.snapshotSizeBytes)
                put("jsonPayload", s.jsonPayload)
            }
            arr.put(obj)
        }
        prefs.edit().putString("cloud_backups_list", arr.toString()).apply()
    }

    // ================= BROWSER BOOKMARKS & HISTORY =================
    fun addBookmark(title: String, url: String, emoji: String = "🌐") {
        val existing = _browserBookmarks.value.find { it.url.equals(url.trim(), ignoreCase = true) }
        if (existing == null) {
            val newBm = BrowserBookmark(
                title = title.trim().ifBlank { url.trim() },
                url = url.trim(),
                iconEmoji = emoji,
                isCloudSynced = true
            )
            _browserBookmarks.value = listOf(newBm) + _browserBookmarks.value
            saveBookmarksToStorage()
            if (_isAutoSyncEnabled.value) {
                performCloudSync()
            }
        }
    }

    fun removeBookmark(bookmarkId: String) {
        _browserBookmarks.value = _browserBookmarks.value.filter { it.id != bookmarkId }
        saveBookmarksToStorage()
    }

    fun addHistoryItem(title: String, url: String) {
        if (url.isBlank() || url == "about:blank") return
        val item = BrowserHistoryItem(
            title = title.trim().ifBlank { url.trim() },
            url = url.trim(),
            timestamp = System.currentTimeMillis()
        )
        _browserHistory.value = (listOf(item) + _browserHistory.value.filter { it.url != url }).take(100)
        saveHistoryToStorage()
    }

    fun clearHistory() {
        _browserHistory.value = emptyList()
        saveHistoryToStorage()
    }

    private fun loadBookmarksFromStorage() {
        val bmJson = prefs.getString("browser_bookmarks_json", null)
        if (bmJson == null) {
            val defaults = listOf(
                BrowserBookmark(title = "Google", url = "https://www.google.com", iconEmoji = "🔍"),
                BrowserBookmark(title = "YouTube", url = "https://www.youtube.com", iconEmoji = "▶️"),
                BrowserBookmark(title = "SoundCloud", url = "https://soundcloud.com", iconEmoji = "☁️"),
                BrowserBookmark(title = "Spotify Web", url = "https://open.spotify.com", iconEmoji = "🎧"),
                BrowserBookmark(title = "Genius Paroles", url = "https://genius.com", iconEmoji = "📜"),
                BrowserBookmark(title = "Radio Garden", url = "https://radio.garden", iconEmoji = "📻"),
                BrowserBookmark(title = "Wikipedia Musique", url = "https://fr.wikipedia.org/wiki/Musique", iconEmoji = "📚")
            )
            _browserBookmarks.value = defaults
            saveBookmarksToStorage()
        } else {
            val list = mutableListOf<BrowserBookmark>()
            try {
                val arr = JSONArray(bmJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        BrowserBookmark(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Signet"),
                            url = obj.optString("url", "https://google.com"),
                            iconEmoji = obj.optString("iconEmoji", "🌐"),
                            addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                            isCloudSynced = obj.optBoolean("isCloudSynced", true)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _browserBookmarks.value = list
        }
    }

    private fun saveBookmarksToStorage() {
        val arr = JSONArray()
        _browserBookmarks.value.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("url", b.url)
                put("iconEmoji", b.iconEmoji)
                put("addedTimestamp", b.addedTimestamp)
                put("isCloudSynced", b.isCloudSynced)
            }
            arr.put(obj)
        }
        prefs.edit().putString("browser_bookmarks_json", arr.toString()).apply()
    }

    private fun loadHistoryFromStorage() {
        val histJson = prefs.getString("browser_history_json", "[]") ?: "[]"
        val list = mutableListOf<BrowserHistoryItem>()
        try {
            val arr = JSONArray(histJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BrowserHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Page"),
                        url = obj.optString("url", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _browserHistory.value = list
    }

    private fun saveHistoryToStorage() {
        val arr = JSONArray()
        _browserHistory.value.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("url", h.url)
                put("timestamp", h.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("browser_history_json", arr.toString()).apply()
    }

    private fun updateLastSyncFormatted(timestamp: Long) {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60000
        _lastSyncTimeFormatted.value = when {
            minutes < 1 -> "À l'instant"
            minutes < 60 -> "Il y a $minutes min"
            else -> {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Aujourd'hui à ${sdf.format(Date(timestamp))}"
            }
        }
    }

    private fun loadAccountsFromStorage() {
        val accountsJson = prefs.getString("cloud_accounts_list", "[]") ?: "[]"
        val list = mutableListOf<CloudAccount>()
        try {
            val arr = JSONArray(accountsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CloudAccount(
                        cloudUserId = obj.optString("cloudUserId", UUID.randomUUID().toString()),
                        username = obj.optString("username", "Utilisateur"),
                        email = obj.optString("email", ""),
                        avatarUri = if (obj.has("avatarUri") && !obj.isNull("avatarUri")) obj.getString("avatarUri") else null,
                        cloudSessionToken = obj.optString("cloudSessionToken", UUID.randomUUID().toString()),
                        refreshToken = obj.optString("refreshToken", UUID.randomUUID().toString()),
                        isVerified = obj.optBoolean("isVerified", true),
                        cloudPlan = obj.optString("cloudPlan", "SLM Cloud Pro 5Go"),
                        authProvider = obj.optString("authProvider", "LOCAL"),
                        usedStorageBytes = obj.optLong("usedStorageBytes", 153600L),
                        totalQuotaBytes = obj.optLong("totalQuotaBytes", 5368709120L),
                        lastSyncTimestamp = obj.optLong("lastSyncTimestamp", System.currentTimeMillis()),
                        createdAtTimestamp = obj.optLong("createdAtTimestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _savedCloudAccounts.value = list

        val activeId = prefs.getString("active_cloud_user_id", null)
        val active = list.find { it.cloudUserId == activeId } ?: list.firstOrNull()
        if (active != null) {
            _cloudAccount.value = active
            updateLastSyncFormatted(active.lastSyncTimestamp)
        } else {
            val defaultAcc = CloudAccount(
                cloudUserId = "cloud_user_default",
                username = "Membre SLM Cloud",
                email = "cloud.user@slmplay.net",
                avatarUri = null,
                cloudSessionToken = "tk_slm_" + UUID.randomUUID().toString().take(12),
                refreshToken = "ref_slm_" + UUID.randomUUID().toString().take(12),
                lastSyncTimestamp = System.currentTimeMillis()
            )
            _savedCloudAccounts.value = listOf(defaultAcc)
            _cloudAccount.value = defaultAcc
            saveAccountsToStorage()
            updateLastSyncFormatted(defaultAcc.lastSyncTimestamp)
        }
    }

    private fun saveAccountsToStorage() {
        val arr = JSONArray()
        _savedCloudAccounts.value.forEach { acc ->
            val obj = JSONObject().apply {
                put("cloudUserId", acc.cloudUserId)
                put("username", acc.username)
                put("email", acc.email)
                put("avatarUri", acc.avatarUri)
                put("cloudSessionToken", acc.cloudSessionToken)
                put("refreshToken", acc.refreshToken)
                put("isVerified", acc.isVerified)
                put("cloudPlan", acc.cloudPlan)
                put("authProvider", acc.authProvider)
                put("usedStorageBytes", acc.usedStorageBytes)
                put("totalQuotaBytes", acc.totalQuotaBytes)
                put("lastSyncTimestamp", acc.lastSyncTimestamp)
                put("createdAtTimestamp", acc.createdAtTimestamp)
            }
            arr.put(obj)
        }
        prefs.edit()
            .putString("cloud_accounts_list", arr.toString())
            .putString("active_cloud_user_id", _cloudAccount.value?.cloudUserId)
            .apply()
    }
}
