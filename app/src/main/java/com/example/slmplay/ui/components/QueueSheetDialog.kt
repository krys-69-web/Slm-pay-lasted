package com.example.slmplay.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.slmplay.data.db.TrackEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheetDialog(
    isOpen: Boolean,
    queue: List<TrackEntity>,
    currentIndex: Int,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveAsPlaylist: (String) -> Unit
) {
    if (!isOpen) return

    var isSaveDialogOpen by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }

    val totalDurationMs = remember(queue) {
        queue.sumOf { if (it.durationMs > 0) it.durationMs else 180000L }
    }

    val totalDurationFormatted = remember(totalDurationMs) {
        val totalSec = totalDurationMs / 1000
        val min = totalSec / 60
        val hr = min / 60
        if (hr > 0) {
            "${hr}h ${min % 60}min"
        } else {
            "${min} min"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCanvas.copy(alpha = 0.85f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Main Glass Sheet Content
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable(enabled = false) {},
                backgroundColor = DarkSurface.copy(alpha = 0.95f),
                borderColor = GlassBorder,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Drag pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(TextTertiary.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppleCrimson.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = AppleCrimson,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "File d'attente",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${queue.size} titre${if (queue.size > 1) "s" else ""} • $totalDurationFormatted",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkGlassCard)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Actions Row (Save as playlist & Clear queue)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                newPlaylistTitle = "File d'attente ${System.currentTimeMillis() % 1000}"
                                isSaveDialogOpen = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DarkGlassCard,
                                contentColor = TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = ApplePurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (queue.size > 1) {
                            OutlinedButton(
                                onClick = onClearQueue,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DarkGlassCard,
                                    contentColor = TextSecondary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.ClearAll, contentDescription = null, tint = AppleCrimson, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vider la suite", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (queue.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "La file d'attente est vide",
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Sélectionnez un titre ou utilisez « Écouter juste après »",
                                    color = TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Section: En cours de lecture
                            if (currentTrack != null) {
                                item {
                                    Text(
                                        text = "EN LECTURE",
                                        color = AppleCrimson,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        backgroundColor = AppleCrimson.copy(alpha = 0.12f),
                                        borderColor = AppleCrimson.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Artwork
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(DarkCanvas)
                                            ) {
                                                val coverRes = when (currentTrack.coverResName) {
                                                    "cover_neon" -> R.drawable.cover_neon
                                                    "cover_ambient" -> R.drawable.cover_ambient
                                                    else -> R.drawable.slm_logo
                                                }
                                                AsyncImage(
                                                    model = currentTrack.coverUri ?: coverRes,
                                                    contentDescription = currentTrack.title,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                if (isPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.4f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.GraphicEq,
                                                            contentDescription = null,
                                                            tint = AppleCrimson,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = currentTrack.title,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = currentTrack.artist,
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Playing badge
                                            Surface(
                                                color = AppleCrimson.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isPlaying) "ACTIF" else "PAUSE",
                                                    color = AppleCrimson,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (queue.size > 1) {
                                        Text(
                                            text = "À SUIVRE (${queue.size - 1})",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Section: Upcoming & rest of the queue
                            itemsIndexed(queue, key = { index, track -> "${track.id}_$index" }) { index, track ->
                                if (index != currentIndex) {
                                    QueueTrackItem(
                                        index = index,
                                        track = track,
                                        canMoveUp = index > 0,
                                        canMoveDown = index < queue.size - 1,
                                        onPlay = { onPlayTrackAt(index) },
                                        onMoveUp = { onReorder(index, index - 1) },
                                        onMoveDown = { onReorder(index, index + 1) },
                                        onRemove = { onRemove(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save As Playlist Dialog
    if (isSaveDialogOpen) {
        AlertDialog(
            onDismissRequest = { isSaveDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = ApplePurple)
                    Text("Créer une playlist", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enregistrer tous les morceaux actuels de la file (${queue.size} titres) dans une nouvelle playlist.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        label = { Text("Nom de la playlist") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ApplePurple,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistTitle.isNotBlank()) {
                            onSaveAsPlaylist(newPlaylistTitle.trim())
                            isSaveDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApplePurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enregistrer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isSaveDialogOpen = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun QueueTrackItem(
    index: Int,
    track: TrackEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        backgroundColor = DarkGlassCard,
        borderColor = GlassBorder,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Index number
            Text(
                text = "${index + 1}",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(18.dp)
            )

            // Cover thumbnail
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCanvas)
            ) {
                val coverRes = when (track.coverResName) {
                    "cover_neon" -> R.drawable.cover_neon
                    "cover_ambient" -> R.drawable.cover_ambient
                    else -> R.drawable.slm_logo
                }
                AsyncImage(
                    model = track.coverUri ?: coverRes,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Reorder up & down arrows
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Monter",
                        tint = if (canMoveUp) TextSecondary else TextTertiary.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Descendre",
                        tint = if (canMoveDown) TextSecondary else TextTertiary.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Supprimer de la file",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
