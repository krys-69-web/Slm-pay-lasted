package com.example.slmplay.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

object SecurityAuthHelper {

    private val secureRandom = SecureRandom()

    /**
     * Generates a 16-byte cryptographically secure random salt in Hex format.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes a password using SHA-256 combined with a secure per-user salt.
     * Never stores plain-text passwords on the server or database.
     */
    fun hashPassword(password: String, salt: String): String {
        val combined = "$salt:$password:slm_cloud_secret_pepper_2026"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies a plain text candidate password against the stored salt and hash.
     */
    fun verifyPassword(candidate: String, salt: String, expectedHash: String): Boolean {
        val candidateHash = hashPassword(candidate, salt)
        return candidateHash.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Generates a realistic secure JWT-like session token for the device.
     */
    fun generateSessionToken(userId: String, deviceId: String): String {
        val header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
        val payload = "{\"sub\":\"$userId\",\"dev\":\"$deviceId\",\"iat\":${System.currentTimeMillis()},\"iss\":\"slm-cloud-auth\"}"
        val headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
        val payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val signature = hashPassword("$headerB64.$payloadB64", userId).take(32)
        return "slm_jwt.$headerB64.$payloadB64.$signature"
    }

    /**
     * Generates a long-lived refresh token.
     */
    fun generateRefreshToken(userId: String): String {
        return "slm_ref_${UUID.randomUUID()}_${userId.take(8)}"
    }

    /**
     * Generates a unique secure device identifier.
     */
    fun generateDeviceId(): String {
        return "android_dev_" + UUID.randomUUID().toString().take(12)
    }
}
