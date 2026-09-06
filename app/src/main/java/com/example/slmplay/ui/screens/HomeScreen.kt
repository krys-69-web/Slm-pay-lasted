package com.example.slmplay.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.slmplay.data.db.PlaylistEntity
import com.example.slmplay.data.db.TrackEntity
import com.example.slmplay.data.model.*
import com.example.slmplay.ui.components.*
import com.example.slmplay.utils.DynamicArtworkExtractor
import com.example.slmplay.utils.UniversalDownloadManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    tracks: List<TrackEntity>,
    allTracksRaw: List<TrackEntity>,
    playlists: List<PlaylistEntity>,
    favoriteTracks: List<TrackEntity>,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    playbackSpeed: Float,
    backgroundMode: BackgroundMode,
    searchQuery: String,
    appSettings: AppSettings,
    dynamicArtworkPalette: DynamicArtworkExtractor.ArtworkPalette,
    audioAmplitudes: FloatArray,
    isSettingsOpen: Boolean,
    isMediaConverterOpen: Boolean,
    isAudioEditorOpen: Boolean,
    isEqualizerOpen: Boolean,
    isArtworkStudioOpen: Boolean,
    isFullscreenVisualizerOpen: Boolean,
    editingTrackForStudio: TrackEntity?,
    isFullPlayerOpen: Boolean,
    isLyricsOpen: Boolean,
    isCreatePlaylistOpen: Boolean,
    isAddToPlaylistOpen: Boolean,
    trackForPlaylistAction: TrackEntity?,
    selectedPlaylist: PlaylistEntity?,
    selectedPlaylistTracks: List<TrackEntity>,
    activeVibe: SlmVibe?,
    presetVibes: List<SlmVibe>,
    statusMessage: String?,
    isAudioPermissionGranted: Boolean,
    isNotificationPermissionGranted: Boolean,
    // Cloud Hub & Auth state
    cloudAccount: CloudAccount?,
    activeSession: AuthSession? = null,
    savedCloudAccounts: List<CloudAccount>,
    cloudBackups: List<CloudBackupSnapshot>,
    cloudConnectionStatus: CloudConnectionStatus,
    browserBookmarks: List<BrowserBookmark>,
    browserHistory: List<BrowserHistoryItem>,
    isAutoSyncEnabled: Boolean,
    lastSyncTimeFormatted: String,
    isCloudHubOpen: Boolean,
    onOpenCloudHub: () -> Unit,
    onCloseCloudHub: () -> Unit,
    onPerformCloudSync: () -> Unit,
    onCreateBackupSnapshot: () -> Unit,
    onRestoreBackupSnapshot: (CloudBackupSnapshot) -> Unit,
    onDeleteBackupSnapshot: (String) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onLoginCloud: (String, String, (AuthResult) -> Unit) -> Unit = { _, _, _ -> },
    onRegisterCloud: (String, String, String, (AuthResult) -> Unit) -> Unit = { _, _, _, _ -> },
    onOAuthLogin: (String, String, String, String?, (AuthResult) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onSwitchCloudAccount: (String) -> Unit,
    onUpdateCloudProfile: (String, String?) -> Unit,
    onLogoutCloud: () -> Unit,
    onDeleteCloudAccount: (String, Boolean) -> Unit = { _, _ -> },
    onAddBrowserBookmark: (String, String) -> Unit,
    onRemoveBrowserBookmark: (String) -> Unit,
    onAddBrowserHistoryItem: (String, String) -> Unit,
    onClearBrowserHistory: () -> Unit,
    // Persistent browser state
    browserTabs: List<BrowserTab> = listOf(BrowserTab(id = "tab_1", title = "Page d'accueil", url = "about:blank")),
    activeBrowserTabId: String = "tab_1",
    browserUrlInput: String = "",
    isBrowserDesktopMode: Boolean = false,
    onUpdateBrowserTabs: (List<BrowserTab>) -> Unit = {},
    onSetActiveBrowserTab: (String) -> Unit = {},
    onUpdateBrowserUrlInput: (String) -> Unit = {},
    onToggleBrowserDesktopMode: (Boolean) -> Unit = {},
    // Albums state
    allAlbums: List<AlbumModel> = emptyList(),
    selectedAlbum: AlbumModel? = null,
    isAlbumDetailOpen: Boolean = false,
    onOpenAlbumDetail: (AlbumModel) -> Unit = {},
    onCloseAlbumDetail: () -> Unit = {},
    onPlayAlbum: (AlbumModel) -> Unit = {},
    onShuffleAlbum: (AlbumModel) -> Unit = {},
    onAddAlbumToQueue: (AlbumModel) -> Unit = {},
    onCreatePlaylistFromAlbum: (AlbumModel) -> Unit = {},
    // Legacy user profile support (backward compatibility)
    userProfile: UserProfile,
    isProfileAuthDialogOpen: Boolean,
    onOpenProfileOrAuth: () -> Unit,
    onCloseProfileOrAuth: () -> Unit,
    onLoginUser: (String, String) -> Unit,
    onRegisterUser: (String, String) -> Unit,
    onUpdateProfile: (String, String?) -> Unit,
    onExportCloudData: () -> Unit,
    onLogoutUser: () -> Unit,
    onDeleteUserAccount: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetBackgroundMode: (BackgroundMode) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onOpenFullPlayer: () -> Unit,
    onCloseFullPlayer: () -> Unit,
    onToggleLyrics: () -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onCloseCreatePlaylist: () -> Unit,
    onOpenAddToPlaylist: (TrackEntity) -> Unit,
    onCloseAddToPlaylist: () -> Unit,
    onSelectPlaylist: (PlaylistEntity?) -> Unit,
    onCreatePlaylist: (String, String, Int) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onAddMultipleTracksToPlaylist: (String, List<String>) -> Unit,
    onRemoveFromPlaylist: (String, String) -> Unit,
    onRemoveMultipleTracksFromPlaylist: (String, List<String>) -> Unit,
    onDeleteTrack: (String) -> Unit,
    onScanMedia: () -> Unit,
    onImportFiles: (List<Uri>) -> Unit,
    onSelectVibe: (SlmVibe) -> Unit,
    onClearStatusMessage: () -> Unit,
    // 13 Features handlers
    onUpdateSettings: (AppSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenMediaConverter: () -> Unit,
    onCloseMediaConverter: () -> Unit,
    onOpenAudioEditor: (TrackEntity?) -> Unit,
    onCloseAudioEditor: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onCloseEqualizer: () -> Unit,
    onOpenArtworkStudio: (TrackEntity?) -> Unit,
    onCloseArtworkStudio: () -> Unit,
    onOpenFullscreenVisualizer: () -> Unit,
    onCloseFullscreenVisualizer: () -> Unit,
    onSelectEqPreset: (String) -> Unit,
    onSetEqBand: (Int, Float) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetVirtualizer: (Float) -> Unit,
    onSetVisualizerType: (VisualizerType) -> Unit,
    onSetLibrarySort: (LibrarySortOption) -> Unit,
    onStartSleepTimer: (Int, Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onAddConvertedTrack: (TrackEntity) -> Unit,
    onSaveTrackMetadata: (String, String, String, String, String, Int, String?) -> Unit,
    onRestoreTrackMetadata: (String) -> Unit,
    // Queue parameters
    isQueueOpen: Boolean = false,
    playQueue: List<TrackEntity> = emptyList(),
    currentQueueIndex: Int = -1,
    onOpenQueue: () -> Unit = {},
    onCloseQueue: () -> Unit = {},
    onPlayNext: (TrackEntity) -> Unit = {},
    onAddToQueue: (TrackEntity) -> Unit = {},
    onPlayQueueItem: (Int) -> Unit = {},
    onReorderQueue: (Int, Int) -> Unit = { _, _ -> },
    onRemoveFromQueue: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onSaveQueueAsPlaylist: (String) -> Unit = {},
    downloadManager: UniversalDownloadManager? = null,
    onOpenPrivateSpace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showBgMenu by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val activeAccentColor = if (appSettings.isDynamicArtworkEnabled) {
        dynamicArtworkPalette.dominantColor
    } else {
        AppleCrimson
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            onImportFiles(uris)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearStatusMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Dynamic Ambient Background
        DynamicVisualizerBackground(
            mode = backgroundMode,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // Main Content or Playlist Detail View
        if (selectedPlaylist != null) {
            PlaylistDetailScreen(
                playlist = selectedPlaylist,
                tracks = selectedPlaylistTracks,
                allTracks = allTracksRaw,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onBack = { onSelectPlaylist(null) },
                onPlayTrack = onPlayTrack,
                onPlayAll = {
                    if (selectedPlaylistTracks.isNotEmpty()) {
                        onPlayTrack(selectedPlaylistTracks.first(), selectedPlaylistTracks, 0)
                    }
                },
                onShuffleAll = {
                    if (selectedPlaylistTracks.isNotEmpty()) {
                        val shuffled = selectedPlaylistTracks.shuffled()
                        onPlayTrack(shuffled.first(), shuffled, 0)
                    }
                },
                onToggleFavorite = onToggleFavorite,
                onRemoveTrack = onRemoveFromPlaylist,
                onRemoveMultipleTracks = onRemoveMultipleTracksFromPlaylist,
                onAddTracksToPlaylist = onAddMultipleTracksToPlaylist,
                onDeletePlaylist = onDeletePlaylist
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Global Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Interactive Profile / Cloud button (tap opens Cloud Hub, 3-second hold triggers Private Space)
                    var isPressingLogo by remember { mutableStateOf(false) }
                    var pressProgress by remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(isPressingLogo) {
                        if (isPressingLogo) {
                            val startTime = System.currentTimeMillis()
                            while (isPressingLogo) {
                                val elapsed = System.currentTimeMillis() - startTime
                                pressProgress = (elapsed / 3000f).coerceIn(0f, 1f)
                                if (elapsed >= 3000) {
                                    isPressingLogo = false
                                    pressProgress = 0f
                                    onOpenPrivateSpace()
                                    break
                                }
                                kotlinx.coroutines.delay(40)
                            }
                        } else {
                            pressProgress = 0f
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onOpenCloudHub() },
                                    onPress = {
                                        isPressingLogo = true
                                        tryAwaitRelease()
                                        isPressingLogo = false
                                    }
                                )
                            }
                            .padding(4.dp)
                            .testTag("home_profile_logo_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = if (pressProgress > 0f) Color(0xFF5E5CE6) else activeAccentColor.copy(alpha = 0.6f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(
                                    if (pressProgress > 0f) (2.5).dp else 1.dp,
                                    if (pressProgress > 0f) Color(0xFF5E5CE6) else if (cloudAccount != null) AppleCrimson else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            if (cloudAccount?.avatarUri != null) {
                                AsyncImage(
                                    model = cloudAccount.avatarUri,
                                    contentDescription = "Avatar profil Cloud",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.slm_logo),
                                    contentDescription = "Logo SLM Play",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Private Space hold visual feedback
                            if (pressProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF5E5CE6).copy(alpha = 0.45f * pressProgress)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Cloud status dot indicator
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(Color(cloudConnectionStatus.colorHex))
                                    .border(1.5.dp, Color.Black, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (pressProgress > 0f) "Espace Privé..." else (cloudAccount?.username ?: "SLM Cloud"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pressProgress > 0f) Color(0xFF5E5CE6) else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (pressProgress > 0f) Icons.Default.Shield else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (pressProgress > 0f) Color(0xFF5E5CE6) else Color(cloudConnectionStatus.colorHex),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pressProgress > 0f) "Maintenez 3s (${(pressProgress * 100).toInt()}%)" else cloudConnectionStatus.label,
                                    fontSize = 11.sp,
                                    color = if (pressProgress > 0f) Color(0xFF5E5CE6) else Color(cloudConnectionStatus.colorHex),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Action icons deck
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cloud Sync Quick Trigger Button
                        IconButton(
                            onClick = onPerformCloudSync,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkGlassCard)
                                .testTag("home_cloud_sync_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Synchroniser avec le Cloud",
                                tint = if (cloudAccount != null) AppleCrimson else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Background Mode Menu
                        Box {
                            IconButton(
                                onClick = { showBgMenu = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkGlassCard)
                                    .testTag("home_bg_mode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = "Fond",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showBgMenu,
                                onDismissRequest = { showBgMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                Text(
                                    text = "Arrière-plan Dynamique",
                                    color = activeAccentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                BackgroundMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = mode.displayName,
                                                    color = if (backgroundMode == mode) activeAccentColor else TextPrimary,
                                                    fontWeight = if (backgroundMode == mode) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = mode.description,
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSetBackgroundMode(mode)
                                            showBgMenu = false
                                        },
                                        trailingIcon = if (backgroundMode == mode) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = activeAccentColor) }
                                        } else null
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Settings Button
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkGlassCard)
                                .testTag("home_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Apple-Glass Segmented Pager Switcher Tabs (3 Tabs: Musique, Navigateur Plein Écran, Studio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = DarkGlassCard,
                        borderColor = GlassBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Tab 0: Musique
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (pagerState.currentPage == 0) activeAccentColor else Color.Transparent
                                    )
                                    .clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (pagerState.currentPage == 0) Color.White else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Musique",
                                        color = if (pagerState.currentPage == 0) Color.White else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            // Tab 1: Navigateur Web
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (pagerState.currentPage == 1) activeAccentColor else Color.Transparent
                                    )
                                    .clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (pagerState.currentPage == 1) Color.White else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Web",
                                        color = if (pagerState.currentPage == 1) Color.White else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            // Tab 2: Studio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (pagerState.currentPage == 2) activeAccentColor else Color.Transparent
                                    )
                                    .clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = if (pagerState.currentPage == 2) Color.White else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Studio",
                                        color = if (pagerState.currentPage == 2) Color.White else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            // Tab 3: Fonctions à venir
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (pagerState.currentPage == 3) activeAccentColor else Color.Transparent
                                    )
                                    .clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (pagerState.currentPage == 3) Color.White else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "À venir",
                                        color = if (pagerState.currentPage == 3) Color.White else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (pagerState.currentPage == 3) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Horizontal Pager: Page 0 = Accueil / Bibliothèque, Page 1 = Web, Page 2 = Studio, Page 3 = À venir
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { pageIndex ->
                    if (pageIndex == 0) {
                        // ================= PAGE 0 : ACCUEIL & BIBLIOTHÈQUE =================
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Search Bar
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { Text("Rechercher un morceau, un artiste, un album...", color = TextSecondary.copy(alpha = 0.6f), fontSize = 14.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = "Recherche", tint = TextSecondary)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkGlassCard,
                                        unfocusedContainerColor = DarkGlassCard,
                                        focusedBorderColor = activeAccentColor,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .testTag("home_search_input")
                                )
                            }

                            // Smart Library Sorting Chips
                            if (appSettings.isSmartLibraryEnabled) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(LibrarySortOption.values()) { opt ->
                                            val isSelected = appSettings.librarySortOption == opt
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) activeAccentColor.copy(alpha = 0.35f) else DarkGlassCard)
                                                    .border(1.dp, if (isSelected) activeAccentColor else GlassBorder, RoundedCornerShape(12.dp))
                                                    .clickable { onSetLibrarySort(opt) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = opt.displayName,
                                                    color = if (isSelected) activeAccentColor else TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Audio Permissions Prompt Card
                            if (!isAudioPermissionGranted) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        backgroundColor = DarkGlassElevated,
                                        borderColor = activeAccentColor.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(activeAccentColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderOpen,
                                                    contentDescription = null,
                                                    tint = activeAccentColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Autoriser l'accès aux musiques",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Synchronisez vos fichiers MP3, FLAC & M4A locaux.",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            GlassButton(
                                                onClick = onRequestAudioPermission,
                                                isPrimary = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.testTag("home_grant_perm_btn")
                                            ) {
                                                Text("Autoriser", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            // Hero Banner: Quick Play & Actions
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_hero_card"),
                                    shape = RoundedCornerShape(24.dp),
                                    backgroundColor = DarkGlassElevated,
                                    borderColor = activeAccentColor.copy(alpha = 0.3f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Expérience Audio Suprême",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activeAccentColor,
                                                    letterSpacing = 1.sp
                                                )
                                                Text(
                                                    text = if (currentTrack != null) currentTrack.title else "Bienvenue sur SLM Play",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (currentTrack != null) "${currentTrack.artist} • ${currentTrack.genre}" else "${tracks.size} titres prêts pour l'écoute",
                                                    fontSize = 13.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Quick Play / Pause Button
                                            IconButton(
                                                onClick = {
                                                    if (currentTrack != null) {
                                                        onTogglePlayPause()
                                                    } else if (tracks.isNotEmpty()) {
                                                        onPlayTrack(tracks.first(), tracks, 0)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .shadow(12.dp, CircleShape, spotColor = activeAccentColor.copy(alpha = 0.6f))
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(listOf(activeAccentColor, ApplePurple))
                                                    )
                                                    .testTag("home_hero_play_btn")
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Lecture",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Quick Buttons: Import Media, Scan Device, Create Playlist
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            GlassButton(
                                                onClick = { filePickerLauncher.launch(arrayOf("audio/*", "video/*")) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Importer", fontSize = 12.sp, color = TextPrimary)
                                                }
                                            }

                                            GlassButton(
                                                onClick = onScanMedia,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Scanner", fontSize = 12.sp, color = TextPrimary)
                                                }
                                            }

                                            GlassButton(
                                                onClick = onOpenCreatePlaylist,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Playlist", fontSize = 12.sp, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // SLM Vibe Section
                            item {
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SLM VIBE • AMBIANCE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )

                                    if (activeVibe != null) {
                                        Text(
                                            text = "Actif : ${activeVibe.title}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = activeAccentColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(presetVibes, key = { it.id }) { vibe ->
                                        val isCurrentVibe = activeVibe?.id == vibe.id

                                        GlassCard(
                                            modifier = Modifier
                                                .width(135.dp)
                                                .height(82.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            backgroundColor = if (isCurrentVibe) activeAccentColor.copy(alpha = 0.3f) else DarkGlassCard,
                                            borderColor = if (isCurrentVibe) activeAccentColor else GlassBorder,
                                            onClick = { onSelectVibe(vibe) }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = vibe.emoji,
                                                        fontSize = 20.sp
                                                    )

                                                    if (isCurrentVibe) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = activeAccentColor,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = vibe.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Playlists Section
                            item {
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VOS PLAYLISTS (${playlists.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )

                                    IconButton(
                                        onClick = onOpenCreatePlaylist,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Nouvelle playlist", tint = activeAccentColor, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(playlists, key = { it.id }) { playlist ->
                                        val gradient = NeonMeshGradients.getOrElse(playlist.gradientIndex % NeonMeshGradients.size) { NeonMeshGradients[0] }

                                        GlassCard(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .clickable { onSelectPlaylist(playlist) },
                                            shape = RoundedCornerShape(20.dp),
                                            backgroundColor = DarkGlassCard,
                                            borderColor = GlassBorder,
                                            onClick = { onSelectPlaylist(playlist) }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(116.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(Brush.linearGradient(gradient)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.QueueMusic,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(42.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Text(
                                                    text = playlist.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = playlist.description,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Automatic Albums Section (Auto-generated from imported tracks)
                            if (allAlbums.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ALBUMS AUTOMATIQUES (${allAlbums.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextTertiary,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )

                                        Text(
                                            text = "Généré par métadonnées",
                                            fontSize = 11.sp,
                                            color = activeAccentColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(allAlbums, key = { it.id }) { album ->
                                            val gradient = NeonMeshGradients.getOrElse(album.gradientIndex % NeonMeshGradients.size) { NeonMeshGradients[0] }

                                            GlassCard(
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .clickable { onOpenAlbumDetail(album) }
                                                    .testTag("album_card_${album.id}"),
                                                shape = RoundedCornerShape(20.dp),
                                                backgroundColor = DarkGlassCard,
                                                borderColor = GlassBorder,
                                                onClick = { onOpenAlbumDetail(album) }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(116.dp)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(Brush.linearGradient(gradient)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (album.coverUri != null) {
                                                            AsyncImage(
                                                                model = album.coverUri,
                                                                contentDescription = album.title,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.Album,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(46.dp)
                                                            )
                                                        }

                                                        // Track count tag badge
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.Black.copy(alpha = 0.65f))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${album.trackCount} titres",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Text(
                                                        text = album.title,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Text(
                                                        text = "${album.artist} • ${album.year}",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Songs Library Section Header
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BIBLIOTHÈQUE (${tracks.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = onScanMedia) {
                                            Text("Actualiser", color = activeAccentColor, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Tracks List
                            if (tracks.isEmpty()) {
                                item {
                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        backgroundColor = DarkGlassCard
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicOff,
                                                contentDescription = null,
                                                tint = TextTertiary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Aucun morceau trouvé",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Importez des fichiers audio/vidéo ou lancez un scan.",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            GlassButton(
                                                onClick = { filePickerLauncher.launch(arrayOf("audio/*", "video/*")) },
                                                isPrimary = true,
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text("Importer des fichiers", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                    val isCurrentPlaying = currentTrack?.id == track.id
                                    var trackMenuOpen by remember { mutableStateOf(false) }

                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .testTag("track_item_${track.id}"),
                                        shape = RoundedCornerShape(18.dp),
                                        backgroundColor = if (isCurrentPlaying) activeAccentColor.copy(alpha = 0.22f) else DarkGlassCard,
                                        borderColor = if (isCurrentPlaying) activeAccentColor else GlassBorder,
                                        onClick = { onPlayTrack(track, tracks, index) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Index or Playing indicator
                                            Text(
                                                text = if (isCurrentPlaying && isPlaying) "▶" else "${index + 1}",
                                                color = if (isCurrentPlaying) activeAccentColor else TextTertiary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(26.dp)
                                            )

                                            // Artwork Thumbnail
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF1E1E2C))
                                            ) {
                                                if (track.coverUri != null) {
                                                    AsyncImage(
                                                        model = track.coverUri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(46.dp)
                                                    )
                                                } else {
                                                    val resId = when (track.coverResName) {
                                                        "cover_neon" -> R.drawable.cover_neon
                                                        "cover_ambient" -> R.drawable.cover_ambient
                                                        else -> R.drawable.slm_logo
                                                    }
                                                    Image(
                                                        painter = painterResource(id = resId),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(46.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Title & Artist
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title,
                                                    color = if (isCurrentPlaying) activeAccentColor else TextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${track.artist} • ${track.album}",
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Favorite Toggle Button
                                            IconButton(
                                                onClick = { onToggleFavorite(track) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Favori",
                                                    tint = if (track.isFavorite) AppleCrimson else TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // More Options Popup Menu
                                            Box {
                                                IconButton(
                                                    onClick = { trackMenuOpen = true },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = trackMenuOpen,
                                                    onDismissRequest = { trackMenuOpen = false },
                                                    modifier = Modifier.background(DarkSurface)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Écouter juste après", color = TextPrimary) },
                                                        onClick = {
                                                            trackMenuOpen = false
                                                            onPlayNext(track)
                                                        },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.QueuePlayNext, contentDescription = null, tint = AppleCrimson)
                                                        }
                                                    )

                                                    DropdownMenuItem(
                                                        text = { Text("Ajouter à la file d'attente", color = TextPrimary) },
                                                        onClick = {
                                                            trackMenuOpen = false
                                                            onAddToQueue(track)
                                                        },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = AppleCrimson)
                                                        }
                                                    )

                                                    DropdownMenuItem(
                                                        text = { Text("Ajouter à une playlist", color = TextPrimary) },
                                                        onClick = {
                                                            trackMenuOpen = false
                                                            onOpenAddToPlaylist(track)
                                                        },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = ApplePurple)
                                                        }
                                                    )

                                                    DropdownMenuItem(
                                                        text = { Text("Éditer l'audio (Rogner / Fondus)", color = TextPrimary) },
                                                        onClick = {
                                                            trackMenuOpen = false
                                                            onOpenAudioEditor(track)
                                                        },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = AppleTeal)
                                                        }
                                                    )

                                                    DropdownMenuItem(
                                                        text = { Text("Studio de pochette & tags", color = TextPrimary) },
                                                        onClick = {
                                                            trackMenuOpen = false
                                                            onOpenArtworkStudio(track)
                                                        },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.Image, contentDescription = null, tint = ApplePink)
                                                        }
                                                    )

                                                    DropdownMenuItem(
                                                        text = { Text("Supprimer de la bibliothèque", color = AppleCrimson) },
                                                        onClick = {
                                                            trackMenuOpen = false
                                                            onDeleteTrack(track.id)
                                                        },
                                                        leadingIcon = {
                                                            Icon(Icons.Default.Delete, contentDescription = null, tint = AppleCrimson)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(110.dp))
                            }
                        }
                    } else if (pageIndex == 1) {
                        // ================= PAGE 1 : NAVIGATEUR WEB PLEIN ÉCRAN =================
                        FullscreenBrowserScreen(
                            bookmarks = browserBookmarks,
                            history = browserHistory,
                            onAddBookmark = onAddBrowserBookmark,
                            onRemoveBookmark = onRemoveBrowserBookmark,
                            onAddHistoryItem = onAddBrowserHistoryItem,
                            onClearHistory = onClearBrowserHistory,
                            onOpenCloudHub = onOpenCloudHub,
                            tabs = browserTabs,
                            activeTabId = activeBrowserTabId,
                            urlInput = browserUrlInput,
                            isDesktopMode = isBrowserDesktopMode,
                            onUpdateTabs = onUpdateBrowserTabs,
                            onSetActiveTab = onSetActiveBrowserTab,
                            onUpdateUrlInput = onUpdateBrowserUrlInput,
                            onToggleDesktopMode = onToggleBrowserDesktopMode,
                            downloadManager = downloadManager,
                            onOpenPrivateSpace = onOpenPrivateSpace,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (pageIndex == 2) {
                        // ================= PAGE 2 : STUDIO & OUTILS MULTIMÉDIA =================
                        AdditionalOptionsScreen(
                            appSettings = appSettings,
                            onOpenMediaConverter = onOpenMediaConverter,
                            onOpenAudioEditor = { onOpenAudioEditor(currentTrack) },
                            onOpenArtworkStudio = { onOpenArtworkStudio(currentTrack) },
                            onOpenEqualizer = onOpenEqualizer,
                            onOpenVisualizerFullscreen = onOpenFullscreenVisualizer,
                            onOpenSettings = onOpenSettings,
                            onSelectVisualizerType = onSetVisualizerType
                        )
                    } else {
                        // ================= PAGE 3 : FONCTIONS À VENIR =================
                        UpcomingFeaturesScreen(
                            accentColor = activeAccentColor,
                            onOpenPrivateSpace = onOpenPrivateSpace,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Floating MiniPlayer Dock
        MiniPlayer(
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext,
            onExpand = onOpenFullPlayer,
            onOpenQueue = onOpenQueue,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Sliding Full Screen Player
        FullPlayerSheet(
            isOpen = isFullPlayerOpen,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            playbackSpeed = playbackSpeed,
            backgroundMode = backgroundMode,
            isLyricsOpen = isLyricsOpen,
            accentColor = activeAccentColor,
            audioAmplitudes = audioAmplitudes,
            visualizerType = appSettings.visualizerType,
            onClose = onCloseFullPlayer,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onToggleRepeat = onToggleRepeat,
            onToggleShuffle = onToggleShuffle,
            onSetSpeed = onSetSpeed,
            onSetBackgroundMode = onSetBackgroundMode,
            onToggleFavorite = onToggleFavorite,
            onToggleLyrics = onToggleLyrics,
            onOpenEqualizer = onOpenEqualizer,
            onOpenFullscreenVisualizer = onOpenFullscreenVisualizer,
            onOpenArtworkStudio = { onOpenArtworkStudio(currentTrack) },
            onOpenQueue = onOpenQueue
        )

        // Advanced Queue Sheet Dialog
        QueueSheetDialog(
            isOpen = isQueueOpen,
            queue = playQueue,
            currentIndex = currentQueueIndex,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            onDismiss = onCloseQueue,
            onPlayTrackAt = onPlayQueueItem,
            onReorder = onReorderQueue,
            onRemove = onRemoveFromQueue,
            onClearQueue = onClearQueue,
            onSaveAsPlaylist = onSaveQueueAsPlaylist
        )

        // Real Cloud Hub Dialog (Account connection, Cloud sync, snapshots & backup restore)
        CloudHubDialog(
            isOpen = isCloudHubOpen,
            cloudAccount = cloudAccount,
            activeSession = activeSession,
            savedAccounts = savedCloudAccounts,
            connectionStatus = cloudConnectionStatus,
            cloudBackups = cloudBackups,
            isAutoSyncEnabled = isAutoSyncEnabled,
            lastSyncTimeFormatted = lastSyncTimeFormatted,
            onDismiss = onCloseCloudHub,
            onPerformCloudSync = onPerformCloudSync,
            onCreateBackupSnapshot = onCreateBackupSnapshot,
            onRestoreBackupSnapshot = onRestoreBackupSnapshot,
            onDeleteBackupSnapshot = onDeleteBackupSnapshot,
            onToggleAutoSync = onToggleAutoSync,
            onLogin = onLoginCloud,
            onRegister = onRegisterCloud,
            onOAuthLogin = onOAuthLogin,
            onSwitchAccount = onSwitchCloudAccount,
            onUpdateProfile = onUpdateCloudProfile,
            onLogout = onLogoutCloud,
            onDeleteAccount = onDeleteCloudAccount
        )

        // Automatic Album Detail Dialog
        AlbumDetailDialog(
            isOpen = isAlbumDetailOpen,
            album = selectedAlbum,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            onDismiss = onCloseAlbumDetail,
            onPlayTrack = onPlayTrack,
            onPlayAlbum = onPlayAlbum,
            onShuffleAlbum = onShuffleAlbum,
            onAddAlbumToQueue = onAddAlbumToQueue,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onCreatePlaylistFromAlbum = onCreatePlaylistFromAlbum
        )

        // Legacy Profile Dialog fallback (if triggered directly)
        ProfileAndAuthDialog(
            isOpen = isProfileAuthDialogOpen,
            userProfile = userProfile,
            onDismiss = onCloseProfileOrAuth,
            onLogin = onLoginUser,
            onRegister = onRegisterUser,
            onUpdateProfile = onUpdateProfile,
            onExportCloudData = onExportCloudData,
            onLogout = onLogoutUser,
            onDeleteAccount = onDeleteUserAccount
        )

        // 1. Convertisseur multimédia Dialog
        MediaConverterDialog(
            isOpen = isMediaConverterOpen,
            tracks = tracks,
            onDismiss = onCloseMediaConverter,
            onTrackConverted = onAddConvertedTrack
        )

        // 2. SLM Audio Editor Dialog
        AudioEditorDialog(
            isOpen = isAudioEditorOpen,
            tracks = tracks,
            initialTrack = editingTrackForStudio,
            onDismiss = onCloseAudioEditor,
            onTrackExported = onAddConvertedTrack
        )

        // 3. SLM Equalizer Dialog
        EqualizerDialog(
            isOpen = isEqualizerOpen,
            isEnabled = appSettings.isEqualizerEnabled,
            currentPreset = appSettings.selectedEqPreset,
            manualBands = appSettings.manualBands,
            bassBoost = appSettings.bassBoostStrength,
            virtualizer = appSettings.virtualizerStrength,
            onDismiss = onCloseEqualizer,
            onToggleEnabled = { onUpdateSettings(appSettings.copy(isEqualizerEnabled = it)) },
            onSelectPreset = onSelectEqPreset,
            onBandChanged = onSetEqBand,
            onBassBoostChanged = onSetBassBoost,
            onVirtualizerChanged = onSetVirtualizer
        )

        // 4. SLM Fullscreen Visualizer Dialog
        FullscreenVisualizerDialog(
            isOpen = isFullscreenVisualizerOpen,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            amplitudes = audioAmplitudes,
            selectedVisualizer = appSettings.visualizerType,
            accentColor = activeAccentColor,
            onSelectVisualizer = onSetVisualizerType,
            onDismiss = onCloseFullscreenVisualizer
        )

        // 7. Artwork Studio Dialog
        ArtworkStudioDialog(
            isOpen = isArtworkStudioOpen,
            track = editingTrackForStudio,
            onDismiss = onCloseArtworkStudio,
            onSaveMetadata = onSaveTrackMetadata,
            onRestoreOriginal = onRestoreTrackMetadata
        )

        // Main Settings Dialog (Control center for all 13 features)
        SettingsScreenDialog(
            isOpen = isSettingsOpen,
            settings = appSettings,
            onDismiss = onCloseSettings,
            onUpdateSettings = onUpdateSettings,
            onOpenMediaConverter = {
                onCloseSettings()
                onOpenMediaConverter()
            },
            onOpenAudioEditor = {
                onCloseSettings()
                onOpenAudioEditor(currentTrack)
            },
            onOpenEqualizer = {
                onCloseSettings()
                onOpenEqualizer()
            },
            onOpenArtworkStudio = {
                onCloseSettings()
                onOpenArtworkStudio(currentTrack)
            },
            onOpenFullscreenVisualizer = {
                onCloseSettings()
                onOpenFullscreenVisualizer()
            },
            onStartSleepTimer = onStartSleepTimer,
            onCancelSleepTimer = onCancelSleepTimer
        )

        // Create Playlist Dialog
        CreatePlaylistDialog(
            isOpen = isCreatePlaylistOpen,
            onDismiss = onCloseCreatePlaylist,
            onCreate = onCreatePlaylist
        )

        // Add to Playlist Dialog
        AddToPlaylistDialog(
            isOpen = isAddToPlaylistOpen,
            track = trackForPlaylistAction,
            playlists = playlists,
            onDismiss = onCloseAddToPlaylist,
            onAddToPlaylist = onAddToPlaylist,
            onCreateNewPlaylistClick = {
                onCloseAddToPlaylist()
                onOpenCreatePlaylist()
            }
        )

        // Floating Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (currentTrack != null) 90.dp else 24.dp)
        )
    }
}
