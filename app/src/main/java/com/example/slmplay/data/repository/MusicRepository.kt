package com.example.slmplay.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.slmplay.data.db.MusicDao
import com.example.slmplay.data.db.PlaylistEntity
import com.example.slmplay.data.db.PlaylistTrackCrossRef
import com.example.slmplay.data.db.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {

    fun getAllTracks(userId: String): Flow<List<TrackEntity>> = musicDao.getAllTracks(userId)

    fun getFavoriteTracks(userId: String): Flow<List<TrackEntity>> = musicDao.getFavoriteTracks(userId)

    fun getAllPlaylists(userId: String): Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists(userId)

    fun getTracksForPlaylist(playlistId: String, userId: String): Flow<List<TrackEntity>> =
        musicDao.getTracksForPlaylist(playlistId, userId)

    fun getPlaylistTrackCount(playlistId: String): Flow<Int> =
        musicDao.getPlaylistTrackCount(playlistId)

    suspend fun purgeDemoData() = withContext(Dispatchers.IO) {
        try {
            musicDao.purgeDemoTracks()
            musicDao.purgeDemoPlaylists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun scanDeviceAudioFiles(userId: String): Int = withContext(Dispatchers.IO) {
        val scannedTracks = mutableListOf<TrackEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val yearCol = it.getColumnIndex(MediaStore.Audio.Media.YEAR)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Piste inconnue"
                    val artist = it.getString(artistCol) ?: "Artiste inconnu"
                    val album = it.getString(albumCol) ?: ""
                    val duration = it.getLong(durCol)
                    val albumId = it.getLong(albumIdCol)
                    val year = if (yearCol != -1) it.getInt(yearCol).coerceAtLeast(1900) else 2024

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = "content://media/external/audio/albumart/$albumId"

                    val track = TrackEntity(
                        id = "local_media_${userId}_$id",
                        userId = userId,
                        title = title,
                        artist = if (artist == "<unknown>") "Artiste inconnu" else artist,
                        album = album,
                        durationMs = duration,
                        uriString = contentUri.toString(),
                        coverUri = albumArtUri,
                        isFavorite = false,
                        isProcedural = false,
                        genre = "Local Audio",
                        year = year,
                        originalTitle = title,
                        originalArtist = artist,
                        originalAlbum = album,
                        originalCoverUri = albumArtUri
                    )
                    scannedTracks.add(track)
                }
            }

            if (scannedTracks.isNotEmpty()) {
                musicDao.insertTracks(scannedTracks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scannedTracks.size
    }

    suspend fun importAudioUris(uris: List<Uri>, userId: String): Int = withContext(Dispatchers.IO) {
        val imported = mutableListOf<TrackEntity>()
        val resolver: ContentResolver = context.contentResolver

        uris.forEach { uri ->
            try {
                var displayName = "Morceau ${imported.size + 1}"

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }

                val cleanTitle = displayName.substringBeforeLast(".")

                val track = TrackEntity(
                    id = "import_" + UUID.randomUUID().toString(),
                    userId = userId,
                    title = cleanTitle,
                    artist = "Fichier importé",
                    album = "Importations",
                    durationMs = 210000L,
                    uriString = uri.toString(),
                    isFavorite = false,
                    isProcedural = false,
                    genre = "Import local",
                    originalTitle = cleanTitle,
                    originalArtist = "Fichier importé",
                    originalAlbum = "Importations"
                )
                imported.add(track)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (imported.isNotEmpty()) {
            musicDao.insertTracks(imported)
        }

        imported.size
    }

    suspend fun insertCustomTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        musicDao.insertTrack(track)
    }

    suspend fun updateTrackMetadata(
        id: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        coverUri: String?
    ) = withContext(Dispatchers.IO) {
        musicDao.updateTrackMetadata(id, title, artist, album, genre, year, coverUri)
    }

    suspend fun restoreTrackMetadata(id: String) = withContext(Dispatchers.IO) {
        musicDao.restoreTrackMetadata(id)
    }

    suspend fun recordPlay(id: String) = withContext(Dispatchers.IO) {
        musicDao.recordPlay(id)
    }

    suspend fun toggleFavorite(trackId: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        val newStatus = !currentStatus
        musicDao.updateFavorite(trackId, newStatus)
    }

    suspend fun createPlaylist(name: String, description: String, gradientIndex: Int, userId: String): String = withContext(Dispatchers.IO) {
        val id = "playlist_" + UUID.randomUUID().toString()
        val playlist = PlaylistEntity(
            id = id,
            userId = userId,
            name = name,
            description = description,
            gradientIndex = gradientIndex,
            isSystem = false
        )
        musicDao.insertPlaylist(playlist)
        id
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        val existing = musicDao.getTrackIdsInPlaylist(playlistId).toSet()
        if (trackId !in existing) {
            musicDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, position = existing.size))
        }
    }

    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>): Int = withContext(Dispatchers.IO) {
        val existing = musicDao.getTrackIdsInPlaylist(playlistId).toSet()
        val toAdd = trackIds.filter { it !in existing }
        if (toAdd.isNotEmpty()) {
            val crossRefs = toAdd.mapIndexed { idx, trackId ->
                PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId, position = existing.size + idx)
            }
            musicDao.addTracksToPlaylist(crossRefs)
        }
        toAdd.size
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>) = withContext(Dispatchers.IO) {
        if (trackIds.isNotEmpty()) {
            musicDao.removeTracksFromPlaylist(playlistId, trackIds)
        }
    }

    suspend fun deleteTrack(trackId: String) = withContext(Dispatchers.IO) {
        musicDao.deleteTrack(trackId)
    }

    suspend fun deleteUserData(userId: String) = withContext(Dispatchers.IO) {
        musicDao.deleteTracksForUser(userId)
        musicDao.deletePlaylistsForUser(userId)
    }
}
