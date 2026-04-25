package com.example.financetracker.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Float) : DownloadStatus()
    object Downloaded : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

class ModelManager(private val context: Context) {
    companion object {
        private const val TAG = "ModelManager"
        const val MODEL_FILENAME = "gemma-4-E2B-it-int4.task"
        const val MODEL_URL = "https://huggingface.co/google/gemma-2b-it-gguf/resolve/main/gemma-2b-it.gguf" // Replace with actual URL later if needed, but the prompt says use a configurable remote URL.
    }

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: Flow<DownloadStatus> = _downloadStatus.asStateFlow()

    fun getModelFile(): File {
        return File(context.filesDir, MODEL_FILENAME)
    }

    fun isModelDownloaded(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() > 0 // Add size check in reality, but for now just exists
    }

    suspend fun downloadModel(url: String = MODEL_URL) = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            _downloadStatus.value = DownloadStatus.Downloaded
            return@withContext
        }

        _downloadStatus.value = DownloadStatus.Downloading(0f)
        val file = getModelFile()
        val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                _downloadStatus.value = DownloadStatus.Error("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                return@withContext
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(tempFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            var lastProgressUpdate = 0L

            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)

                if (fileLength > 0) {
                    val progress = (total * 100f / fileLength)
                    val currentTime = System.currentTimeMillis()
                    // Update UI at most every 100ms
                    if (currentTime - lastProgressUpdate > 100) {
                        _downloadStatus.value = DownloadStatus.Downloading(progress / 100f)
                        lastProgressUpdate = currentTime
                    }
                }
            }

            output.flush()
            output.close()
            input.close()

            if (fileLength > 0 && total < fileLength) {
                // Incomplete download
                tempFile.delete()
                _downloadStatus.value = DownloadStatus.Error("Incomplete download")
            } else {
                if (tempFile.renameTo(file)) {
                    _downloadStatus.value = DownloadStatus.Downloaded
                } else {
                    tempFile.delete()
                    _downloadStatus.value = DownloadStatus.Error("Failed to rename temporary file")
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            tempFile.delete() // Cleanup on failure
            _downloadStatus.value = DownloadStatus.Error(e.message ?: "Unknown error occurred")
        }
    }
}
