package com.example.slmplay.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "guest",
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val uriString: String,
    val coverResName: String? = null,
    val coverUri: String? = null,
    val isFavorite: Boolean = false,
    val addedDate: Long = System.currentTimeMillis(),
    val genre: String = "Audio",
    val isProcedural: Boolean = false,
    val proceduralPreset: String = "synthwave",
    val playCount: Int = 0,
    val year: Int = 2024,
    val lastPlayedDate: Long = 0L,
    val originalTitle: String? = null,
    val originalArtist: String? = null,
    val originalAlbum: String? = null,
    val originalCoverUri: String? = null
) {
    fun formattedDuration(): String {
        val totalSecs = (durationMs / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
