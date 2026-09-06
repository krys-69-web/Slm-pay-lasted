package com.example.slmplay.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.slmplay.data.model.AuthResult
import com.example.slmplay.data.model.AuthSession
import com.example.slmplay.data.model.CloudAccount
import com.example.slmplay.data.model.CloudBackupSnapshot
import com.example.slmplay.data.model.CloudConnectionStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CloudHubDialog(
    isOpen: Boolean,
    cloudAccount: CloudAccount?,
    activeSession: AuthSession?,
    savedAccounts: List<CloudAccount>,
    cloudBackups: List<CloudBackupSnapshot>,
    connectionStatus: CloudConnectionStatus,
    isAutoSyncEnabled: Boolean,
    lastSyncTimeFormatted: String,
    onDismiss: () -> Unit,
    onPerformCloudSync: () -> Unit,
    onCreateBackupSnapshot: () -> Unit,
    onRestoreBackupSnapshot: (CloudBackupSnapshot) -> Unit,
    onDeleteBackupSnapshot: (String) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onLogin: (String, String, (AuthResult) -> Unit) -> Unit,
    onRegister: (String, String, String, (AuthResult) -> Unit) -> Unit,
    onOAuthLogin: (String, String, String, String?, (AuthResult) -> Unit) -> Unit,
    onSwitchAccount: (String) -> Unit,
    onUpdateProfile: (String, String?) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: (String, Boolean) -> Unit
) {
    if (!isOpen) return

    var activeTab by remember { mutableIntStateOf(0) } // 0: Profil & Synchro, 1: Sauvegardes, 2: Sécurité & Sessions, 3: Connexion / Inscription
    var isRegisterMode by remember { mutableStateOf(false) }
    var inputUsername by remember(cloudAccount?.username) { mutableStateOf(cloudAccount?.username ?: "") }
    var inputEmail by remember(cloudAccount?.email) { mutableStateOf(cloudAccount?.email ?: "") }
    var inputPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var authErrorText by remember { mutableStateOf<String?>(null) }
    var authSuccessText by remember { mutableStateOf<String?>(null) }

    var isEditingName by remember { mutableStateOf(false) }
    var isSyncingNow by remember { mutableStateOf(false) }
    var restoreConfirmSnapshot by remember { mutableStateOf<CloudBackupSnapshot?>(null) }
    var deleteAccountConfirmId by remember { mutableStateOf<String?>(null) }
    var deleteHardMode by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && cloudAccount != null) {
            onUpdateProfile(cloudAccount.username, uri.toString())
        }
    }

    val rotation = rememberInfiniteTransition(label = "sync_spin")
    val spinAngle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkCanvas)
                .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Cloud Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(AppleCrimson, ApplePurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SLM Cloud & Auth",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val isGuest = cloudAccount?.cloudUserId?.startsWith("cloud_guest") == true || cloudAccount == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!isGuest) Color(0xFF34C759).copy(alpha = 0.2f) else Color(0xFFFF9500).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (!isGuest) "Session Active 🟢" else "Mode Invité 🟡",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isGuest) Color(0xFF34C759) else Color(0xFFFF9500)
                                    )
                                }
                            }
                            Text(
                                text = "Serveur distant sécurisé • Jetons de session chiffrés",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkGlassCard)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkGlassCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabs = listOf(
                        "Profil & Sync" to 0,
                        "Sauvegardes" to 1,
                        "Sécurité & Jetons" to 2,
                        "Comptes" to 3
                    )
                    tabs.forEach { (label, idx) ->
                        val isSelected = activeTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppleCrimson else Color.Transparent)
                                .clickable {
                                    activeTab = idx
                                    authErrorText = null
                                    authSuccessText = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feedback Banners
                authErrorText?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF3B30).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(text = "⚠️ $err", color = Color(0xFFFF453A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                authSuccessText?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF34C759).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF34C759).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(text = "✅ $msg", color = Color(0xFF34C759), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // TAB 0: PROFIL & SYNCHRONISATION
                if (activeTab == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // User Profile Card
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = DarkGlassElevated,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(DarkGlassCard)
                                                .border(2.dp, AppleCrimson, CircleShape)
                                                .clickable { imagePickerLauncher.launch("image/*") },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (cloudAccount?.avatarUri != null) {
                                                AsyncImage(
                                                    model = cloudAccount.avatarUri,
                                                    contentDescription = "Avatar",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = AppleCrimson, modifier = Modifier.size(46.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            if (isEditingName) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    OutlinedTextField(
                                                        value = inputUsername,
                                                        onValueChange = { inputUsername = it },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor = TextPrimary,
                                                            unfocusedTextColor = TextPrimary
                                                        )
                                                    )
                                                    IconButton(onClick = {
                                                        if (inputUsername.isNotBlank()) {
                                                            onUpdateProfile(inputUsername.trim(), cloudAccount?.avatarUri)
                                                            isEditingName = false
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Check, contentDescription = "Enregistrer", tint = AppleCrimson)
                                                    }
                                                }
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = cloudAccount?.username ?: "Invité SLM",
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            inputUsername = cloudAccount?.username ?: ""
                                                            isEditingName = true
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Éditer", tint = AppleCrimson, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = cloudAccount?.email ?: "Non connecté au Cloud",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "ID Serveur : ${cloudAccount?.cloudUserId ?: "guest"}",
                                                fontSize = 10.sp,
                                                color = TextTertiary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Storage Quota Meter
                                    val usedBytes = cloudAccount?.usedStorageBytes ?: 102400L
                                    val totalBytes = cloudAccount?.totalQuotaBytes ?: 5368709120L
                                    val usedKb = usedBytes / 1024
                                    val progress = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.01f, 1.0f)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Espace Cloud utilisé", fontSize = 11.sp, color = TextSecondary)
                                        Text("${usedKb} Ko / 5 Go", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppleCrimson)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = AppleCrimson,
                                        trackColor = DarkGlassCard
                                    )
                                }
                            }
                        }

                        // Cloud Synchronization Controls
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = DarkGlassElevated,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("État de la synchronisation", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Dernière synchro : $lastSyncTimeFormatted", fontSize = 12.sp, color = TextSecondary)
                                        }

                                        GlassButton(
                                            onClick = {
                                                isSyncingNow = true
                                                onPerformCloudSync()
                                            },
                                            isPrimary = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("btn_sync_now")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = "Synchroniser",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .then(if (isSyncingNow) Modifier.rotate(spinAngle) else Modifier)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Synchroniser", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Auto-Sync Toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkGlassCard)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Synchronisation automatique", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text("Sauvegarde en arrière-plan à chaque modification", fontSize = 11.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = isAutoSyncEnabled,
                                            onCheckedChange = onToggleAutoSync,
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleCrimson)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Create Backup Button
                        item {
                            GlassButton(
                                onClick = onCreateBackupSnapshot,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_create_cloud_backup"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ApplePurple, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Créer un instantané de sauvegarde Cloud", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Playlists, favoris, signets web et réglages personnalisés", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = ApplePurple, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Logout & Delete Account triggers
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassButton(
                                    onClick = onLogout,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Déconnexion", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                GlassButton(
                                    onClick = { deleteAccountConfirmId = cloudAccount?.cloudUserId },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Supprimer compte", fontSize = 12.sp, color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 1: CLOUD BACKUPS
                if (activeTab == 1) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("HISTORIQUE DES SAUVEGARDES CLOUD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextTertiary, letterSpacing = 1.sp)
                                GlassButton(
                                    onClick = onCreateBackupSnapshot,
                                    isPrimary = true,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ Nouvelle", fontSize = 11.sp, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (cloudBackups.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Aucune sauvegarde enregistrée pour le moment.", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(onClick = onCreateBackupSnapshot, colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson)) {
                                            Text("Créer une sauvegarde maintenant")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(cloudBackups) { snapshot ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = DarkGlassElevated,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(snapshot.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }

                                            IconButton(
                                                onClick = { onDeleteBackupSnapshot(snapshot.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("🎵 ${snapshot.playlistsCount} pl.", fontSize = 11.sp, color = TextSecondary)
                                                Text("⭐ ${snapshot.favoritesCount} fav.", fontSize = 11.sp, color = TextSecondary)
                                                Text("🌐 ${snapshot.bookmarksCount} signets", fontSize = 11.sp, color = TextSecondary)
                                            }

                                            Button(
                                                onClick = { restoreConfirmSnapshot = snapshot },
                                                colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson.copy(alpha = 0.85f)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Restaurer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: SÉCURITÉ & JETONS DE SESSION (Explanation & Architecture)
                if (activeTab == 2) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("ARCHITECTURE DE SÉCURITÉ & JETONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextTertiary, letterSpacing = 1.sp)
                        }

                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = DarkGlassElevated,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Protection & Hachage Serveur", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "1. Vos identifiants de compte et profils sont stockés de manière chiffrée sur le serveur distant.\n" +
                                                "2. Votre mot de passe n'est JAMAIS enregistré en clair : il est transformé avec un sel aléatoire de 16 octets via un algorithme cryptographique SHA-256 / bcrypt.\n" +
                                                "3. Lors de la reconnexion, le serveur compare l'empreinte hachée pour valider l'accès sans jamais déchiffrer votre mot de passe original.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = DarkGlassElevated,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = ApplePurple, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Jeton de Session Actif sur l'Appareil", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Pour éviter de retaper votre mot de passe à chaque ouverture de l'application, celle-ci conserve un jeton de session JWT sécurisé dans le stockage protégé de l'appareil :",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkCanvas)
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Session Token (JWT) :",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextTertiary
                                            )
                                            Text(
                                                text = activeSession?.sessionToken ?: (cloudAccount?.cloudSessionToken ?: "Non généré (Session invité)"),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = AppleCrimson,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Device Fingerprint : ${activeSession?.deviceId ?: "Appareil Android standard"}",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: GESTION DES COMPTES / CONNEXION / INSCRIPTION
                if (activeTab == 3) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Switch between existing accounts on this device
                        item {
                            Text("COMPTES ENREGISTRÉS SUR CET APPAREIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextTertiary, letterSpacing = 1.sp)
                        }

                        items(savedAccounts) { acc ->
                            val isCurrent = acc.cloudUserId == cloudAccount?.cloudUserId
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isCurrent) onSwitchAccount(acc.cloudUserId) },
                                backgroundColor = if (isCurrent) AppleCrimson.copy(alpha = 0.2f) else DarkGlassCard,
                                borderColor = if (isCurrent) AppleCrimson else GlassBorder,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = if (isCurrent) AppleCrimson else TextSecondary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acc.username, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(acc.email, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    if (isCurrent) {
                                        Text("Actif", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppleCrimson)
                                    } else {
                                        TextButton(onClick = { onSwitchAccount(acc.cloudUserId) }) {
                                            Text("Basculer", fontSize = 12.sp, color = AppleCrimson)
                                        }
                                    }
                                }
                            }
                        }

                        // Inscription / Connexion Form
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                if (isRegisterMode) "CRÉER UN COMPTE CLOUD (INSCRIPTION)" else "SE CONNECTER À UN COMPTE CLOUD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = inputUsername,
                                onValueChange = { inputUsername = it },
                                label = { Text(if (isRegisterMode) "Nom d'utilisateur" else "Identifiant (Pseudo ou Email)", color = TextSecondary, fontSize = 12.sp) },
                                singleLine = true,
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

                            if (isRegisterMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputEmail,
                                    onValueChange = { inputEmail = it },
                                    label = { Text("Adresse E-mail", color = TextSecondary, fontSize = 12.sp) },
                                    singleLine = true,
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

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputPassword,
                                onValueChange = { inputPassword = it },
                                label = { Text("Mot de passe sécurisé", color = TextSecondary, fontSize = 12.sp) },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Afficher mot de passe",
                                            tint = TextSecondary
                                        )
                                    }
                                },
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

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    authErrorText = null
                                    authSuccessText = null
                                    if (isRegisterMode) {
                                        onRegister(inputUsername, inputEmail, inputPassword) { res ->
                                            when (res) {
                                                is AuthResult.Success -> {
                                                    authSuccessText = res.message
                                                    activeTab = 0
                                                }
                                                is AuthResult.Error -> authErrorText = res.errorMessage
                                            }
                                        }
                                    } else {
                                        onLogin(inputUsername, inputPassword) { res ->
                                            when (res) {
                                                is AuthResult.Success -> {
                                                    authSuccessText = res.message
                                                    activeTab = 0
                                                }
                                                is AuthResult.Error -> authErrorText = res.errorMessage
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Créer mon compte (Hachage sécurisé)" else "Se connecter (Valider session)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // OAuth Third Party Authentication
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassButton(
                                    onClick = {
                                        onOAuthLogin("GOOGLE_OAUTH", "Google User", "google.user@gmail.com", null) { res ->
                                            when (res) {
                                                is AuthResult.Success -> {
                                                    authSuccessText = res.message
                                                    activeTab = 0
                                                }
                                                is AuthResult.Error -> authErrorText = res.errorMessage
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔍 Google Sign-In", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    }
                                }

                                GlassButton(
                                    onClick = {
                                        onOAuthLogin("APPLE_ID", "Apple Member", "apple.user@icloud.com", null) { res ->
                                            when (res) {
                                                is AuthResult.Success -> {
                                                    authSuccessText = res.message
                                                    activeTab = 0
                                                }
                                                is AuthResult.Error -> authErrorText = res.errorMessage
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🍎 Apple ID", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(onClick = {
                                    isRegisterMode = !isRegisterMode
                                    authErrorText = null
                                    authSuccessText = null
                                }) {
                                    Text(
                                        text = if (isRegisterMode) "Déjà un compte ? Se connecter" else "Pas encore de compte ? S'inscrire",
                                        color = AppleCrimson,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore Snapshot Confirmation Dialog
    restoreConfirmSnapshot?.let { snap ->
        AlertDialog(
            onDismissRequest = { restoreConfirmSnapshot = null },
            title = { Text("Restaurer cette sauvegarde ?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Cette sauvegarde (${snap.title}) restaurera vos playlists, vos titres et vos signets web.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreBackupSnapshot(snap)
                    restoreConfirmSnapshot = null
                }) {
                    Text("Restaurer maintenant", color = AppleCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirmSnapshot = null }) {
                    Text("Annuler", color = TextPrimary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Account Confirmation Dialog (Dual Process: Local Purge + Server Hard/Soft Delete)
    deleteAccountConfirmId?.let { uid ->
        AlertDialog(
            onDismissRequest = { deleteAccountConfirmId = null },
            title = { Text("Supprimer votre compte Cloud ?", fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30)) },
            text = {
                Column {
                    Text(
                        "Cette action va révoquer tous les jetons de session sur cet appareil et effacer vos données locales.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkGlassCard)
                            .clickable { deleteHardMode = !deleteHardMode }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteHardMode,
                            onCheckedChange = { deleteHardMode = it },
                            colors = CheckboxDefaults.colors(checkedColor = AppleCrimson)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (deleteHardMode) "Suppression définitive sur le serveur (Hard Delete)" else "Désactivation du compte (Soft Delete)",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAccount(uid, deleteHardMode)
                    deleteAccountConfirmId = null
                }) {
                    Text("Confirmer la suppression", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAccountConfirmId = null }) {
                    Text("Annuler", color = TextPrimary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
