package com.example.slmplay.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Download item status
 */
enum class DownloadStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

/**
 * Sniffed media item detected on a webpage
 */
data class SniffedMediaItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val extension: String,
    val mediaType: VaultFileType,
    val estimatedSize: String = ""
)

/**
 * Download tracking model
 */
data class DownloadItem(
    val id: Long,
    val title: String,
    val url: String,
    val destinationPath: String,
    val isPrivateVault: Boolean,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val progressPercent: Int = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Universal Download Manager handling standard Android DownloadManager,
 * hidden private vault direct downloads, and media stream sniffing.
 */
class UniversalDownloadManager(
    private val context: Context,
    private val securityVaultManager: SecurityVaultManager
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val okHttpClient = OkHttpClient()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var trackerJob: Job? = null

    private val _activeDownloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val activeDownloads: StateFlow<List<DownloadItem>> = _activeDownloads.asStateFlow()

    private val _sniffedMedia = MutableStateFlow<List<SniffedMediaItem>>(emptyList())
    val sniffedMedia: StateFlow<List<SniffedMediaItem>> = _sniffedMedia.asStateFlow()

    init {
        startProgressTracker()
    }

    /**
     * Launch standard public download via Android DownloadManager.
     * Accessible in system Downloads folder and file managers.
     */
    fun startPublicDownload(url: String, userAgent: String? = null, suggestedTitle: String? = null): Long {
        return try {
            val uri = Uri.parse(url)
            val fileName = suggestedTitle?.takeIf { it.isNotBlank() }
                ?: URLUtil.guessFileName(url, null, getMimeType(url))
                ?: "download_${System.currentTimeMillis()}"

            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setDescription("Téléchargement en cours...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookie)
                }
                if (!userAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }
            }

            val downloadId = downloadManager.enqueue(request)

            val item = DownloadItem(
                id = downloadId,
                title = fileName,
                url = url,
                destinationPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName",
                isPrivateVault = false,
                status = DownloadStatus.RUNNING
            )

            val current = _activeDownloads.value.toMutableList()
            current.add(0, item)
            _activeDownloads.value = current

            downloadId
        } catch (e: Exception) {
            Log.e("UniversalDownloadManager", "Failed to start public download", e)
            -1L
        }
    }

    /**
     * Launch hidden private download directly to the App's secure vault folder.
     * Completely hidden from public Android storage and other apps.
     */
    fun startPrivateVaultDownload(url: String, suggestedTitle: String? = null): Long {
        val downloadId = System.currentTimeMillis()
        val fileName = suggestedTitle?.takeIf { it.isNotBlank() }
            ?: URLUtil.guessFileName(url, null, getMimeType(url))
            ?: "vault_${System.currentTimeMillis()}"

        val vaultDir = securityVaultManager.getVaultDirectory()
        val targetFile = File(vaultDir, fileName)

        val item = DownloadItem(
            id = downloadId,
            title = fileName,
            url = url,
            destinationPath = targetFile.absolutePath,
            isPrivateVault = true,
            status = DownloadStatus.RUNNING
        )

        val current = _activeDownloads.value.toMutableList()
        current.add(0, item)
        _activeDownloads.value = current

        // Perform streaming download to private vault
        coroutineScope.launch {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    updateItemStatus(downloadId, DownloadStatus.FAILED, "Erreur serveur: ${response.code}")
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    updateItemStatus(downloadId, DownloadStatus.FAILED, "Corps vide")
                    return@launch
                }

                val contentLength = body.contentLength()
                var downloadedBytes = 0L

                targetFile.parentFile?.mkdirs()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    val percent = if (contentLength > 0) ((downloadedBytes * 100) / contentLength).toInt() else 50
                    updateItemProgress(downloadId, downloadedBytes, contentLength, percent)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                updateItemStatus(downloadId, DownloadStatus.SUCCESS)
                securityVaultManager.refreshVaultFiles()
            } catch (e: Exception) {
                Log.e("UniversalDownloadManager", "Private vault download failed", e)
                updateItemStatus(downloadId, DownloadStatus.FAILED, e.localizedMessage ?: "Échec du téléchargement")
            }
        }

        return downloadId
    }

    /**
     * Periodically tracks DownloadManager progress
     */
    private fun startProgressTracker() {
        trackerJob?.cancel()
        trackerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000)
                val list = _activeDownloads.value
                val hasRunningPublic = list.any { !it.isPrivateVault && it.status == DownloadStatus.RUNNING }
                if (hasRunningPublic) {
                    pollDownloadManager()
                }
            }
        }
    }

    private fun pollDownloadManager() {
        try {
            val list = _activeDownloads.value.toMutableList()
            var modified = false

            for (i in list.indices) {
                val item = list[i]
                if (item.isPrivateVault || item.status != DownloadStatus.RUNNING) continue

                val query = DownloadManager.Query().setFilterById(item.id)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesSoFarIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    val bytesSoFar = if (bytesSoFarIdx != -1) cursor.getLong(bytesSoFarIdx) else 0L
                    val bytesTotal = if (bytesTotalIdx != -1) cursor.getLong(bytesTotalIdx) else 0L
                    val dmStatus = if (statusIdx != -1) cursor.getInt(statusIdx) else -1

                    val newStatus = when (dmStatus) {
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESS
                        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        else -> DownloadStatus.RUNNING
                    }

                    val percent = if (bytesTotal > 0) ((bytesSoFar * 100) / bytesTotal).toInt() else item.progressPercent

                    list[i] = item.copy(
                        downloadedBytes = bytesSoFar,
                        totalBytes = bytesTotal,
                        progressPercent = percent,
                        status = newStatus
                    )
                    modified = true
                    cursor.close()
                }
            }

            if (modified) {
                _activeDownloads.value = list
            }
        } catch (e: Exception) {
            Log.e("UniversalDownloadManager", "Error polling DownloadManager", e)
        }
    }

    private fun updateItemProgress(id: Long, downloaded: Long, total: Long, percent: Int) {
        val list = _activeDownloads.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                downloadedBytes = downloaded,
                totalBytes = total,
                progressPercent = percent
            )
            _activeDownloads.value = list
        }
    }

    private fun updateItemStatus(id: Long, status: DownloadStatus, error: String? = null) {
        val list = _activeDownloads.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                status = status,
                errorMessage = error,
                progressPercent = if (status == DownloadStatus.SUCCESS) 100 else list[idx].progressPercent
            )
            _activeDownloads.value = list
        }
    }

    /**
     * Updates sniffed media list when scanned from a webpage
     */
    fun setSniffedMedia(mediaList: List<SniffedMediaItem>) {
        _sniffedMedia.value = mediaList
    }

    fun clearSniffedMedia() {
        _sniffedMedia.value = emptyList()
    }

    /**
     * Clears completed or failed downloads from list
     */
    fun clearFinishedDownloads() {
        _activeDownloads.value = _activeDownloads.value.filter { it.status == DownloadStatus.RUNNING }
    }

    /**
     * Generates standard JavaScript to inject into WebView to sniff media links
     */
    fun getMediaSnifferScript(): String {
        return """
            (function() {
                var results = [];
                var seen = {};

                function add(url, type, title) {
                    if (!url || url.startsWith('data:') || url.startsWith('blob:') || seen[url]) return;
                    seen[url] = true;
                    results.push({ url: url, type: type, title: title || '' });
                }

                // 1. Videos
                document.querySelectorAll('video, video source').forEach(function(v) {
                    var src = v.src || v.currentSrc;
                    if (src) add(src, 'video', document.title || 'Vidéo Web');
                });

                // 2. Audio
                document.querySelectorAll('audio, audio source').forEach(function(a) {
                    var src = a.src || a.currentSrc;
                    if (src) add(src, 'audio', document.title || 'Piste Audio');
                });

                // 3. Download links
                document.querySelectorAll('a[href]').forEach(function(a) {
                    var href = a.href;
                    var lower = href.toLowerCase();
                    if (lower.match(/\.(mp3|m4a|wav|flac|aac|ogg|mp4|webm|mkv|pdf|zip|rar|apk|png|jpg|jpeg|webp)($|\?)/)) {
                        var ext = 'doc';
                        if (lower.match(/\.(mp3|m4a|wav|flac|aac|ogg)($|\?)/)) ext = 'audio';
                        else if (lower.match(/\.(mp4|webm|mkv)($|\?)/)) ext = 'video';
                        else if (lower.match(/\.(png|jpg|jpeg|webp)($|\?)/)) ext = 'image';
                        add(href, ext, a.innerText.trim() || a.getAttribute('download') || 'Fichier');
                    }
                });

                // 4. Large images
                document.querySelectorAll('img[src]').forEach(function(img) {
                    var src = img.src;
                    if (src && (img.naturalWidth > 200 || img.width > 200)) {
                        add(src, 'image', img.alt || 'Image Web');
                    }
                });

                return JSON.stringify(results.slice(0, 30));
            })();
        """.trimIndent()
    }

    companion object {
        fun getMimeType(url: String): String {
            val ext = MimeTypeMap.getFileExtensionFromUrl(url)
            return if (!ext.isNullOrBlank()) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
            } else {
                "application/octet-stream"
            }
        }
    }
}
