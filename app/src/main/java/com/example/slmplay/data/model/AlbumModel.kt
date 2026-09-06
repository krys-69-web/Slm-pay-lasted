package com.example.slmplay.data.model

import com.example.slmplay.data.db.TrackEntity

data class AlbumModel(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int = 2024,
    val genre: String = "Divers",
    val coverUri: String? = null,
    val coverResName: String? = null,
    val tracks: List<TrackEntity> = emptyList(),
    val gradientIndex: Int = 0
) {
    val trackCount: Int get() = tracks.size
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }

    fun formattedDuration(): String {
        val totalSecs = totalDurationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        val hrs = mins / 60
        return if (hrs > 0) {
            val remainMins = mins % 60
            "${hrs}h ${remainMins}m"
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
