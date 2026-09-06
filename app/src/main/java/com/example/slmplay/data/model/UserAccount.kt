package com.example.slmplay.data.model

data class UserAccount(
    val id: String,
    val username: String,
    val emailOrId: String,
    val avatarUri: String? = null,
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
