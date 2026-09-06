package com.example.slmplay.data.model

data class UserProfile(
    val userId: String = "",
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val emailOrId: String = "",
    val avatarUri: String? = null,
    val bio: String = "",
    val joinedDate: Long = 0L,
    val totalTracksPlayed: Int = 0,
    val lastCloudBackupDate: Long? = null
)
