package com.example.slmplay.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.slmplay.data.db.MusicDatabase
import com.example.slmplay.data.db.PlaylistEntity
import com.example.slmplay.data.db.TrackEntity
import com.example.slmplay.data.model.*
import com.example.slmplay.data.repository.CloudSyncManager
import com.example.slmplay.data.repository.MusicRepository
import com.example.slmplay.service.MusicPlaybackService
import com.example.slmplay.utils.DynamicArtworkExtractor
import com.example.slmplay.utils.SecurityVaultManager
import com.example.slmplay.utils.UniversalDownloadManager
import com.example.slmplay.utils.WebSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    private var playbackService: MusicPlaybackService? = null
    private var isBound = false

    private val userPrefs = application.getSharedPreferences("slm_play_user_system", Context.MODE_PRIVATE)

    // Current active User ID ("guest" for unauthenticated empty space, or unique user ID)
    private val _currentUserId = MutableStateFlow(userPrefs.getString("active_user_id", "guest") ?: "guest")
    val currentUserId = _currentUserId.asStateFlow()

    // Registered user accounts on device
    private val _savedAccounts = MutableStateFlow<List<UserAccount>>(emptyList())
    val savedAccounts = _savedAccounts.asStateFlow()

    // User Profile State (Strictly real data, no fake pre-fills)
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    // App Settings per-user
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings = _appSettings.asStateFlow()

    private val _isProfileAuthDialogOpen = MutableStateFlow(false)
    val isProfileAuthDialogOpen = _isProfileAuthDialogOpen.asStateFlow()

    // Cloud Sync Manager & State
    private val cloudSyncManager: CloudSyncManager
    val cloudAccount: StateFlow<CloudAccount?>
    val activeSession: StateFlow<AuthSession?>
    val savedCloudAccounts: StateFlow<List<CloudAccount>>
    val cloudConnectionStatus: StateFlow<CloudConnectionStatus>
    val cloudBackups: StateFlow<List<CloudBackupSnapshot>>
    val browserBookmarks: StateFlow<List<BrowserBookmark>>
    val browserHistory: StateFlow<List<BrowserHistoryItem>>
    val isAutoSyncEnabled: StateFlow<Boolean>
    val lastSyncTimeFormatted: StateFlow<String>

    // Persistent Web Browser Session State (survives tab switching & navigation)
    private val _browserTabs = MutableStateFlow<List<BrowserTab>>(
        listOf(
            BrowserTab(
                id = "tab_1",
                title = "Page d'accueil",
                url = "about:blank"
            )
        )
    )
    val browserTabs = _browserTabs.asStateFlow()

    private val _activeBrowserTabId = MutableStateFlow("tab_1")
    val activeBrowserTabId = _activeBrowserTabId.asStateFlow()

    private val _browserUrlInput = MutableStateFlow("")
    val browserUrlInput = _browserUrlInput.asStateFlow()

    private val _isBrowserDesktopMode = MutableStateFlow(false)
    val isBrowserDesktopMode = _isBrowserDesktopMode.asStateFlow()

    private val _isCloudHubOpen = MutableStateFlow(false)
    val isCloudHubOpen = _isCloudHubOpen.asStateFlow()

    // Security & Universal Download Architecture
    val securityVaultManager = SecurityVaultManager(application)
    val downloadManager = UniversalDownloadManager(application, securityVaultManager)

    val isPrivateSpaceActive = securityVaultManager.isPrivateSpaceActive
    val isDecoyModeActive = securityVaultManager.isDecoyModeActive
    val privateVaultFiles = securityVaultManager.vaultFiles

    private val _isPinLockScreenVisible = MutableStateFlow(false)
    val isPinLockScreenVisible = _isPinLockScreenVisible.asStateFlow()

    private val _isAppLaunchLocked = MutableStateFlow(
        securityVaultManager.isLockOnAppLaunchEnabled() && securityVaultManager.isPinConfigured()
    )
    val isAppLaunchLocked = _isAppLaunchLocked.asStateFlow()

    fun openPinLockForPrivateSpace() {
        _isPinLockScreenVisible.value = true
    }

    fun closePinLock() {
        _isPinLockScreenVisible.value = false
    }

    fun onPinAuthSuccess(isDecoy: Boolean) {
        _isPinLockScreenVisible.value = false
        _isAppLaunchLocked.value = false
        securityVaultManager.enterPrivateSpace(isDecoy)
    }

    fun onAppUnlockSuccess() {
        _isAppLaunchLocked.value = false
    }

    fun exitPrivateSpace() {
        if (securityVaultManager.isAutoIncognitoEnabled()) {
            WebSessionManager.clearPrivateSession(getApplication())
        }
        securityVaultManager.exitPrivateSpace()
    }

    // Dialog & Tool Screens visibility
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen = _isSettingsOpen.asStateFlow()

    private val _isMediaConverterOpen = MutableStateFlow(false)
    val isMediaConverterOpen = _isMediaConverterOpen.asStateFlow()

    private val _isAudioEditorOpen = MutableStateFlow(false)
    val isAudioEditorOpen = _isAudioEditorOpen.asStateFlow()

    private val _isEqualizerOpen = MutableStateFlow(false)
    val isEqualizerOpen = _isEqualizerOpen.asStateFlow()

    private val _isArtworkStudioOpen = MutableStateFlow(false)
    val isArtworkStudioOpen = _isArtworkStudioOpen.asStateFlow()

    private val _isFullscreenVisualizerOpen = MutableStateFlow(false)
    val isFullscreenVisualizerOpen = _isFullscreenVisualizerOpen.asStateFlow()

    private val _editingTrackForStudio = MutableStateFlow<TrackEntity?>(null)
    val editingTrackForStudio = _editingTrackForStudio.asStateFlow()

    // Album View & Details State
    private val _selectedAlbum = MutableStateFlow<AlbumModel?>(null)
    val selectedAlbum = _selectedAlbum.asStateFlow()

    private val _isAlbumDetailOpen = MutableStateFlow(false)
    val isAlbumDetailOpen = _isAlbumDetailOpen.asStateFlow()

    // UI & Navigation State
    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen = _isFullPlayerOpen.asStateFlow()

    private val _isLyricsOpen = MutableStateFlow(false)
    val isLyricsOpen = _isLyricsOpen.asStateFlow()

    private val _isCreatePlaylistDialogOpen = MutableStateFlow(false)
    val isCreatePlaylistDialogOpen = _isCreatePlaylistDialogOpen.asStateFlow()

    private val _isAddToPlaylistDialogOpen = MutableStateFlow(false)
    val isAddToPlaylistDialogOpen = _isAddToPlaylistDialogOpen.asStateFlow()

    private val _trackForPlaylistAction = MutableStateFlow<TrackEntity?>(null)
    val trackForPlaylistAction = _trackForPlaylistAction.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _backgroundMode = MutableStateFlow(BackgroundMode.DYNAMIC_MESH)
    val backgroundMode = _backgroundMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private val _activeVibe = MutableStateFlow<SlmVibe?>(null)
    val activeVibe = _activeVibe.asStateFlow()

    // Real-time audio amplitudes for reactive visualizer
    private val _audioAmplitudes = MutableStateFlow(FloatArray(16) { 0.25f })
    val audioAmplitudes = _audioAmplitudes.asStateFlow()

    // Dynamic Artwork Palette
    private val _dynamicArtworkPalette = MutableStateFlow(
        DynamicArtworkExtractor.ArtworkPalette(
            dominantColor = Color(0xFFFF2D55),
            accentColor = Color(0xFFAF52DE),
            surfaceColor = Color(0xFF12050A),
            isDark = true
        )
    )
    val dynamicArtworkPalette = _dynamicArtworkPalette.asStateFlow()

    // Service bound state mirrors
    val currentTrack = MutableStateFlow<TrackEntity?>(null)
    val isPlaying = MutableStateFlow(false)
    val currentPositionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(180000L)
    val isBuffering = MutableStateFlow(false)

    // Queue State
    val playQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val currentQueueIndex = MutableStateFlow(-1)
    private val _isQueueOpen = MutableStateFlow(false)
    val isQueueOpen = _isQueueOpen.asStateFlow()

    // User-isolated Repository Flows: reactive to active userId
    val allTracks: StateFlow<List<TrackEntity>>
    val favoriteTracks: StateFlow<List<TrackEntity>>
    val allPlaylists: StateFlow<List<PlaylistEntity>>
    val allAlbums: StateFlow<List<AlbumModel>>
    val filteredTracks: StateFlow<List<TrackEntity>>

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val selectedPlaylistTracks = _selectedPlaylistTracks.asStateFlow()

    val presetVibes = listOf(
        SlmVibe(
            id = "vibe_cyberpunk",
            title = "Cyberpunk Energy",
            subtitle = "Bassline percutante & néon rouge #ff2d55",
            emoji = "⚡",
            promptContext = "Énergie brute, synthétiseurs agressifs.",
            gradientColors = listOf(0xFFFF2D55, 0xFFAF52DE, 0xFF007AFF),
            targetBpmRange = "120 - 135 BPM"
        ),
        SlmVibe(
            id = "vibe_lofi",
            title = "Apple Glass Lofi",
            subtitle = "Détente acoustique et mélodies feutrées",
            emoji = "☕",
            promptContext = "Accords chauds, rythme décontracté.",
            gradientColors = listOf(0xFF5856D6, 0xFFFF2D55, 0xFFFF9500),
            targetBpmRange = "70 - 85 BPM"
        ),
        SlmVibe(
            id = "vibe_ambient",
            title = "HOpE Immersion 3D",
            subtitle = "Nappes spatiales & évasion sonore",
            emoji = "🌌",
            promptContext = "Sons éthérés, flou acoustique, sensation de lévitation.",
            gradientColors = listOf(0xFF00C7BE, 0xFF30B0C7, 0xFFAF52DE),
            targetBpmRange = "60 - 75 BPM"
        ),
        SlmVibe(
            id = "vibe_sunset",
            title = "Sunset Drive",
            subtitle = "Groove rétro-futuriste & mélancolie",
            emoji = "🌇",
            promptContext = "Ambiance route de nuit sous les néons.",
            gradientColors = listOf(0xFFFF375F, 0xFFFF9500, 0xFFFF2D55),
            targetBpmRange = "105 - 118 BPM"
        )
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicPlaybackService.MusicBinder
            playbackService = binder?.getService()
            isBound = true

            playbackService?.let { s ->
                viewModelScope.launch {
                    s.currentTrack.collect { trk ->
                        currentTrack.value = trk
                        if (trk != null) {
                            repository.recordPlay(trk.id)
                            if (_appSettings.value.isDynamicArtworkEnabled) {
                                val palette = DynamicArtworkExtractor.extractPalette(
                                    getApplication(),
                                    trk.coverUri,
                                    trk.coverResName
                                )
                                _dynamicArtworkPalette.value = palette
                            }
                        }
                    }
                }
                viewModelScope.launch {
                    s.isPlaying.collect { isPlaying.value = it }
                }
                viewModelScope.launch {
                    s.currentPosition.collect { currentPositionMs.value = it }
                }
                viewModelScope.launch {
                    s.duration.collect { durationMs.value = it }
                }
                viewModelScope.launch {
                    s.isBuffering.collect { isBuffering.value = it }
                }
                viewModelScope.launch {
                    s.audioAmplitudes.collect { _audioAmplitudes.value = it }
                }
                viewModelScope.launch {
                    s.playlistQueueFlow.collect { playQueue.value = it }
                }
                viewModelScope.launch {
                    s.currentQueueIndexFlow.collect { currentQueueIndex.value = it }
                }
                viewModelScope.launch {
                    s.sleepTimerSecondsLeft.collect { sec ->
                        _appSettings.value = _appSettings.value.copy(
                            isSleepTimerActive = sec > 0,
                            sleepTimerRemainingSeconds = sec
                        )
                    }
                }

                applySettingsToService(_appSettings.value)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isBound = false
        }
    }

    init {
        val database = MusicDatabase.getDatabase(application)
        repository = MusicRepository(application, database.musicDao())
        cloudSyncManager = CloudSyncManager(application, database.musicDao(), viewModelScope)

        cloudAccount = cloudSyncManager.cloudAccount
        activeSession = cloudSyncManager.activeSession
        savedCloudAccounts = cloudSyncManager.savedCloudAccounts
        cloudConnectionStatus = cloudSyncManager.cloudConnectionStatus
        cloudBackups = cloudSyncManager.cloudBackups
        browserBookmarks = cloudSyncManager.browserBookmarks
        browserHistory = cloudSyncManager.browserHistory
        isAutoSyncEnabled = cloudSyncManager.isAutoSyncEnabled
        lastSyncTimeFormatted = cloudSyncManager.lastSyncTimeFormatted

        // Purge any remnant demo/mock data
        viewModelScope.launch {
            repository.purgeDemoData()
        }

        // Load saved accounts list
        loadSavedAccounts()

        // Load active session
        val activeUid = _currentUserId.value
        refreshUserSession(activeUid)

        // Reactive User-Isolated Flows
        allTracks = _currentUserId.flatMapLatest { uid ->
            repository.getAllTracks(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        favoriteTracks = _currentUserId.flatMapLatest { uid ->
            repository.getFavoriteTracks(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allPlaylists = _currentUserId.flatMapLatest { uid ->
            repository.getAllPlaylists(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allAlbums = allTracks.map { trackList ->
            groupTracksIntoAlbums(trackList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        filteredTracks = combine(
            allTracks,
            _searchQuery,
            _appSettings
        ) { tracks, query, settings ->
            var list = if (query.isBlank()) {
                tracks
            } else {
                tracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.artist.contains(query, ignoreCase = true) ||
                            it.album.contains(query, ignoreCase = true) ||
                            it.genre.contains(query, ignoreCase = true)
                }
            }

            if (settings.isSmartLibraryEnabled) {
                list = when (settings.librarySortOption) {
                    LibrarySortOption.ADDED_RECENT -> list.sortedByDescending { it.addedDate }
                    LibrarySortOption.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
                    LibrarySortOption.ARTIST -> list.sortedBy { it.artist.lowercase() }
                    LibrarySortOption.ALBUM -> list.sortedBy { it.album.lowercase() }
                    LibrarySortOption.GENRE -> list.sortedBy { it.genre.lowercase() }
                    LibrarySortOption.YEAR -> list.sortedByDescending { it.year }
                    LibrarySortOption.DURATION -> list.sortedByDescending { it.durationMs }
                    LibrarySortOption.FAVORITES -> list.sortedByDescending { it.isFavorite }
                    LibrarySortOption.RECENTLY_PLAYED -> list.sortedByDescending { it.lastPlayedDate }
                }
            }

            list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        bindPlaybackService()
    }

    private fun bindPlaybackService() {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // ================= MULTI-USER ACCOUNT MANAGEMENT =================

    private fun loadSavedAccounts() {
        val jsonStr = userPrefs.getString("saved_accounts_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<UserAccount>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UserAccount(
                        id = obj.getString("id"),
                        username = obj.getString("username"),
                        emailOrId = obj.optString("email", ""),
                        avatarUri = if (obj.has("avatarUri") && !obj.isNull("avatarUri")) obj.getString("avatarUri") else null,
                        bio = obj.optString("bio", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            _savedAccounts.value = list
        } catch (e: Exception) {
            _savedAccounts.value = emptyList()
        }
    }

    private fun saveSavedAccounts(list: List<UserAccount>) {
        _savedAccounts.value = list
        try {
            val arr = JSONArray()
            list.forEach { acc ->
                val obj = JSONObject().apply {
                    put("id", acc.id)
                    put("username", acc.username)
                    put("email", acc.emailOrId)
                    put("avatarUri", acc.avatarUri)
                    put("bio", acc.bio)
                    put("createdAt", acc.createdAt)
                }
                arr.put(obj)
            }
            userPrefs.edit().putString("saved_accounts_json", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshUserSession(userId: String) {
        _currentUserId.value = userId
        userPrefs.edit().putString("active_user_id", userId).apply()

        if (userId == "guest" || userId.isBlank()) {
            _userProfile.value = UserProfile(
                userId = "guest",
                isLoggedIn = false,
                username = "",
                emailOrId = "",
                avatarUri = null
            )
            loadSettingsForUser("guest")
        } else {
            val account = _savedAccounts.value.find { it.id == userId }
            if (account != null) {
                _userProfile.value = UserProfile(
                    userId = account.id,
                    isLoggedIn = true,
                    username = account.username,
                    emailOrId = account.emailOrId,
                    avatarUri = account.avatarUri,
                    bio = account.bio,
                    joinedDate = account.createdAt
                )
                loadSettingsForUser(account.id)
            } else {
                _userProfile.value = UserProfile(
                    userId = userId,
                    isLoggedIn = false,
                    username = "",
                    emailOrId = ""
                )
                loadSettingsForUser(userId)
            }
        }
    }

    fun loginUser(username: String, emailOrId: String) {
        val cleanName = username.trim()
        val cleanEmail = emailOrId.trim()
        if (cleanName.isBlank()) return

        val existing = _savedAccounts.value.find {
            it.username.equals(cleanName, ignoreCase = true) ||
                    (cleanEmail.isNotBlank() && it.emailOrId.equals(cleanEmail, ignoreCase = true))
        }

        val accountId = if (existing != null) {
            existing.id
        } else {
            val newAcc = UserAccount(
                id = "user_" + UUID.randomUUID().toString(),
                username = cleanName,
                emailOrId = cleanEmail,
                createdAt = System.currentTimeMillis()
            )
            val updated = _savedAccounts.value + newAcc
            saveSavedAccounts(updated)
            newAcc.id
        }

        refreshUserSession(accountId)
        _isProfileAuthDialogOpen.value = false
        _statusMessage.value = "Connecté : Bienvenue $cleanName"
    }

    fun registerUser(username: String, emailOrId: String) {
        val cleanName = username.trim()
        val cleanEmail = emailOrId.trim()
        if (cleanName.isBlank()) return

        val newAcc = UserAccount(
            id = "user_" + UUID.randomUUID().toString(),
            username = cleanName,
            emailOrId = cleanEmail,
            createdAt = System.currentTimeMillis()
        )
        val updated = _savedAccounts.value.filter { it.id != newAcc.id } + newAcc
        saveSavedAccounts(updated)

        refreshUserSession(newAcc.id)
        _isProfileAuthDialogOpen.value = false
        _statusMessage.value = "Espace créé pour $cleanName"
    }

    fun switchAccount(targetUserId: String) {
        if (_currentUserId.value == targetUserId) return
        playbackService?.togglePlayPause() // pause playback when switching users
        _selectedPlaylist.value = null
        _selectedPlaylistTracks.value = emptyList()
        refreshUserSession(targetUserId)
        _statusMessage.value = if (targetUserId == "guest") "Session invité active" else "Changement d'utilisateur effectué"
    }

    fun updateProfile(username: String, avatarUri: String?) {
        val currentUid = _currentUserId.value
        if (currentUid == "guest") return

        val cleanName = username.trim()
        val updatedList = _savedAccounts.value.map {
            if (it.id == currentUid) {
                it.copy(username = if (cleanName.isNotBlank()) cleanName else it.username, avatarUri = avatarUri)
            } else {
                it
            }
        }
        saveSavedAccounts(updatedList)
        refreshUserSession(currentUid)
        _statusMessage.value = "Profil mis à jour"
    }

    fun logoutUser() {
        playbackService?.togglePlayPause()
        _selectedPlaylist.value = null
        _selectedPlaylistTracks.value = emptyList()
        refreshUserSession("guest")
        _isProfileAuthDialogOpen.value = false
        _statusMessage.value = "Déconnexion effectuée"
    }

    fun deleteUserAccount() {
        val uid = _currentUserId.value
        if (uid != "guest") {
            viewModelScope.launch {
                repository.deleteUserData(uid)
            }
            val updated = _savedAccounts.value.filter { it.id != uid }
            saveSavedAccounts(updated)
        }
        logoutUser()
        _statusMessage.value = "Compte et données personnelles supprimés"
    }

    // ================= USER SETTINGS PERSISTENCE =================

    private fun loadSettingsForUser(userId: String) {
        val p = getApplication<Application>().getSharedPreferences("user_settings_$userId", Context.MODE_PRIVATE)
        val savedVisualizerType = try {
            VisualizerType.valueOf(p.getString("visualizer_type", VisualizerType.BARS.name) ?: VisualizerType.BARS.name)
        } catch (e: Exception) {
            VisualizerType.BARS
        }
        val s = AppSettings(
            isMediaConverterEnabled = p.getBoolean("media_converter", true),
            isAudioEditorEnabled = p.getBoolean("audio_editor", true),
            isEqualizerEnabled = p.getBoolean("equalizer_enabled", true),
            selectedEqPreset = p.getString("eq_preset", "Bass Boost") ?: "Bass Boost",
            bassBoostStrength = p.getFloat("bass_boost", 0.6f),
            virtualizerStrength = p.getFloat("virtualizer", 0.4f),
            visualizerType = savedVisualizerType,
            isSlmVibeEnabled = p.getBoolean("vibes_enabled", true),
            isDynamicArtworkEnabled = p.getBoolean("dynamic_artwork", true),
            isArtworkStudioEnabled = p.getBoolean("artwork_studio", true),
            isSmartLibraryEnabled = p.getBoolean("smart_library", true),
            librarySortOption = try {
                LibrarySortOption.valueOf(p.getString("library_sort", LibrarySortOption.ADDED_RECENT.name) ?: LibrarySortOption.ADDED_RECENT.name)
            } catch (e: Exception) {
                LibrarySortOption.ADDED_RECENT
            },
            isSmartFavoritesEnabled = p.getBoolean("smart_favorites", true),
            isSmartShuffleEnabled = p.getBoolean("smart_shuffle", true),
            isGaplessPlaybackEnabled = p.getBoolean("gapless", true),
            isCrossfadeEnabled = p.getBoolean("crossfade", true),
            crossfadeDurationSeconds = p.getInt("crossfade_duration", 3)
        )
        _appSettings.value = s
        applySettingsToService(s)
    }

    fun updateSettings(newSettings: AppSettings) {
        _appSettings.value = newSettings
        val userId = _currentUserId.value
        val p = getApplication<Application>().getSharedPreferences("user_settings_$userId", Context.MODE_PRIVATE)
        p.edit()
            .putBoolean("media_converter", newSettings.isMediaConverterEnabled)
            .putBoolean("audio_editor", newSettings.isAudioEditorEnabled)
            .putBoolean("equalizer_enabled", newSettings.isEqualizerEnabled)
            .putString("eq_preset", newSettings.selectedEqPreset)
            .putFloat("bass_boost", newSettings.bassBoostStrength)
            .putFloat("virtualizer", newSettings.virtualizerStrength)
            .putString("visualizer_type", newSettings.visualizerType.name)
            .putBoolean("vibes_enabled", newSettings.isSlmVibeEnabled)
            .putBoolean("dynamic_artwork", newSettings.isDynamicArtworkEnabled)
            .putBoolean("artwork_studio", newSettings.isArtworkStudioEnabled)
            .putBoolean("smart_library", newSettings.isSmartLibraryEnabled)
            .putString("library_sort", newSettings.librarySortOption.name)
            .putBoolean("smart_favorites", newSettings.isSmartFavoritesEnabled)
            .putBoolean("smart_shuffle", newSettings.isSmartShuffleEnabled)
            .putBoolean("gapless", newSettings.isGaplessPlaybackEnabled)
            .putBoolean("crossfade", newSettings.isCrossfadeEnabled)
            .putInt("crossfade_duration", newSettings.crossfadeDurationSeconds)
            .apply()

        applySettingsToService(newSettings)
    }

    private fun applySettingsToService(settings: AppSettings) {
        playbackService?.let { s ->
            s.setSmartShuffle(settings.isSmartShuffleEnabled)
            s.setGapless(settings.isGaplessPlaybackEnabled)
            s.setCrossfade(settings.isCrossfadeEnabled, settings.crossfadeDurationSeconds)
            val effectMgr = s.getAudioEffectManager()
            effectMgr.setEqualizerEnabled(settings.isEqualizerEnabled)
            if (settings.isEqualizerEnabled) {
                if (settings.selectedEqPreset == "Manuel") {
                    settings.manualBands.forEachIndexed { i, gain ->
                        effectMgr.setManualBand(i, gain)
                    }
                } else {
                    effectMgr.applyPreset(settings.selectedEqPreset)
                }
                effectMgr.setBassBoost(settings.bassBoostStrength)
                effectMgr.setVirtualizer(settings.virtualizerStrength)
            }
        }
    }

    // Settings & Tool Dialogs
    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    fun openMediaConverter() { _isMediaConverterOpen.value = true }
    fun closeMediaConverter() { _isMediaConverterOpen.value = false }

    fun openAudioEditor(track: TrackEntity? = null) {
        _editingTrackForStudio.value = track ?: currentTrack.value ?: allTracks.value.firstOrNull()
        _isAudioEditorOpen.value = true
    }
    fun closeAudioEditor() { _isAudioEditorOpen.value = false }

    fun openEqualizer() { _isEqualizerOpen.value = true }
    fun closeEqualizer() { _isEqualizerOpen.value = false }

    fun openArtworkStudio(track: TrackEntity? = null) {
        _editingTrackForStudio.value = track ?: currentTrack.value ?: allTracks.value.firstOrNull()
        _isArtworkStudioOpen.value = true
    }
    fun closeArtworkStudio() { _isArtworkStudioOpen.value = false }

    fun openFullscreenVisualizer() { _isFullscreenVisualizerOpen.value = true }
    fun closeFullscreenVisualizer() { _isFullscreenVisualizerOpen.value = false }

    fun openQueue() { _isQueueOpen.value = true }
    fun closeQueue() { _isQueueOpen.value = false }

    fun setEqualizerPreset(preset: String) {
        val updated = _appSettings.value.copy(selectedEqPreset = preset)
        updateSettings(updated)
    }

    fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        val bands = _appSettings.value.manualBands.toMutableList()
        if (bandIndex in bands.indices) {
            bands[bandIndex] = gainDb
            val updated = _appSettings.value.copy(
                manualBands = bands,
                selectedEqPreset = "Manuel"
            )
            updateSettings(updated)
        }
    }

    fun setBassBoost(strength: Float) {
        val updated = _appSettings.value.copy(bassBoostStrength = strength)
        updateSettings(updated)
    }

    fun setVirtualizer(strength: Float) {
        val updated = _appSettings.value.copy(virtualizerStrength = strength)
        updateSettings(updated)
    }

    fun setVisualizerType(type: VisualizerType) {
        val updated = _appSettings.value.copy(visualizerType = type)
        updateSettings(updated)
    }

    fun setLibrarySortOption(option: LibrarySortOption) {
        val updated = _appSettings.value.copy(librarySortOption = option)
        updateSettings(updated)
    }

    fun startSleepTimer(minutes: Int, endOnTrack: Boolean = false) {
        playbackService?.startSleepTimer(minutes, endOnTrack)
        _statusMessage.value = if (endOnTrack) "Minuteur : Arrêt à la fin du morceau" else "Minuteur activé : $minutes minutes"
    }

    fun cancelSleepTimer() {
        playbackService?.cancelSleepTimer()
        _appSettings.value = _appSettings.value.copy(isSleepTimerActive = false, sleepTimerRemainingSeconds = 0L)
        _statusMessage.value = "Minuteur de sommeil annulé"
    }

    fun addConvertedTrack(track: TrackEntity) {
        viewModelScope.launch {
            val userTrack = track.copy(userId = _currentUserId.value)
            repository.insertCustomTrack(userTrack)
            _statusMessage.value = "« ${track.title} » ajoutée à votre espace"
        }
    }

    fun saveTrackMetadata(
        id: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        coverUri: String?
    ) {
        viewModelScope.launch {
            repository.updateTrackMetadata(id, title, artist, album, genre, year, coverUri)
            _statusMessage.value = "Métadonnées mises à jour avec succès"
        }
    }

    fun restoreTrackMetadata(id: String) {
        viewModelScope.launch {
            repository.restoreTrackMetadata(id)
            _statusMessage.value = "Informations originales restaurées"
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playTrack(track: TrackEntity, queue: List<TrackEntity> = allTracks.value, index: Int = 0) {
        playbackService?.playTrack(track, queue, index)
    }

    fun togglePlayPause() {
        playbackService?.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackService?.seekTo(positionMs)
    }

    fun skipToNext() {
        playbackService?.skipToNext()
    }

    fun skipToPrevious() {
        playbackService?.skipToPrevious()
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        playbackService?.setRepeatMode(next)
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        playbackService?.setShuffle(next)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        playbackService?.setPlaybackSpeed(speed)
    }

    // ==========================================
    // ADVANCED PLAY QUEUE ACTIONS
    // ==========================================

    fun playNext(track: TrackEntity) {
        playbackService?.playNext(track)
        _statusMessage.value = "« ${track.title} » sera joué juste après"
    }

    fun addToQueue(track: TrackEntity) {
        playbackService?.addToQueue(track)
        _statusMessage.value = "« ${track.title} » ajouté à la file d'attente"
    }

    fun playQueueItem(index: Int) {
        playbackService?.playQueueItem(index)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        playbackService?.reorderQueue(fromIndex, toIndex)
    }

    fun removeFromQueue(index: Int) {
        playbackService?.removeFromQueue(index)
    }

    fun clearQueueExceptCurrent() {
        playbackService?.clearQueueExceptCurrent()
        _statusMessage.value = "File d'attente vidée"
    }

    fun saveQueueAsPlaylist(playlistTitle: String) {
        viewModelScope.launch {
            val q = playQueue.value
            if (q.isEmpty()) return@launch
            val uid = _currentUserId.value
            val playlistId = repository.createPlaylist(playlistTitle, "Sauvegarde de file d'attente (${q.size} titres)", 2, uid)
            q.forEach { track ->
                repository.addTrackToPlaylist(playlistId, track.id)
            }
            _statusMessage.value = "Playlist « $playlistTitle » créée avec ${q.size} morceaux"
        }
    }

    fun setBackgroundMode(mode: BackgroundMode) {
        _backgroundMode.value = mode
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
        }
    }

    fun openFullPlayer() {
        _isFullPlayerOpen.value = true
    }

    fun closeFullPlayer() {
        _isFullPlayerOpen.value = false
    }

    fun toggleLyrics() {
        _isLyricsOpen.value = !_isLyricsOpen.value
    }

    fun openCreatePlaylistDialog() {
        _isCreatePlaylistDialogOpen.value = true
    }

    fun closeCreatePlaylistDialog() {
        _isCreatePlaylistDialogOpen.value = false
    }

    fun openAddToPlaylistDialog(track: TrackEntity) {
        _trackForPlaylistAction.value = track
        _isAddToPlaylistDialogOpen.value = true
    }

    fun closeAddToPlaylistDialog() {
        _isAddToPlaylistDialogOpen.value = false
        _trackForPlaylistAction.value = null
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
        if (playlist != null) {
            viewModelScope.launch {
                repository.getTracksForPlaylist(playlist.id, _currentUserId.value).collect {
                    _selectedPlaylistTracks.value = it
                }
            }
        } else {
            _selectedPlaylistTracks.value = emptyList()
        }
    }

    fun createPlaylist(name: String, description: String, gradientIndex: Int) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isNotBlank()) {
                repository.createPlaylist(cleanName, description.trim(), gradientIndex, _currentUserId.value)
                _statusMessage.value = "Playlist « $cleanName » créée"
                closeCreatePlaylistDialog()
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            if (_selectedPlaylist.value?.id == playlist.id) {
                _selectedPlaylist.value = null
            }
            _statusMessage.value = "Playlist supprimée"
        }
    }

    fun openProfileOrAuth() {
        _isProfileAuthDialogOpen.value = true
    }

    fun closeProfileOrAuth() {
        _isProfileAuthDialogOpen.value = false
    }

    fun exportCloudData() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                _userProfile.value = _userProfile.value.copy(lastCloudBackupDate = now)

                val exportText = buildString {
                    appendLine("=== SLM PLAY BACKUP & METADATA EXPORT ===")
                    appendLine("Date: ${java.util.Date(now)}")
                    appendLine("Utilisateur: ${_userProfile.value.username.ifBlank { "Invité" }} (ID: ${_currentUserId.value})")
                    appendLine("Total Pistes: ${allTracks.value.size}")
                    appendLine("Favoris: ${favoriteTracks.value.size}")
                    appendLine("Playlists (${allPlaylists.value.size}):")
                    allPlaylists.value.forEach { pl ->
                        appendLine(" - ${pl.name} (${pl.description})")
                    }
                    appendLine("Réglages Equalizer: ${_appSettings.value.selectedEqPreset}")
                    appendLine("=========================================")
                }

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "SLM_Play_Cloud_Export_${System.currentTimeMillis()}.txt")
                    putExtra(Intent.EXTRA_TEXT, exportText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                getApplication<Application>().startActivity(Intent.createChooser(intent, "Sauvegarder les données").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })

                _statusMessage.value = "Export des données préparé"
            } catch (e: Exception) {
                _statusMessage.value = "Exportation terminée"
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            _statusMessage.value = "Morceau ajouté à la playlist"
            closeAddToPlaylistDialog()
        }
    }

    fun addMultipleTracksToPlaylist(playlistId: String, trackIds: List<String>) {
        viewModelScope.launch {
            val addedCount = repository.addTracksToPlaylist(playlistId, trackIds)
            _statusMessage.value = if (addedCount > 0) "$addedCount morceau(x) ajouté(s)" else "Tous les morceaux sont déjà présents"
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            _statusMessage.value = "Morceau retiré de la playlist"
        }
    }

    fun removeMultipleTracksFromPlaylist(playlistId: String, trackIds: List<String>) {
        viewModelScope.launch {
            repository.removeTracksFromPlaylist(playlistId, trackIds)
            _statusMessage.value = "${trackIds.size} morceau(x) retiré(s) de la playlist"
        }
    }

    fun deleteTrack(trackId: String) {
        viewModelScope.launch {
            repository.deleteTrack(trackId)
            _statusMessage.value = "Morceau supprimé"
        }
    }

    fun scanLocalMedia() {
        viewModelScope.launch {
            _statusMessage.value = "Scan des musiques locales en cours..."
            val count = repository.scanDeviceAudioFiles(_currentUserId.value)
            _statusMessage.value = if (count > 0) "$count morceaux ajoutés à votre espace !" else "Aucun fichier audio détecté"
        }
    }

    fun importAudioFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _statusMessage.value = "Importation de ${uris.size} fichiers..."
            val count = repository.importAudioUris(uris, _currentUserId.value)
            _statusMessage.value = "$count morceau(x) importé(s) dans votre espace"
        }
    }

    fun selectVibe(vibe: SlmVibe) {
        _activeVibe.value = vibe
        val matchingTracks = allTracks.value.filter {
            it.genre.contains(vibe.title.split(" ").first(), ignoreCase = true) ||
                    it.genre.contains(vibe.id.substringAfter("vibe_"), ignoreCase = true)
        }.ifEmpty { allTracks.value }

        if (matchingTracks.isNotEmpty()) {
            playTrack(matchingTracks.first(), matchingTracks, 0)
            _statusMessage.value = "Ambiance « ${vibe.title} » activée"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // ================= REAL CLOUD CONNECTION & AUTH =================
    fun openCloudHub() {
        _isCloudHubOpen.value = true
    }

    fun closeCloudHub() {
        _isCloudHubOpen.value = false
    }

    fun performCloudSync() {
        cloudSyncManager.performCloudSync { success, message ->
            _statusMessage.value = message
        }
    }

    fun createBackupSnapshot() {
        cloudSyncManager.createCloudBackupSnapshot { snapshot ->
            _statusMessage.value = "Sauvegarde Cloud créée : ${snapshot.title}"
        }
    }

    fun createCloudBackup() = createBackupSnapshot()

    fun restoreBackupSnapshot(snapshot: CloudBackupSnapshot) {
        cloudSyncManager.restoreCloudBackupSnapshot(snapshot) { success, message ->
            _statusMessage.value = message
        }
    }

    fun restoreCloudBackup(snapshot: CloudBackupSnapshot) = restoreBackupSnapshot(snapshot)

    fun deleteBackupSnapshot(snapshotId: String) {
        cloudSyncManager.deleteCloudBackupSnapshot(snapshotId)
    }

    fun deleteCloudBackup(snapshotId: String) = deleteBackupSnapshot(snapshotId)

    fun toggleAutoSync(enabled: Boolean) {
        cloudSyncManager.toggleAutoSync(enabled)
    }

    fun loginCloudAccount(usernameOrEmail: String, pass: String, onResult: ((AuthResult) -> Unit)? = null) {
        cloudSyncManager.loginCloudAccount(usernameOrEmail, pass) { result ->
            when (result) {
                is AuthResult.Success -> {
                    _statusMessage.value = result.message
                    refreshUserSession(result.account.cloudUserId)
                }
                is AuthResult.Error -> {
                    _statusMessage.value = result.errorMessage
                }
            }
            onResult?.invoke(result)
        }
    }

    fun registerCloudAccount(username: String, email: String, pass: String, onResult: ((AuthResult) -> Unit)? = null) {
        cloudSyncManager.registerCloudAccount(username, email, pass) { result ->
            when (result) {
                is AuthResult.Success -> {
                    _statusMessage.value = result.message
                    refreshUserSession(result.account.cloudUserId)
                }
                is AuthResult.Error -> {
                    _statusMessage.value = result.errorMessage
                }
            }
            onResult?.invoke(result)
        }
    }

    fun loginWithOAuth(
        provider: String,
        displayName: String,
        email: String,
        avatarUrl: String? = null,
        onResult: ((AuthResult) -> Unit)? = null
    ) {
        cloudSyncManager.loginWithOAuth(provider, displayName, email, avatarUrl) { result ->
            when (result) {
                is AuthResult.Success -> {
                    _statusMessage.value = result.message
                    refreshUserSession(result.account.cloudUserId)
                }
                is AuthResult.Error -> {
                    _statusMessage.value = result.errorMessage
                }
            }
            onResult?.invoke(result)
        }
    }

    fun switchCloudAccount(cloudUserId: String) {
        cloudSyncManager.switchCloudAccount(cloudUserId)
        refreshUserSession(cloudUserId)
    }

    fun updateCloudProfile(username: String, avatarUri: String?) {
        cloudSyncManager.updateCloudProfile(username, avatarUri)
    }

    fun logoutCloudAccount() {
        cloudSyncManager.logoutCloudAccount()
        refreshUserSession("guest")
        _statusMessage.value = "Déconnexion réussie. Session sécurisée révoquée."
    }

    fun deleteCloudAccount(cloudUserId: String, hardDelete: Boolean = true) {
        cloudSyncManager.deleteCloudAccount(cloudUserId, hardDelete) { msg ->
            _statusMessage.value = msg
            refreshUserSession("guest")
        }
    }

    // ================= ALBUM ACTIONS & PLAYBACK =================
    private fun groupTracksIntoAlbums(trackList: List<TrackEntity>): List<AlbumModel> {
        if (trackList.isEmpty()) return emptyList()

        val grouped = trackList.groupBy { track ->
            val albumName = track.album.trim()
            if (albumName.isBlank() || albumName.equals("Inconnu", ignoreCase = true) || albumName.equals("Unknown", ignoreCase = true)) {
                "Singles & Inédits"
            } else {
                albumName
            }
        }

        return grouped.entries.mapIndexed { index, (albumTitle, tracks) ->
            val firstTrackWithCover = tracks.firstOrNull { it.coverUri != null } ?: tracks.firstOrNull()
            val representativeArtist = if (tracks.map { it.artist }.distinct().size > 1) {
                "Artistes Divers"
            } else {
                tracks.firstOrNull()?.artist ?: "Artiste SLM"
            }

            AlbumModel(
                id = "album_" + Math.abs(albumTitle.hashCode()).toString(),
                title = albumTitle,
                artist = representativeArtist,
                year = tracks.firstOrNull()?.year ?: 2024,
                genre = tracks.firstOrNull()?.genre ?: "Mix Audio SLM",
                coverUri = firstTrackWithCover?.coverUri,
                coverResName = firstTrackWithCover?.coverResName ?: "slm_logo",
                tracks = tracks,
                gradientIndex = index % 5
            )
        }
    }

    fun openAlbumDetail(album: AlbumModel) {
        _selectedAlbum.value = album
        _isAlbumDetailOpen.value = true
    }

    fun closeAlbumDetail() {
        _isAlbumDetailOpen.value = false
        _selectedAlbum.value = null
    }

    fun playAlbum(album: AlbumModel, shuffle: Boolean = false) {
        if (album.tracks.isEmpty()) return
        val queue = if (shuffle) album.tracks.shuffled() else album.tracks
        playTrack(queue.first(), queue, 0)
        _statusMessage.value = "Lecture de l'album « ${album.title} »"
    }

    fun shuffleAlbum(album: AlbumModel) {
        playAlbum(album, shuffle = true)
    }

    fun addAlbumToQueue(album: AlbumModel) {
        album.tracks.forEach { track ->
            playbackService?.addToQueue(track)
        }
        _statusMessage.value = "${album.trackCount} titres de l'album ajoutés à la file d'attente"
    }

    fun createPlaylistFromAlbum(album: AlbumModel) {
        viewModelScope.launch {
            val uid = _currentUserId.value
            val playlistId = repository.createPlaylist(album.title, "Album par ${album.artist}", album.gradientIndex, uid)
            val trackIds = album.tracks.map { it.id }
            repository.addTracksToPlaylist(playlistId, trackIds)
            _statusMessage.value = "Playlist « ${album.title} » créée avec ${trackIds.size} morceaux"
        }
    }

    // ================= FULLSCREEN BROWSER ACTIONS =================
    fun addBrowserBookmark(title: String, url: String) {
        cloudSyncManager.addBookmark(title, url)
        _statusMessage.value = "Signet « $title » ajouté et synchronisé au Cloud"
    }

    fun removeBrowserBookmark(bookmarkId: String) {
        cloudSyncManager.removeBookmark(bookmarkId)
    }

    fun addBrowserHistoryItem(title: String, url: String) {
        cloudSyncManager.addHistoryItem(title, url)
    }

    fun clearBrowserHistory() {
        cloudSyncManager.clearHistory()
    }

    fun updateBrowserTabs(tabs: List<BrowserTab>) {
        _browserTabs.value = tabs
    }

    fun setActiveBrowserTab(tabId: String) {
        _activeBrowserTabId.value = tabId
    }

    fun setBrowserUrlInput(url: String) {
        _browserUrlInput.value = url
    }

    fun setBrowserDesktopMode(desktop: Boolean) {
        _isBrowserDesktopMode.value = desktop
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                // ignore
            }
            isBound = false
        }
    }
}
