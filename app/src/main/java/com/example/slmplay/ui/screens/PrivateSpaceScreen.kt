package com.example.slmplay.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.slmplay.data.model.BrowserHistoryItem
import com.example.slmplay.utils.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSpaceScreen(
    securityVaultManager: SecurityVaultManager,
    downloadManager: UniversalDownloadManager,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDecoyMode by securityVaultManager.isDecoyModeActive.collectAsState()
    val privateHistory by securityVaultManager.privateHistory.collectAsState()
    val vaultFiles by securityVaultManager.vaultFiles.collectAsState()

    // Enforce Anti-Capture Screen (FLAG_SECURE)
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val isEmulator = securityVaultManager.isRunningOnEmulator()
        val shouldProtect = securityVaultManager.isAntiScreenshotEnabled() && !isEmulator
        if (shouldProtect) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Handle back button to exit private space safely
    BackHandler {
        onExit()
    }

    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Navigateur, 1 = Coffre-fort, 2 = Sécurité
    var privateUrlInput by remember { mutableStateOf("https://duckduckgo.com") }
    var currentWebUrl by remember { mutableStateOf("https://duckduckgo.com") }
    var currentWebTitle by remember { mutableStateOf("SLM Furtif") }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isWebLoading by remember { mutableStateOf(false) }

    var showDownloaderSheet by remember { mutableStateOf(false) }
    var showPinChangeModal by remember { mutableStateOf(false) }
    var pinModalMode by remember { mutableStateOf(PinLockMode.SETUP_NEW) }

    // Preview/player for vault audio/media
    var previewMessage by remember { mutableStateOf<String?>(null) }

    val accent = Color(0xFF5E5CE6) // Deep Indigo for Private Space

    Scaffold(
        containerColor = Color(0xFF0A0B10),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1118))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.2f))
                                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isDecoyMode) "Espace Invité" else "SLM Space",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF34C759).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isDecoyMode) "Invité" else "Chiffré",
                                        color = Color(0xFF34C759),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Session privée isolée • Anti-capture actif",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Exit / Lock button
                    FilledTonalButton(
                        onClick = onExit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppleCrimson.copy(alpha = 0.2f),
                            contentColor = AppleCrimson
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verrouiller", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Switcher (Navigateur / Coffre-fort / Sécurité)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PrivateNavChip(
                        title = "Navigateur Furtif",
                        icon = Icons.Default.Language,
                        isSelected = selectedSection == 0,
                        accentColor = accent,
                        onClick = { selectedSection = 0 },
                        modifier = Modifier.weight(1.2f)
                    )
                    PrivateNavChip(
                        title = "Coffre-fort (${vaultFiles.size})",
                        icon = Icons.Default.FolderSpecial,
                        isSelected = selectedSection == 1,
                        accentColor = accent,
                        onClick = { selectedSection = 1 },
                        modifier = Modifier.weight(1.2f)
                    )
                    PrivateNavChip(
                        title = "Sécurité",
                        icon = Icons.Default.VpnKey,
                        isSelected = selectedSection == 2,
                        accentColor = accent,
                        onClick = { selectedSection = 2 },
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0A0B10))
        ) {
            when (selectedSection) {
                0 -> {
                    // ================= SECTION 0: NAVIGATEUR FURTIF =================
                    Column(modifier = Modifier.fillMaxSize()) {
                        // URL Bar & Tools
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF13151D))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = privateUrlInput,
                                onValueChange = { privateUrlInput = it },
                                placeholder = {
                                    Text("Recherche ou URL privée...", fontSize = 12.5.sp, color = TextSecondary)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = DarkGlassCard,
                                    unfocusedContainerColor = DarkGlassCard,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Load URL button
                            IconButton(
                                onClick = {
                                    var target = privateUrlInput.trim()
                                    if (target.isNotBlank()) {
                                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                            target = if (target.contains(".") && !target.contains(" ")) {
                                                "https://$target"
                                            } else {
                                                "https://duckduckgo.com/?q=${Uri.encode(target)}"
                                            }
                                        }
                                        currentWebUrl = target
                                        val wv = WebSessionManager.getCurrentPrivateWebView()
                                        wv?.loadUrl(target)
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Naviguer",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Sniffer / Downloader Button
                            IconButton(
                                onClick = {
                                    // Trigger media sniffer in private webview
                                    val wv = WebSessionManager.getCurrentPrivateWebView()
                                    if (wv != null) {
                                        wv.evaluateJavascript(downloadManager.getMediaSnifferScript()) { jsonResult ->
                                            try {
                                                val cleanJson = if (jsonResult.startsWith("\"") && jsonResult.endsWith("\"")) {
                                                    // Unescape stringified JSON from JS
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
                                                downloadManager.setSniffedMedia(list)
                                            } catch (e: Exception) {
                                                downloadManager.clearSniffedMedia()
                                            }
                                        }
                                    }
                                    showDownloaderSheet = true
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2F3E))
                                    .border(1.dp, GlassBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Téléchargeur",
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Progress Indicator
                        if (isWebLoading) {
                            LinearProgressIndicator(
                                progress = { webProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = accent,
                                trackColor = Color.Transparent
                            )
                        }

                        // Isolated Private WebView
                        AndroidView(
                            factory = { ctx ->
                                val wv = WebSessionManager.getOrCreatePrivateWebView(ctx)
                                wv.webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        webProgress = newProgress / 100f
                                        isWebLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) {
                                            currentWebTitle = title
                                        }
                                    }
                                }

                                wv.webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isWebLoading = true
                                        if (url != null) {
                                            privateUrlInput = url
                                            currentWebUrl = url
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isWebLoading = false
                                        if (url != null && !url.startsWith("about:") && !isDecoyMode) {
                                            securityVaultManager.addPrivateHistoryItem(
                                                title = currentWebTitle,
                                                url = url
                                            )
                                        }
                                    }
                                }

                                if (wv.url.isNullOrBlank() || wv.url == "about:blank") {
                                    wv.loadUrl(currentWebUrl)
                                }
                                wv
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                1 -> {
                    // ================= SECTION 1: COFFRE-FORT PRIVÉ =================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Fichiers Masqués du Coffre",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Stockage interne privé (.nomedia) • Invisible sur l'appareil",
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }

                            if (vaultFiles.isNotEmpty()) {
                                TextButton(
                                    onClick = { securityVaultManager.clearVault() }
                                ) {
                                    Text("Tout vider", color = Color(0xFFFF453A), fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (vaultFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = TextSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Le coffre-fort est vide",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Téléchargez des médias depuis le navigateur privé\nen choisissant 'Coffre-fort Privé'",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(vaultFiles, key = { it.path }) { fileItem ->
                                    VaultFileCard(
                                        item = fileItem,
                                        accentColor = accent,
                                        onOpen = {
                                            openVaultFile(context, fileItem.path)
                                        },
                                        onDelete = {
                                            securityVaultManager.deleteVaultFile(fileItem.path)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ================= SECTION 2: PARAMÈTRES SÉCURITÉ =================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Options de Sécurité & Confidentialité",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gérez vos codes de verrouillage, biométrie et protections d'écran",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // PIN Code Configuration Card
                        item {
                            SecurityActionCard(
                                title = "Modifier le Code PIN Principal",
                                subtitle = "Code maître à 4 ou 6 chiffres",
                                icon = Icons.Default.Pin,
                                onClick = {
                                    pinModalMode = PinLockMode.SETUP_NEW
                                    showPinChangeModal = true
                                }
                            )
                        }

                        // Decoy / Fake PIN (Désinformation)
                        item {
                            SecurityActionCard(
                                title = "Faux Code PIN (Désinformation)",
                                subtitle = if (securityVaultManager.isDecoyPinConfigured())
                                    "Actif • Ouvre un espace factice sous contrainte"
                                else "Désactivé • Touchez pour configurer",
                                icon = Icons.Default.VpnKeyOff,
                                onClick = {
                                    pinModalMode = PinLockMode.SETUP_DECOY
                                    showPinChangeModal = true
                                }
                            )
                        }

                        // Biometric & Face Lock Switch
                        item {
                            var bioEnabled by remember {
                                mutableStateOf(securityVaultManager.isBiometricEnabled())
                            }
                            SecurityToggleCard(
                                title = "Face Lock & Biométrie Mobile",
                                subtitle = "Reconnaissance faciale, Empreinte et Sécurité Système Android",
                                icon = Icons.Default.Face,
                                isChecked = bioEnabled,
                                onCheckedChange = {
                                    bioEnabled = it
                                    securityVaultManager.setBiometricEnabled(it)
                                }
                            )
                        }

                        // Lock on App Launch Switch
                        item {
                            var lockLaunch by remember {
                                mutableStateOf(securityVaultManager.isLockOnAppLaunchEnabled())
                            }
                            SecurityToggleCard(
                                title = "Verrouillage au Démarrage",
                                subtitle = "Demander le code PIN à chaque ouverture de SLM Play",
                                icon = Icons.Default.LockClock,
                                isChecked = lockLaunch,
                                onCheckedChange = {
                                    lockLaunch = it
                                    securityVaultManager.setLockOnAppLaunchEnabled(it)
                                }
                            )
                        }

                        // Auto Incognito Switch
                        item {
                            var autoIncognito by remember {
                                mutableStateOf(securityVaultManager.isAutoIncognitoEnabled())
                            }
                            SecurityToggleCard(
                                title = "Mode Incognito Automatique",
                                subtitle = "Effacer cache et cookies privés dès la fermeture de l'espace",
                                icon = Icons.Default.AutoDelete,
                                isChecked = autoIncognito,
                                onCheckedChange = {
                                    autoIncognito = it
                                    securityVaultManager.setAutoIncognitoEnabled(it)
                                }
                            )
                        }

                        // Anti-Screenshot (FLAG_SECURE) Switch
                        item {
                            var antiScreenshot by remember {
                                mutableStateOf(securityVaultManager.isAntiScreenshotEnabled())
                            }
                            SecurityToggleCard(
                                title = "Anti-Capture d'Écran (FLAG_SECURE)",
                                subtitle = "Bloque captures d'écran et prévisualisations multitâche",
                                icon = Icons.Default.VisibilityOff,
                                isChecked = antiScreenshot,
                                onCheckedChange = {
                                    antiScreenshot = it
                                    securityVaultManager.setAntiScreenshotEnabled(it)
                                    val window = (context as? Activity)?.window
                                    if (it) {
                                        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                    } else {
                                        window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                    }
                                }
                            )
                        }

                        // Clear Private History
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkGlassCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { securityVaultManager.clearPrivateHistory() }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = Color(0xFFFF453A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Purger l'Historique Privé Chiffré",
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${privateHistory.size} entrées privées",
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Downloader Bottom Sheet
    if (showDownloaderSheet) {
        UniversalDownloaderSheet(
            activeUrl = currentWebUrl,
            pageTitle = currentWebTitle,
            downloadManager = downloadManager,
            isPrivateSpaceContext = true,
            onDismiss = { showDownloaderSheet = false }
        )
    }

    // PIN Change Modal
    if (showPinChangeModal) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            PinLockScreen(
                securityVaultManager = securityVaultManager,
                onAuthSuccess = {
                    showPinChangeModal = false
                },
                onDismiss = { showPinChangeModal = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun openVaultFile(context: android.content.Context, filePath: String) {
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, UniversalDownloadManager.getMimeType(filePath))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback or internal preview
    }
}

@Composable
private fun PrivateNavChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VaultFileCard(
    item: VaultFileItem,
    accentColor: Color,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, color) = when (item.fileType) {
        VaultFileType.AUDIO -> Pair(Icons.Default.MusicNote, Color(0xFFFF2D55))
        VaultFileType.VIDEO -> Pair(Icons.Default.Videocam, Color(0xFFFF9500))
        VaultFileType.IMAGE -> Pair(Icons.Default.Image, Color(0xFF30B0C7))
        VaultFileType.DOCUMENT -> Pair(Icons.Default.Description, Color(0xFF5856D6))
        VaultFileType.OTHER -> Pair(Icons.Default.InsertDriveFile, Color.Gray)
    }

    val formattedSize = remember(item.sizeBytes) {
        when {
            item.sizeBytes > 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f Mo", item.sizeBytes / (1024.0 * 1024.0))
            item.sizeBytes > 1024 -> String.format(Locale.getDefault(), "%.1f Ko", item.sizeBytes / 1024.0)
            else -> "${item.sizeBytes} B"
        }
    }

    val formattedDate = remember(item.lastModified) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.lastModified))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkGlassCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedSize,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $formattedDate",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onOpen,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Ouvrir",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SecurityActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkGlassCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5E5CE6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF5E5CE6),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SecurityToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkGlassCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }
}
