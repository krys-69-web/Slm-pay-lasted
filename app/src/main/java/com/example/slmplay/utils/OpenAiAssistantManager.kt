package com.example.slmplay.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AssistantChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class OpenAiAssistantManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("slm_openai_assistant_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "OpenAiAssistantManager"
        private const val KEY_OPENAI_API_KEY = "key_openai_api_key"
        private const val KEY_SERVER_ENDPOINT = "key_server_endpoint"
        private const val DEFAULT_SERVER_URL = "http://10.0.2.2:3001/api/assistant"
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply()
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_ENDPOINT, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_ENDPOINT, url.trim()).apply()
    }

    /**
     * Send a query to OpenAI assistant via either:
     * 1. Direct OpenAI API (if client API Key provided)
     * 2. Local/Remote SLM Play Server (if server available or fallback)
     */
    suspend fun sendMessage(
        userMessage: String,
        libraryContext: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val serverUrl = getServerUrl()

        // 1. Try Direct OpenAI API if API key is configured
        if (apiKey.isNotBlank()) {
            try {
                val directResult = callDirectOpenAiApi(apiKey, userMessage, libraryContext)
                return@withContext Result.success(directResult)
            } catch (e: Exception) {
                Log.w(TAG, "Direct OpenAI call failed, checking backend server", e)
            }
        }

        // 2. Try Node.js Backend Server
        try {
            val serverResult = callBackendServer(serverUrl, userMessage, libraryContext, apiKey)
            return@withContext Result.success(serverResult)
        } catch (e: Exception) {
            Log.w(TAG, "Backend server call failed, using built-in SLM Musical AI engine", e)
        }

        // 3. Built-in Offline Musical AI Engine fallback
        val fallback = generateBuiltInMusicalReply(userMessage, libraryContext)
        Result.success(fallback)
    }

    private fun callDirectOpenAiApi(apiKey: String, message: String, library: List<String>): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        val systemPrompt = "Tu es l'assistant musical intelligent officiel de l'application SLM Play (lecteur audio Hi-Fi cyberpunk et studio musical). " +
                "Ton rôle : conseiller l'utilisateur sur ses choix musicaux, recommander des morceaux parmi sa bibliothèque : ${library.take(15).joinToString(", ")}, " +
                "suggérer des ambiances sonores (Cyberpunk, Midnight Chill, Synthwave, Gym Beast) et donner des conseils sur les réglages d'égalisation SLM Play."

        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o-mini")
            val messagesArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", message)
                })
            }
            put("messages", messagesArr)
            put("temperature", 0.7)
            put("max_tokens", 450)
        }

        OutputStreamWriter(connection.outputStream).use { it.write(jsonBody.toString()) }

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val respJson = JSONObject(responseStr)
            val choices = respJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content")
            }
        } else {
            val errStr = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
            throw Exception("OpenAI API Erreur $responseCode: $errStr")
        }
        return "Aucune réponse de l'assistant."
    }

    private fun callBackendServer(serverUrl: String, message: String, library: List<String>, apiKey: String): String {
        val url = URL(serverUrl)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 5000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val jsonBody = JSONObject().apply {
            put("message", message)
            put("apiKey", apiKey)
            val libArr = JSONArray()
            library.take(15).forEach { libArr.put(it) }
            put("userLibrarySummary", libArr)
        }

        OutputStreamWriter(connection.outputStream).use { it.write(jsonBody.toString()) }

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val respJson = JSONObject(responseStr)
            return respJson.optString("reply", "Réponse reçue du serveur.")
        } else {
            throw Exception("Erreur serveur $responseCode")
        }
    }

    private fun generateBuiltInMusicalReply(message: String, library: List<String>): String {
        val lower = message.toLowerCase()
        return when {
            lower.contains("sport") || lower.contains("gym") || lower.contains("muscu") -> {
                "⚡ **Sélection Gym Beast (Haute Énergie)** :\n" +
                        "• Boostez les basses avec le preset **Bass Boost & Spatial Stereo** dans le menu SLM.\n" +
                        "• Tempo idéal : 128 - 140 BPM.\n" +
                        "• Mode suggéré : Activez le **Smart Shuffle** pour éviter les doublons !"
            }
            lower.contains("dormir") || lower.contains("calme") || lower.contains("chill") || lower.contains("nuit") -> {
                "🌙 **Ambiance Sommeil & Détente** :\n" +
                        "• Activez le **Sleep Timer** (30 min) avec arrêt progressif en fondu.\n" +
                        "• Profitez de l'égalisation douce **Midnight Chill**.\n" +
                        "• Pour une immersion totale, masquez l'écran avec le mode minimaliste."
            }
            lower.contains("playlist") || lower.contains("ordre") -> {
                "🎵 **Conseil d'Architecture Musicale** :\n" +
                        "• Commencez par 2 titres d'introduction progressive.\n" +
                        "• Placez vos morceaux les plus percutants au tiers de la session.\n" +
                        "• Activez **Gapless Playback** pour un enchaînement direct sans silence entre les pistes !"
            }
            else -> {
                "✨ **SLM Musical Intelligence** :\n" +
                        "J'ai bien analysé votre demande. Vous pouvez configurer votre clé OpenAI directement dans les paramètres de cette fenêtre pour poser des questions personnalisées et composer des playlists sur mesure !"
            }
        }
    }
}
