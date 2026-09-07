package com.example.slmplay.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * StoragePersistenceManager ensures user-imported images, covers, avatars,
 * and audio files are permanently preserved in app-specific persistent storage.
 *
 * It prevents temporary blob/picker URIs from becoming invalid after app restart,
 * phone reboot, screen switch, or account changes.
 */
object StoragePersistenceManager {

    private const val TAG = "StoragePersistence"
    private const val MEDIA_ROOT = "persistent_media"
    private const val IMAGES_DIR = "images"
    private const val AUDIO_DIR = "audio"

    private fun getMediaDirectory(context: Context, subDir: String): File {
        val root = File(context.filesDir, MEDIA_ROOT)
        val target = File(root, subDir)
        if (!target.exists()) {
            target.mkdirs()
        }
        return target
    }

    /**
     * Copies any image URI (picker, temp, content, or external) into internal persistent storage.
     * Returns a persistent, permanent file URI string ("file:///data/user/0/...").
     */
    suspend fun persistImage(
        context: Context,
        sourceUri: Uri,
        category: String,
        itemId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val scheme = sourceUri.scheme
            // If it's already an internal file in our persistent directory, no need to duplicate
            if (scheme == ContentResolver.SCHEME_FILE) {
                val path = sourceUri.path ?: ""
                if (path.contains(context.filesDir.absolutePath) && File(path).exists()) {
                    return@withContext sourceUri.toString()
                }
            }

            val categoryDir = File(getMediaDirectory(context, IMAGES_DIR), category)
            if (!categoryDir.exists()) categoryDir.mkdirs()

            val safeId = itemId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(categoryDir, "${safeId}_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.w(TAG, "Cannot open input stream for image: $sourceUri")
                return@withContext null
            }

            // Verify file was written and is valid
            if (targetFile.exists() && targetFile.length() > 0) {
                // Delete previous version for this item if one exists
                cleanOldVersions(categoryDir, safeId, targetFile.name)
                val persistentUri = Uri.fromFile(targetFile).toString()
                Log.d(TAG, "Image permanently persisted: $persistentUri")
                return@withContext persistentUri
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist image: $sourceUri", e)
            null
        }
    }

    private fun cleanOldVersions(dir: File, prefix: String, keepFilename: String) {
        try {
            val files = dir.listFiles { file ->
                file.name.startsWith("${prefix}_") && file.name != keepFilename
            } ?: return
            for (f in files) {
                f.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning old versions", e)
        }
    }

    /**
     * Copies an imported audio file to app-internal persistent storage
     * so it never vanishes if the original is moved or transient permission expires.
     */
    suspend fun persistAudioFile(
        context: Context,
        sourceUri: Uri,
        trackId: String,
        suggestedName: String
    ): String = withContext(Dispatchers.IO) {
        // Try taking persistent URI permission if granted by Android SAF
        try {
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Not a persistable SAF grant; we will copy file directly
        }

        try {
            val audioDir = getMediaDirectory(context, AUDIO_DIR)
            val extension = when {
                suggestedName.contains(".") -> suggestedName.substringAfterLast(".").lowercase()
                else -> "mp3"
            }
            val safeTrackId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(audioDir, "${safeTrackId}.$extension")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                val fileUri = Uri.fromFile(targetFile).toString()
                Log.d(TAG, "Audio file permanently copied: $fileUri")
                return@withContext fileUri
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy audio file to internal storage, falling back to source URI: $sourceUri", e)
        }
        return@withContext sourceUri.toString()
    }

    /**
     * Extracts embedded album artwork directly from an audio file using MediaMetadataRetriever
     * and saves it permanently to the persistent covers storage.
     */
    suspend fun extractAndPersistEmbeddedArtwork(
        context: Context,
        audioUri: Uri,
        trackId: String
    ): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, audioUri)
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                val coversDir = File(getMediaDirectory(context, IMAGES_DIR), "embedded_covers")
                if (!coversDir.exists()) coversDir.mkdirs()

                val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val targetFile = File(coversDir, "${safeId}_art.jpg")

                FileOutputStream(targetFile).use { output ->
                    output.write(pictureBytes)
                }

                if (targetFile.exists() && targetFile.length() > 0) {
                    val coverUri = Uri.fromFile(targetFile).toString()
                    Log.d(TAG, "Extracted & persisted embedded artwork: $coverUri")
                    return@withContext coverUri
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "No embedded artwork in $audioUri: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
        null
    }

    data class ExtractedAudioDetails(
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val year: Int,
        val durationMs: Long,
        val embeddedCoverUri: String?
    )

    /**
     * Extracts rich metadata (title, artist, album, genre, year, duration, cover) from audio file.
     */
    suspend fun extractFullMetadata(
        context: Context,
        audioUri: Uri,
        fallbackTitle: String,
        trackId: String
    ): ExtractedAudioDetails = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var title = fallbackTitle
        var artist = "Artiste inconnu"
        var album = "Importations"
        var genre = "Audio"
        var year = 2024
        var durationMs = 180000L
        var embeddedCoverUri: String? = null

        try {
            retriever.setDataSource(context, audioUri)

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                title = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                artist = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
                album = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf { it.isNotBlank() }?.let {
                genre = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let {
                if (it in 1900..2099) year = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                if (it > 0) durationMs = it
            }

            // Extract picture if any
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                val coversDir = File(getMediaDirectory(context, IMAGES_DIR), "embedded_covers")
                if (!coversDir.exists()) coversDir.mkdirs()

                val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val targetFile = File(coversDir, "${safeId}_art.jpg")

                FileOutputStream(targetFile).use { output ->
                    output.write(pictureBytes)
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    embeddedCoverUri = Uri.fromFile(targetFile).toString()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting metadata from $audioUri", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }

        ExtractedAudioDetails(
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            year = year,
            durationMs = durationMs,
            embeddedCoverUri = embeddedCoverUri
        )
    }

    /**
     * Safely deletes a persistent file if it was created inside our app's persistent storage.
     */
    fun deletePersistentMedia(context: Context, fileUriString: String?): Boolean {
        if (fileUriString == null) return false
        return try {
            val uri = Uri.parse(fileUriString)
            if (uri.scheme == ContentResolver.SCHEME_FILE) {
                val path = uri.path ?: return false
                val file = File(path)
                if (path.contains(context.filesDir.absolutePath) && file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting persistent media $fileUriString", e)
            false
        }
    }
}
