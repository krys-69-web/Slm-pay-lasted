package com.example.slmplay.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.slmplay.data.model.BrowserBookmark
import com.example.slmplay.data.model.BrowserHistoryItem
import com.example.slmplay.data.model.BrowserTab
import com.example.slmplay.data.model.QuickSpeedDial
import com.example.slmplay.ui.components.GlassButton
import com.example.slmplay.ui.components.GlassCard
import com.example.slmplay.utils.SniffedMediaItem
import com.example.slmplay.utils.UniversalDownloadManager
import com.example.slmplay.utils.VaultFileType
import com.example.slmplay.utils.WebSessionManager
import com.example.ui.theme.*
import org.json.JSONArray
import java.util.UUID

private val speedDials = listOf(
    QuickSpeedDial("Google", "https://www.google.com", "🔍", "Recherche", 0xFF4285F4),
    QuickSpeedDial("YouTube", "https://www.youtube.com", "▶️", "Vidéo & Musique", 0xFFFF0000),
    QuickSpeedDial("SoundCloud", "https://soundcloud.com", "☁️", "Streaming Audio", 0xFFFF5500),
    QuickSpeedDial("Spotify Web", "https://open.spotify.com", "🎧", "Musique & Podcasts", 0xFF1DB954),
    QuickSpeedDial("Genius Paroles", "https://genius.com", "📜", "Paroles & Infos", 0xFFFFE600),
    QuickSpeedDial("Radio Garden", "https://radio.garden", "📻", "Radios du Monde", 0xFF00C7BE),
    QuickSpeedDial("Wikipedia", "https://fr.wikipedia.org", "📚", "Encyclopédie", 0xFF9E9E9E),
    QuickSpeedDial("Bandcamp", "https://bandcamp.com", "🎸", "Artistes Indépendants", 0xFF1DA0C3)
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullscreenBrowserScreen(
    bookmarks: List<BrowserBookmark>,
    history: List<BrowserHistoryItem>,
    onAddBookmark: (String, String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onAddHistoryItem: (String, String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenCloudHub: () -> Unit = {},
    tabs: List<BrowserTab> = listOf(BrowserTab(id = "tab_1", title = "Page d'accueil", url = "about:blank")),
    activeTabId: String = "tab_1",
    urlInput: String = "",
    isDesktopMode: Boolean = false,
    onUpdateTabs: (List<BrowserTab>) -> Unit = {},
    onSetActiveTab: (String) -> Unit = {},
    onUpdateUrlInput: (String) -> Unit = {},
    onToggleDesktopMode: (Boolean) -> Unit = {},
    downloadManager: UniversalDownloadManager? = null,
    onOpenPrivateSpace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: BrowserTab("tab_1", "Page d'accueil", "about:blank")

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isImmersiveFullscreen by remember { mutableStateOf(false) }
    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var showDownloaderSheet by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableIntStateOf(0) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Flush cookies and state when leaving screen or disposing
    DisposableEffect(Unit) {
        onDispose {
            WebSessionManager.flushSession()
        }
    }

    // Handle Android hardware/gesture back press
    BackHandler(enabled = isImmersiveFullscreen || (webViewInstance?.canGoBack() == true && currentTab.url != "about:blank")) {
        if (isImmersiveFullscreen) {
            isImmersiveFullscreen = false
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        }
    }

    fun loadUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val formattedUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }

        onUpdateUrlInput(formattedUrl)
        val updatedTabs = tabs.map {
            if (it.id == activeTabId) it.copy(url = formattedUrl, title = formattedUrl) else it
        }
        onUpdateTabs(updatedTabs)
        webViewInstance?.loadUrl(formattedUrl)
        focusManager.clearFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP ADDRESS & NAVIGATION TOOLBAR (Hides in Immersive Fullscreen)
            AnimatedVisibility(
                visible = !isImmersiveFullscreen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkGlassElevated)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Navigation controls + URL Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Back Button
                        IconButton(
                            onClick = { webViewInstance?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Page précédente",
                                tint = if (canGoBack) TextPrimary else TextTertiary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Forward Button
                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Page suivante",
                                tint = if (canGoForward) TextPrimary else TextTertiary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // URL / Search Input Bar
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { onUpdateUrlInput(it) },
                            placeholder = {
                                Text(
                                    "Rechercher ou entrer une URL...",
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (currentTab.url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (currentTab.url.startsWith("https://")) Color(0xFF34C759) else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (urlInput.isNotBlank()) {
                                        IconButton(
                                            onClick = { onUpdateUrlInput("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (isLoadingPage) {
                                        IconButton(
                                            onClick = { webViewInstance?.stopLoading() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Arrêter", tint = AppleCrimson, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (currentTab.url != "about:blank") {
                                                    webViewInstance?.reload()
                                                } else {
                                                    loadUrl(urlInput)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { loadUrl(urlInput) }),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkGlassCard,
                                unfocusedContainerColor = DarkGlassCard,
                                focusedBorderColor = AppleCrimson,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("browser_url_input")
                        )

                        // Home Button
                        IconButton(
                            onClick = {
                                onUpdateUrlInput("")
                                val updatedTabs = tabs.map {
                                    if (it.id == activeTabId) it.copy(url = "about:blank", title = "Page d'accueil") else it
                                }
                                onUpdateTabs(updatedTabs)
                                webViewInstance?.loadUrl("about:blank")
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Accueil", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }

                        // Fullscreen Mode Toggle Button
                        IconButton(
                            onClick = { isImmersiveFullscreen = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppleCrimson.copy(alpha = 0.2f))
                                .testTag("browser_fullscreen_toggle_btn")
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Plein écran total", tint = AppleCrimson, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Secondary Quick Actions Row (Bookmark, Desktop mode, Tabs count, Bookmarks/History Hub, Cloud Sync)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Quick Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Bookmark this page
                            val isBookmarked = bookmarks.any { it.url == currentTab.url && it.url != "about:blank" }
                            AssistChip(
                                onClick = {
                                    if (currentTab.url != "about:blank") {
                                        if (isBookmarked) {
                                            val bm = bookmarks.find { it.url == currentTab.url }
                                            bm?.let { onRemoveBookmark(it.id) }
                                        } else {
                                            onAddBookmark(currentTab.title, currentTab.url)
                                        }
                                    }
                                },
                                label = { Text(if (isBookmarked) "Enregistré ⭐" else "Favori +", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (isBookmarked) Color(0xFFFFD60A) else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isBookmarked) Color(0xFFFFD60A).copy(alpha = 0.2f) else DarkGlassCard,
                                    labelColor = TextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Desktop Mode Toggle
                            AssistChip(
                                onClick = {
                                    val newMode = !isDesktopMode
                                    onToggleDesktopMode(newMode)
                                    webViewInstance?.let { wv ->
                                        val settings = wv.settings
                                        if (newMode) {
                                            settings.userAgentString =
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                        } else {
                                            settings.userAgentString = null
                                        }
                                        wv.reload()
                                    }
                                },
                                label = { Text(if (isDesktopMode) "Bureau 💻" else "Mobile 📱", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isDesktopMode) ApplePurple.copy(alpha = 0.25f) else DarkGlassCard,
                                    labelColor = TextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Signets & Historique
                            AssistChip(
                                onClick = { showBookmarksHistorySheet = true },
                                label = { Text("Signets", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Bookmarks, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(containerColor = DarkGlassCard, labelColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Universal Downloader & Media Sniffer Button
                            AssistChip(
                                onClick = {
                                    webViewInstance?.let { wv ->
                                        downloadManager?.let { dm ->
                                            wv.evaluateJavascript(dm.getMediaSnifferScript()) { jsonResult ->
                                                try {
                                                    val cleanJson = if (jsonResult.startsWith("\"") && jsonResult.endsWith("\"")) {
                                                        jsonResult.substring(1, jsonResult.length - 1)
                                                            .replace("\\\"", "\"")
                                                            .replace("\\\\", "\\")
                                                    } else {
                                                        jsonResult
                                                    }
                                                    val arr = JSONArray(cleanJson)
                                                    val list = mutableListOf<SniffedMediaItem>()
                                                    for (i in 0 until arr.length()) {
                                                        val obj = arr.getJSONObject(i)
                                                        val url = obj.getString("url")
                                                        val typeStr = obj.optString("type", "other")
                                                        val title = obj.optString("title", "Média Web")
                                                        val mediaType = when (typeStr) {
                                                            "video" -> VaultFileType.VIDEO
                                                            "audio" -> VaultFileType.AUDIO
                                                            "image" -> VaultFileType.IMAGE
                                                            "doc" -> VaultFileType.DOCUMENT
                                                            else -> VaultFileType.OTHER
                                                        }
                                                        list.add(
                                                            SniffedMediaItem(
                                                                url = url,
                                                                title = title,
                                                                extension = url.substringAfterLast(".").take(5),
                                                                mediaType = mediaType
                                                            )
                                                        )
                                                    }
                                                    dm.setSniffedMedia(list)
                                                } catch (_: Exception) {
                                                    dm.clearSniffedMedia()
                                                }
                                            }
                                        }
                                    }
                                    showDownloaderSheet = true
                                },
                                label = { Text("Télécharger", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AppleCrimson, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(containerColor = DarkGlassCard, labelColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Right side: Tabs Switcher + Cloud Hub Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Cloud Quick Sync
                            IconButton(
                                onClick = onOpenCloudHub,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(DarkGlassCard)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Cloud SLM", tint = AppleCrimson, modifier = Modifier.size(16.dp))
                            }

                            // Tabs Counter Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppleCrimson)
                                    .clickable { showTabsSheet = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${tabs.size} onglet${if (tabs.size > 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Loading Progress Indicator
                    if (isLoadingPage && currentProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { currentProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .padding(top = 4.dp),
                            color = AppleCrimson,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }

            // WEBVIEW & SPEED DIAL HOMEPAGE CONTAINER (Occupies full space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // If url is about:blank, show the Speed Dial Home Hub
                if (currentTab.url == "about:blank") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title / Hero
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(listOf(AppleCrimson, ApplePurple))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Navigateur Web Plein Écran",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Navigation rapide, streaming multimédia & synchronisation Cloud",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Search Card
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = DarkGlassElevated,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Rechercher sur le Web", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = urlInput,
                                        onValueChange = { onUpdateUrlInput(it) },
                                        placeholder = { Text("Tapez une recherche ou une adresse...", color = TextSecondary, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppleCrimson) },
                                        trailingIcon = {
                                            if (urlInput.isNotBlank()) {
                                                IconButton(onClick = { loadUrl(urlInput) }) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Aller", tint = AppleCrimson)
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                        keyboardActions = KeyboardActions(onGo = { loadUrl(urlInput) }),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkGlassCard,
                                            unfocusedContainerColor = DarkGlassCard,
                                            focusedBorderColor = AppleCrimson,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Speed Dials Grid Section
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RACCROURIS RAPIDES & STREAMING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Speed Dials cards
                        items(speedDials.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { dial ->
                                    GlassCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { loadUrl(dial.url) },
                                        backgroundColor = DarkGlassElevated,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(dial.colorHex).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(dial.emoji, fontSize = 18.sp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(dial.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(dial.category, fontSize = 10.sp, color = TextSecondary)
                                            }
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // User's Cloud Bookmarks list
                        if (bookmarks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VOS FAVORIS SYNCHRONISÉS AU CLOUD (${bookmarks.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(bookmarks) { bm ->
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable { loadUrl(bm.url) },
                                    backgroundColor = DarkGlassCard,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(bm.iconEmoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bm.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                                            Text(bm.url, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(
                                            onClick = { onRemoveBookmark(bm.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Android WebView for actual web rendering
                    AndroidView(
                        factory = { ctx ->
                            val wv = WebSessionManager.getOrCreateWebView(ctx)

                            wv.webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoadingPage = true
                                    url?.let {
                                        onUpdateUrlInput(it)
                                        canGoBack = view?.canGoBack() ?: false
                                        canGoForward = view?.canGoForward() ?: false
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoadingPage = false
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                    val pageTitle = view?.title ?: url ?: "Page Web"
                                    url?.let { validUrl ->
                                        if (validUrl != "about:blank") {
                                            onAddHistoryItem(pageTitle, validUrl)
                                            val updated = tabs.map {
                                                if (it.id == activeTabId) it.copy(title = pageTitle, url = validUrl) else it
                                            }
                                            onUpdateTabs(updated)
                                        }
                                    }
                                    WebSessionManager.flushSession()
                                }
                            }

                            wv.webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    currentProgress = newProgress
                                    if (newProgress >= 100) {
                                        isLoadingPage = false
                                    }
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    title?.let { t ->
                                        val updated = tabs.map {
                                            if (it.id == activeTabId) it.copy(title = t) else it
                                        }
                                        onUpdateTabs(updated)
                                    }
                                }
                            }

                            // Intercept web downloads
                            wv.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                                downloadManager?.let { dm ->
                                    val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                    val mType = when {
                                        mimetype?.startsWith("audio/") == true -> VaultFileType.AUDIO
                                        mimetype?.startsWith("video/") == true -> VaultFileType.VIDEO
                                        mimetype?.startsWith("image/") == true -> VaultFileType.IMAGE
                                        else -> VaultFileType.DOCUMENT
                                    }
                                    dm.setSniffedMedia(
                                        listOf(
                                            SniffedMediaItem(
                                                url = url,
                                                title = filename,
                                                extension = filename.substringAfterLast(".").take(5),
                                                mediaType = mType
                                            )
                                        )
                                    )
                                    showDownloaderSheet = true
                                }
                            }

                            webViewInstance = wv

                            if (currentTab.url != "about:blank" && wv.url != currentTab.url) {
                                wv.loadUrl(currentTab.url)
                            }

                            wv
                        },
                        update = { view ->
                            // If url changed and not blank, load it
                            if (currentTab.url != "about:blank" && view.url != currentTab.url) {
                                view.loadUrl(currentTab.url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D0F15))
                    )
                }

                // FLOATING FULLSCREEN CONTROL HUD (Visible when Immersive Fullscreen is active)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isImmersiveFullscreen,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
                    ) {
                        GlassCard(
                            backgroundColor = DarkCanvas.copy(alpha = 0.9f),
                            borderColor = GlassBorder,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Back in history
                                IconButton(
                                    onClick = { webViewInstance?.goBack() },
                                    enabled = canGoBack,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = if (canGoBack) Color.White else TextTertiary, modifier = Modifier.size(16.dp))
                                }

                                // Reload
                                IconButton(
                                    onClick = { webViewInstance?.reload() },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Exit Fullscreen
                                IconButton(
                                    onClick = { isImmersiveFullscreen = false },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(AppleCrimson)
                                ) {
                                    Icon(Icons.Default.FullscreenExit, contentDescription = "Quitter le plein écran", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MULTI-TABS BOTTOM SHEET
    if (showTabsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTabsSheet = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Onglets ouverts (${tabs.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    GlassButton(
                        onClick = {
                            val newId = "tab_" + UUID.randomUUID().toString().take(6)
                            val newTab = BrowserTab(id = newId, title = "Nouvel onglet", url = "about:blank")
                            val updated = tabs + newTab
                            onUpdateTabs(updated)
                            onSetActiveTab(newId)
                            onUpdateUrlInput("")
                            webViewInstance?.loadUrl("about:blank")
                            showTabsSheet = false
                        },
                        isPrimary = true,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nouvel onglet", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        val isCurrent = tab.id == activeTabId
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetActiveTab(tab.id)
                                    val targetUrl = if (tab.url == "about:blank") "" else tab.url
                                    onUpdateUrlInput(targetUrl)
                                    webViewInstance?.loadUrl(tab.url)
                                    showTabsSheet = false
                                },
                            backgroundColor = if (isCurrent) AppleCrimson.copy(alpha = 0.25f) else DarkGlassCard,
                            borderColor = if (isCurrent) AppleCrimson else GlassBorder,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tab.url == "about:blank") Icons.Default.Home else Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isCurrent) AppleCrimson else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tab.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                                    Text(if (tab.url == "about:blank") "Accueil" else tab.url, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                }
                                if (tabs.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val updated = tabs.filter { it.id != tab.id }
                                            onUpdateTabs(updated)
                                            if (activeTabId == tab.id) {
                                                val nextTab = updated.first()
                                                onSetActiveTab(nextTab.id)
                                                val targetUrl = if (nextTab.url == "about:blank") "" else nextTab.url
                                                onUpdateUrlInput(targetUrl)
                                                webViewInstance?.loadUrl(nextTab.url)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Fermer onglet", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // SIGNETS & HISTORIQUE MODAL
    if (showBookmarksHistorySheet) {
        var selectedSubTab by remember { mutableIntStateOf(0) }

        ModalBottomSheet(
            onDismissRequest = { showBookmarksHistorySheet = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Tab Switcher between Signets & Historique
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGlassCard)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedSubTab == 0) AppleCrimson else Color.Transparent)
                            .clickable { selectedSubTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Signets Cloud (${bookmarks.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedSubTab == 1) AppleCrimson else Color.Transparent)
                            .clickable { selectedSubTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Historique (${history.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedSubTab == 0) {
                    // Signets list
                    if (bookmarks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun signet pour le moment.", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(bookmarks) { bm ->
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            loadUrl(bm.url)
                                            showBookmarksHistorySheet = false
                                        },
                                    backgroundColor = DarkGlassCard,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(bm.iconEmoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bm.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                                            Text(bm.url, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                        }
                                        IconButton(
                                            onClick = { onRemoveBookmark(bm.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Historique list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dernières visites", fontSize = 12.sp, color = TextSecondary)
                        if (history.isNotEmpty()) {
                            TextButton(onClick = onClearHistory) {
                                Text("Effacer", color = AppleCrimson, fontSize = 12.sp)
                            }
                        }
                    }
                    if (history.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                            Text("Historique vide.", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(history) { h ->
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            loadUrl(h.url)
                                            showBookmarksHistorySheet = false
                                        },
                                    backgroundColor = DarkGlassCard,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(h.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                                            Text(h.url, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Universal Downloader & Media Sniffer Sheet
    if (showDownloaderSheet && downloadManager != null) {
        UniversalDownloaderSheet(
            activeUrl = currentTab.url,
            pageTitle = currentTab.title,
            downloadManager = downloadManager,
            isPrivateSpaceContext = false,
            onDismiss = { showDownloaderSheet = false }
        )
    }
}
