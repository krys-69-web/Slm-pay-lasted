package com.example.slmplay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.slmplay.data.db.TrackEntity
import com.example.slmplay.data.model.AlbumModel
import com.example.ui.theme.*

@Composable
fun AlbumDetailDialog(
    isOpen: Boolean,
    album: AlbumModel?,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>, Int) -> Unit,
    onPlayAlbum: (AlbumModel) -> Unit,
    onShuffleAlbum: (AlbumModel) -> Unit,
    onAddAlbumToQueue: (AlbumModel) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onAddToQueue: (TrackEntity) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onOpenAddToPlaylist: (TrackEntity) -> Unit,
    onCreatePlaylistFromAlbum: (AlbumModel) -> Unit
) {
    if (!isOpen || album == null) return

    val gradient = NeonMeshGradients.getOrElse(album.gradientIndex % NeonMeshGradients.size) { NeonMeshGradients[0] }

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header Banner with Artwork & Glass Overlay
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        // Background Ambient Mesh Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(gradient.first().copy(alpha = 0.85f), gradient.last().copy(alpha = 0.3f), DarkCanvas)))
                        )

                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Album Artwork & Core Meta
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Cover Artwork Box
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.5.dp, GlassBorder, RoundedCornerShape(20.dp))
                                    .background(DarkGlassCard),
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
                                    val resId = when (album.coverResName) {
                                        "cover_neon" -> R.drawable.cover_neon
                                        "cover_ambient" -> R.drawable.cover_ambient
                                        else -> R.drawable.slm_logo
                                    }
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppleCrimson.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("ALBUM OFFICIEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppleCrimson, letterSpacing = 1.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = album.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.artist,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${album.trackCount} titres • ${album.formattedDuration()}",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }

                // Action Controls Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play All Button
                        Button(
                            onClick = { onPlayAlbum(album) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppleCrimson),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_play_album")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lire", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Shuffle Button
                        GlassButton(
                            onClick = { onShuffleAlbum(album) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aléatoire", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                        }

                        // Add album to queue
                        IconButton(
                            onClick = { onAddAlbumToQueue(album) },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkGlassCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.QueuePlayNext, contentDescription = "File d'attente", tint = AppleCrimson, modifier = Modifier.size(20.dp))
                        }

                        // Create playlist from album
                        IconButton(
                            onClick = { onCreatePlaylistFromAlbum(album) },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkGlassCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = "Créer playlist", tint = ApplePurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Divider / Section Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LISTE DES TITRES DE L'ALBUM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${album.trackCount} pistes",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                // Track list
                itemsIndexed(album.tracks, key = { _, track -> track.id }) { index, track ->
                    val isCurrent = currentTrack?.id == track.id
                    var menuOpen by remember { mutableStateOf(false) }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .testTag("album_track_${track.id}"),
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (isCurrent) AppleCrimson.copy(alpha = 0.2f) else DarkGlassCard,
                        borderColor = if (isCurrent) AppleCrimson else GlassBorder,
                        onClick = { onPlayTrack(track, album.tracks, index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Track Number or Equalizer pulse
                            Text(
                                text = if (isCurrent && isPlaying) "▶" else "${index + 1}",
                                color = if (isCurrent) AppleCrimson else TextTertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp)
                            )

                            // Title and details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) AppleCrimson else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${track.formattedDuration()}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Favorite button
                            IconButton(
                                onClick = { onToggleFavorite(track) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favori",
                                    tint = if (track.isFavorite) AppleCrimson else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Context menu
                            Box {
                                IconButton(
                                    onClick = { menuOpen = true },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false },
                                    modifier = Modifier.background(DarkSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Écouter juste après", color = TextPrimary) },
                                        onClick = {
                                            menuOpen = false
                                            onPlayNext(track)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.QueuePlayNext, contentDescription = null, tint = AppleCrimson)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Ajouter à la file d'attente", color = TextPrimary) },
                                        onClick = {
                                            menuOpen = false
                                            onAddToQueue(track)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = ApplePurple)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Ajouter à une playlist", color = TextPrimary) },
                                        onClick = {
                                            menuOpen = false
                                            onOpenAddToPlaylist(track)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = TextPrimary)
                                        }
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
