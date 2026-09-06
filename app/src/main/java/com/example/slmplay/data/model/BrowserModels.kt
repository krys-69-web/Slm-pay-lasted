package com.example.slmplay.data.model

import java.util.UUID

data class BrowserBookmark(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val iconEmoji: String = "🌐",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val isCloudSynced: Boolean = true
)

data class BrowserHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Nouvel onglet",
    val url: String = "about:blank",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false
)

data class QuickSpeedDial(
    val title: String,
    val url: String,
    val emoji: String,
    val category: String,
    val colorHex: Long
)
