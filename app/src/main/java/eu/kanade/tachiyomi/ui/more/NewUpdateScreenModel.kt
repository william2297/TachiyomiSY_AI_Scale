package eu.kanade.tachiyomi.ui.more

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

@AssistedInject
class NewUpdateScreenModel(
    @Assisted changelogInfo: String,
    @Assisted private val downloadLink: String,
    private val context: Context,
    private val network: NetworkHelper,
    private val preferences: BasePreferences,
) : ViewModel() {

    val state: StateFlow<NewUpdateScreenModel.State>
        field = MutableStateFlow<NewUpdateScreenModel.State>(State(changelogInfo = changelogInfo))

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(changelogInfo: String, downloadLink: String): NewUpdateScreenModel
    }

    private val apkFile: File
        get() = File(context.externalCacheDir, "update.apk")

    private var downloadJob: Job? = null

    fun startDownload() {
        if (downloadJob?.isActive == true) return

        downloadJob = viewModelScope.launch {
            state.update { it.copy(downloadProgress = 0, stage = Stage.Downloading) }
            try {
                withContext(Dispatchers.IO) { downloadApk() }
                state.update { it.copy(downloadProgress = 100, stage = Stage.Downloaded) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                apkFile.delete()
                context.toast("Download failed: ${e.message ?: e::class.java.simpleName}")
                state.update { it.copy(stage = Stage.Failed) }
            }
        }
    }

    /**
     * Downloads the APK, picking up where it stopped when the connection drops (a few times), because on a weak
     * connection one long transfer rarely finishes in a single go.
     */
    private suspend fun downloadApk() {
        apkFile.delete()
        var failures = 0
        while (true) {
            try {
                downloadFrom(apkFile.length())
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                logcat(LogPriority.WARN, e)
                if (++failures > MAX_DOWNLOAD_RETRIES) throw e
                delay(RETRY_DELAY_MS)
            }
        }
    }

    private suspend fun downloadFrom(existingSize: Long) {
        val progressListener = object : ProgressListener {
            // Progress of the download
            var savedProgress = 0

            // Keep track of the last update sent to avoid updating the state too often.
            var lastTick = 0L

            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val total = if (existingSize > 0) contentLength + existingSize else contentLength
                if (total <= 0) return
                val progress = (100 * (bytesRead.toFloat() / total)).toInt()
                val currentTime = System.currentTimeMillis()
                if (progress > savedProgress && currentTime - 200 > lastTick) {
                    savedProgress = progress
                    lastTick = currentTime
                    state.update { it.copy(downloadProgress = progress) }
                }
            }
        }

        // The shared client gives a whole call two minutes, which a 150 MB APK on a slow connection exceeds
        // every time. Only a stalled connection (no bytes for a minute) should fail.
        val client = network.client.newBuilder()
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val response = client.newCachelessCallWithProgress(GET(downloadLink), progressListener, existingSize)
            .awaitSuccess()
        // 206 means the server continued from where the file ended; anything else sends the whole file again
        val append = existingSize > 0 && response.code == 206
        apkFile.parentFile?.mkdirs()
        response.body.source().saveTo(FileOutputStream(apkFile, append))
    }

    fun skipUpdate(versionName: String) {
        downloadJob?.cancel()
        preferences.skippedAppUpdate.set(versionName)
    }

    fun installUpdate() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkFile.getUriCompat(context), ExtensionInstaller.APK_MIME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    @Immutable
    data class State(
        val changelogInfo: String,
        val downloadProgress: Int = 0,
        val stage: Stage = Stage.Available,
    )

    enum class Stage {
        Available,
        Downloading,
        Downloaded,
        Failed,
    }
}

private const val MAX_DOWNLOAD_RETRIES = 6
private const val RETRY_DELAY_MS = 2_000L
