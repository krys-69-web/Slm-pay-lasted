package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slmplay.ui.screens.HomeScreen
import com.example.slmplay.ui.screens.PinLockScreen
import com.example.slmplay.ui.screens.PrivateSpaceScreen
import com.example.slmplay.ui.viewmodel.MusicViewModel
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkCanvas
                ) {
                    MainScreenContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("open_private_space", false) == true) {
            viewModel.openPinLockForPrivateSpace()
        }
    }
}

@Composable
fun MainScreenContent(viewModel: MusicViewModel) {
    val tracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val allTracksRaw by viewModel.allTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val backgroundMode by viewModel.backgroundMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val dynamicArtworkPalette by viewModel.dynamicArtworkPalette.collectAsStateWithLifecycle()
    val audioAmplitudes by viewModel.audioAmplitudes.collectAsStateWithLifecycle()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isMediaConverterOpen by viewModel.isMediaConverterOpen.collectAsStateWithLifecycle()
    val isAudioEditorOpen by viewModel.isAudioEditorOpen.collectAsStateWithLifecycle()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsStateWithLifecycle()
    val isArtworkStudioOpen by viewModel.isArtworkStudioOpen.collectAsStateWithLifecycle()
    val isFullscreenVisualizerOpen by viewModel.isFullscreenVisualizerOpen.collectAsStateWithLifecycle()
    val editingTrackForStudio by viewModel.editingTrackForStudio.collectAsStateWithLifecycle()

    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsStateWithLifecycle()
    val isLyricsOpen by viewModel.isLyricsOpen.collectAsStateWithLifecycle()
    val isCreatePlaylistOpen by viewModel.isCreatePlaylistDialogOpen.collectAsStateWithLifecycle()
    val isAddToPlaylistOpen by viewModel.isAddToPlaylistDialogOpen.collectAsStateWithLifecycle()
    val trackForPlaylistAction by viewModel.trackForPlaylistAction.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()
    val activeVibe by viewModel.activeVibe.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Profile & Auth (Legacy)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isProfileAuthDialogOpen by viewModel.isProfileAuthDialogOpen.collectAsStateWithLifecycle()

    // Real Cloud Hub & Browser States
    val cloudAccount by viewModel.cloudAccount.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val savedCloudAccounts by viewModel.savedCloudAccounts.collectAsStateWithLifecycle()
    val cloudBackups by viewModel.cloudBackups.collectAsStateWithLifecycle()
    val cloudConnectionStatus by viewModel.cloudConnectionStatus.collectAsStateWithLifecycle()
    val browserBookmarks by viewModel.browserBookmarks.collectAsStateWithLifecycle()
    val browserHistory by viewModel.browserHistory.collectAsStateWithLifecycle()
    val browserTabs by viewModel.browserTabs.collectAsStateWithLifecycle()
    val activeBrowserTabId by viewModel.activeBrowserTabId.collectAsStateWithLifecycle()
    val browserUrlInput by viewModel.browserUrlInput.collectAsStateWithLifecycle()
    val isBrowserDesktopMode by viewModel.isBrowserDesktopMode.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val lastSyncTimeFormatted by viewModel.lastSyncTimeFormatted.collectAsStateWithLifecycle()
    val isCloudHubOpen by viewModel.isCloudHubOpen.collectAsStateWithLifecycle()

    // Automatic Albums States
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val isAlbumDetailOpen by viewModel.isAlbumDetailOpen.collectAsStateWithLifecycle()

    // Queue States
    val isQueueOpen by viewModel.isQueueOpen.collectAsStateWithLifecycle()
    val playQueue by viewModel.playQueue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()

    // Private Space & Security States
    val isPrivateSpaceActive by viewModel.isPrivateSpaceActive.collectAsStateWithLifecycle()
    val isPinLockScreenVisible by viewModel.isPinLockScreenVisible.collectAsStateWithLifecycle()
    val isAppLaunchLocked by viewModel.isAppLaunchLocked.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Permission states
    var isAudioPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var isNotificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isAudioPermissionGranted = granted
        if (granted) {
            viewModel.scanLocalMedia()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationPermissionGranted = granted
    }

    HomeScreen(
        tracks = tracks,
        allTracksRaw = allTracksRaw,
        playlists = playlists,
        favoriteTracks = favoriteTracks,
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        repeatMode = repeatMode,
        isShuffle = isShuffle,
        playbackSpeed = playbackSpeed,
        backgroundMode = backgroundMode,
        searchQuery = searchQuery,
        appSettings = appSettings,
        dynamicArtworkPalette = dynamicArtworkPalette,
        audioAmplitudes = audioAmplitudes,
        isSettingsOpen = isSettingsOpen,
        isMediaConverterOpen = isMediaConverterOpen,
        isAudioEditorOpen = isAudioEditorOpen,
        isEqualizerOpen = isEqualizerOpen,
        isArtworkStudioOpen = isArtworkStudioOpen,
        isFullscreenVisualizerOpen = isFullscreenVisualizerOpen,
        editingTrackForStudio = editingTrackForStudio,
        isFullPlayerOpen = isFullPlayerOpen,
        isLyricsOpen = isLyricsOpen,
        isCreatePlaylistOpen = isCreatePlaylistOpen,
        isAddToPlaylistOpen = isAddToPlaylistOpen,
        trackForPlaylistAction = trackForPlaylistAction,
        selectedPlaylist = selectedPlaylist,
        selectedPlaylistTracks = selectedPlaylistTracks,
        activeVibe = activeVibe,
        presetVibes = viewModel.presetVibes,
        statusMessage = statusMessage,
        isAudioPermissionGranted = isAudioPermissionGranted,
        isNotificationPermissionGranted = isNotificationPermissionGranted,
        // Cloud Hub & Browser
        cloudAccount = cloudAccount,
        activeSession = activeSession,
        savedCloudAccounts = savedCloudAccounts,
        cloudBackups = cloudBackups,
        cloudConnectionStatus = cloudConnectionStatus,
        browserBookmarks = browserBookmarks,
        browserHistory = browserHistory,
        isAutoSyncEnabled = isAutoSyncEnabled,
        lastSyncTimeFormatted = lastSyncTimeFormatted,
        isCloudHubOpen = isCloudHubOpen,
        onOpenCloudHub = viewModel::openCloudHub,
        onCloseCloudHub = viewModel::closeCloudHub,
        onPerformCloudSync = viewModel::performCloudSync,
        onCreateBackupSnapshot = viewModel::createBackupSnapshot,
        onRestoreBackupSnapshot = viewModel::restoreBackupSnapshot,
        onDeleteBackupSnapshot = viewModel::deleteBackupSnapshot,
        onToggleAutoSync = viewModel::toggleAutoSync,
        onLoginCloud = { user, pass, cb -> viewModel.loginCloudAccount(user, pass, cb) },
        onRegisterCloud = { user, email, pass, cb -> viewModel.registerCloudAccount(user, email, pass, cb) },
        onOAuthLogin = { prov, name, email, avatar, cb -> viewModel.loginWithOAuth(prov, name, email, avatar, cb) },
        onSwitchCloudAccount = viewModel::switchCloudAccount,
        onUpdateCloudProfile = viewModel::updateCloudProfile,
        onLogoutCloud = viewModel::logoutCloudAccount,
        onDeleteCloudAccount = { uid, hard -> viewModel.deleteCloudAccount(uid, hard) },
        onAddBrowserBookmark = viewModel::addBrowserBookmark,
        onRemoveBrowserBookmark = viewModel::removeBrowserBookmark,
        onAddBrowserHistoryItem = viewModel::addBrowserHistoryItem,
        onClearBrowserHistory = viewModel::clearBrowserHistory,
        // Persistent browser session
        browserTabs = browserTabs,
        activeBrowserTabId = activeBrowserTabId,
        browserUrlInput = browserUrlInput,
        isBrowserDesktopMode = isBrowserDesktopMode,
        onUpdateBrowserTabs = viewModel::updateBrowserTabs,
        onSetActiveBrowserTab = viewModel::setActiveBrowserTab,
        onUpdateBrowserUrlInput = viewModel::setBrowserUrlInput,
        onToggleBrowserDesktopMode = viewModel::setBrowserDesktopMode,
        // Automatic Albums
        allAlbums = allAlbums,
        selectedAlbum = selectedAlbum,
        isAlbumDetailOpen = isAlbumDetailOpen,
        onOpenAlbumDetail = viewModel::openAlbumDetail,
        onCloseAlbumDetail = viewModel::closeAlbumDetail,
        onPlayAlbum = viewModel::playAlbum,
        onShuffleAlbum = viewModel::shuffleAlbum,
        onAddAlbumToQueue = viewModel::addAlbumToQueue,
        onCreatePlaylistFromAlbum = viewModel::createPlaylistFromAlbum,
        // Legacy Profile Dialog
        userProfile = userProfile,
        isProfileAuthDialogOpen = isProfileAuthDialogOpen,
        onOpenProfileOrAuth = viewModel::openCloudHub,
        onCloseProfileOrAuth = viewModel::closeProfileOrAuth,
        onLoginUser = viewModel::loginUser,
        onRegisterUser = viewModel::registerUser,
        onUpdateProfile = viewModel::updateProfile,
        onExportCloudData = viewModel::exportCloudData,
        onLogoutUser = viewModel::logoutUser,
        onDeleteUserAccount = viewModel::deleteUserAccount,
        onRequestAudioPermission = {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            audioPermissionLauncher.launch(perm)
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onSearchQueryChange = viewModel::setSearchQuery,
        onPlayTrack = viewModel::playTrack,
        onTogglePlayPause = viewModel::togglePlayPause,
        onSeekTo = viewModel::seekTo,
        onSkipNext = viewModel::skipToNext,
        onSkipPrevious = viewModel::skipToPrevious,
        onToggleRepeat = viewModel::toggleRepeatMode,
        onToggleShuffle = viewModel::toggleShuffle,
        onSetSpeed = viewModel::setPlaybackSpeed,
        onSetBackgroundMode = viewModel::setBackgroundMode,
        onToggleFavorite = viewModel::toggleFavorite,
        onOpenFullPlayer = viewModel::openFullPlayer,
        onCloseFullPlayer = viewModel::closeFullPlayer,
        onToggleLyrics = viewModel::toggleLyrics,
        onOpenCreatePlaylist = viewModel::openCreatePlaylistDialog,
        onCloseCreatePlaylist = viewModel::closeCreatePlaylistDialog,
        onOpenAddToPlaylist = viewModel::openAddToPlaylistDialog,
        onCloseAddToPlaylist = viewModel::closeAddToPlaylistDialog,
        onSelectPlaylist = viewModel::selectPlaylist,
        onCreatePlaylist = viewModel::createPlaylist,
        onDeletePlaylist = viewModel::deletePlaylist,
        onAddToPlaylist = viewModel::addTrackToPlaylist,
        onAddMultipleTracksToPlaylist = viewModel::addMultipleTracksToPlaylist,
        onRemoveFromPlaylist = viewModel::removeTrackFromPlaylist,
        onRemoveMultipleTracksFromPlaylist = viewModel::removeMultipleTracksFromPlaylist,
        onDeleteTrack = viewModel::deleteTrack,
        onScanMedia = viewModel::scanLocalMedia,
        onImportFiles = viewModel::importAudioFiles,
        onSelectVibe = viewModel::selectVibe,
        onClearStatusMessage = viewModel::clearStatusMessage,
        // 13 Features handlers
        onUpdateSettings = viewModel::updateSettings,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
        onOpenMediaConverter = viewModel::openMediaConverter,
        onCloseMediaConverter = viewModel::closeMediaConverter,
        onOpenAudioEditor = viewModel::openAudioEditor,
        onCloseAudioEditor = viewModel::closeAudioEditor,
        onOpenEqualizer = viewModel::openEqualizer,
        onCloseEqualizer = viewModel::closeEqualizer,
        onOpenArtworkStudio = viewModel::openArtworkStudio,
        onCloseArtworkStudio = viewModel::closeArtworkStudio,
        onOpenFullscreenVisualizer = viewModel::openFullscreenVisualizer,
        onCloseFullscreenVisualizer = viewModel::closeFullscreenVisualizer,
        onSelectEqPreset = viewModel::setEqualizerPreset,
        onSetEqBand = viewModel::setEqualizerBand,
        onSetBassBoost = viewModel::setBassBoost,
        onSetVirtualizer = viewModel::setVirtualizer,
        onSetVisualizerType = viewModel::setVisualizerType,
        onSetLibrarySort = viewModel::setLibrarySortOption,
        onStartSleepTimer = viewModel::startSleepTimer,
        onCancelSleepTimer = viewModel::cancelSleepTimer,
        onAddConvertedTrack = viewModel::addConvertedTrack,
        onSaveTrackMetadata = viewModel::saveTrackMetadata,
        onRestoreTrackMetadata = viewModel::restoreTrackMetadata,
        // Queue arguments
        isQueueOpen = isQueueOpen,
        playQueue = playQueue,
        currentQueueIndex = currentQueueIndex,
        onOpenQueue = viewModel::openQueue,
        onCloseQueue = viewModel::closeQueue,
        onPlayNext = viewModel::playNext,
        onAddToQueue = viewModel::addToQueue,
        onPlayQueueItem = viewModel::playQueueItem,
        onReorderQueue = viewModel::reorderQueue,
        onRemoveFromQueue = viewModel::removeFromQueue,
        onClearQueue = viewModel::clearQueueExceptCurrent,
        onSaveQueueAsPlaylist = viewModel::saveQueueAsPlaylist,
        downloadManager = viewModel.downloadManager,
        onOpenPrivateSpace = viewModel::openPinLockForPrivateSpace
    )

    // App Launch Lock Screen (if enabled in vault settings)
    if (isAppLaunchLocked) {
        PinLockScreen(
            securityVaultManager = viewModel.securityVaultManager,
            isForAppLaunch = true,
            onAuthSuccess = { isDecoy ->
                viewModel.onAppUnlockSuccess()
                if (isDecoy) {
                    viewModel.onPinAuthSuccess(isDecoy = true)
                }
            },
            onDismiss = { /* Remains until unlocked */ }
        )
    }

    // Security PIN Lock & Biometric Screen for Private Space access
    if (isPinLockScreenVisible) {
        PinLockScreen(
            securityVaultManager = viewModel.securityVaultManager,
            isForAppLaunch = false,
            onAuthSuccess = { isDecoy ->
                viewModel.onPinAuthSuccess(isDecoy)
            },
            onDismiss = viewModel::closePinLock
        )
    }

    // Isolated Private Space (Safe Browser, Encrypted Vault, Downloads, FLAG_SECURE)
    if (isPrivateSpaceActive) {
        PrivateSpaceScreen(
            securityVaultManager = viewModel.securityVaultManager,
            downloadManager = viewModel.downloadManager,
            onExit = viewModel::exitPrivateSpace
        )
    }
}
