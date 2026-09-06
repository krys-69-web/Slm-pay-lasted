package com.example.slmplay.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slmplay.utils.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalDownloaderSheet(
    activeUrl: String,
    pageTitle: String,
    downloadManager: UniversalDownloadManager,
    isPrivateSpaceContext: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDownloads by downloadManager.activeDownloads.collectAsState()
    val sniffedMedia by downloadManager.sniffedMedia.collectAsState()

    var selectedDestinationIsPrivate by remember {
        mutableStateOf(isPrivateSpaceContext)
    }

    var customUrlInput by remember { mutableStateOf(activeUrl) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sniffer / Liens, 1 = En cours

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF13151D),
        contentColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.White.copy(alpha = 0.25f)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppleCrimson.copy(alpha = 0.15f))
                            .border(1.dp, AppleCrimson.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = AppleCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Téléchargeur Universel",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (sniffedMedia.isNotEmpty()) "${sniffedMedia.size} médias détectés" else "Analyse en direct",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Destination Selector (Public Downloads vs Coffre-fort Privé)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkGlassCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Option 1: Téléchargement Public
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (!selectedDestinationIsPrivate) AppleCrimson else Color.Transparent
                            )
                            .clickable { selectedDestinationIsPrivate = false }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (!selectedDestinationIsPrivate) Color.White else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Téléchargements",
                                color = if (!selectedDestinationIsPrivate) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (!selectedDestinationIsPrivate) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Option 2: Coffre-fort Privé
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedDestinationIsPrivate) Color(0xFF5E5CE6) else Color.Transparent
                            )
                            .clickable { selectedDestinationIsPrivate = true }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (selectedDestinationIsPrivate) Color.White else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Coffre-fort Privé",
                                color = if (selectedDestinationIsPrivate) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selectedDestinationIsPrivate) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-tabs: Médias détectés vs Téléchargements en cours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabChip(
                    title = "Médias détectés (${sniffedMedia.size})",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabChip(
                    title = "En cours (${activeDownloads.size})",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // Media Sniffer List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Action: Download active page URL directly
                    if (activeUrl.isNotBlank() && !activeUrl.startsWith("about:")) {
                        item {
                            ActivePageDownloadCard(
                                title = pageTitle.ifBlank { "Page web active" },
                                url = activeUrl,
                                isPrivate = selectedDestinationIsPrivate,
                                onDownload = {
                                    if (selectedDestinationIsPrivate) {
                                        downloadManager.startPrivateVaultDownload(activeUrl, pageTitle)
                                    } else {
                                        downloadManager.startPublicDownload(activeUrl, suggestedTitle = pageTitle)
                                    }
                                    selectedTab = 1
                                }
                            )
                        }
                    }

                    if (sniffedMedia.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aucun flux média supplémentaire détecté",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Vous pouvez télécharger la page active ci-dessus",
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(sniffedMedia, key = { it.id }) { media ->
                            SniffedMediaRow(
                                item = media,
                                isPrivate = selectedDestinationIsPrivate,
                                onDownload = {
                                    if (selectedDestinationIsPrivate) {
                                        downloadManager.startPrivateVaultDownload(media.url, media.title)
                                    } else {
                                        downloadManager.startPublicDownload(media.url, suggestedTitle = media.title)
                                    }
                                    selectedTab = 1
                                }
                            )
                        }
                    }
                }
            } else {
                // Active Downloads List
                if (activeDownloads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun téléchargement actif",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeDownloads, key = { it.id }) { item ->
                            DownloadProgressCard(item = item)
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(onClick = { downloadManager.clearFinishedDownloads() }) {
                                    Text(
                                        "Effacer l'historique des téléchargements",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.12f) else DarkGlassCard)
            .border(
                1.dp,
                if (isSelected) AppleCrimson.copy(alpha = 0.5f) else GlassBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActivePageDownloadCard(
    title: String,
    url: String,
    isPrivate: Boolean,
    onDownload: () -> Unit
) {
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF34C759).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onDownload,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isPrivate) Color(0xFF5E5CE6) else AppleCrimson,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPrivate) "Coffre" else "Télécharger",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SniffedMediaRow(
    item: SniffedMediaItem,
    isPrivate: Boolean,
    onDownload: () -> Unit
) {
    val (icon, badgeColor) = when (item.mediaType) {
        VaultFileType.VIDEO -> Pair(Icons.Default.Videocam, Color(0xFFFF9500))
        VaultFileType.AUDIO -> Pair(Icons.Default.MusicNote, Color(0xFFFF2D55))
        VaultFileType.IMAGE -> Pair(Icons.Default.Image, Color(0xFF30B0C7))
        VaultFileType.DOCUMENT -> Pair(Icons.Default.Description, Color(0xFF5856D6))
        VaultFileType.OTHER -> Pair(Icons.Default.InsertDriveFile, Color.Gray)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkGlassCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { item.url.substringAfterLast("/") },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.mediaType.name,
                        color = badgeColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${item.url.take(35)}...",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDownload,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isPrivate) Color(0xFF5E5CE6) else AppleCrimson)
            ) {
                Icon(
                    imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Download,
                    contentDescription = "Télécharger",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(item: DownloadItem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkGlassCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (item.isPrivateVault) Icons.Default.Lock else Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (item.isPrivateVault) Color(0xFF5E5CE6) else AppleCrimson,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = when (item.status) {
                        DownloadStatus.RUNNING -> "${item.progressPercent}%"
                        DownloadStatus.SUCCESS -> "Terminé"
                        DownloadStatus.FAILED -> "Échoué"
                        DownloadStatus.PENDING -> "En attente"
                    },
                    color = when (item.status) {
                        DownloadStatus.SUCCESS -> Color(0xFF34C759)
                        DownloadStatus.FAILED -> Color(0xFFFF453A)
                        else -> AppleCrimson
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = {
                    if (item.status == DownloadStatus.SUCCESS) 1f
                    else item.progressPercent / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (item.isPrivateVault) Color(0xFF5E5CE6) else AppleCrimson,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            if (item.status == DownloadStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.errorMessage,
                    color = Color(0xFFFF453A),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}
