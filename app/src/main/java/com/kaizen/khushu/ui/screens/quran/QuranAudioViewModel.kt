package com.kaizen.khushu.ui.screens.quran

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.kaizen.khushu.data.model.AyahBlock
import com.kaizen.khushu.data.model.ContentBlock
import com.kaizen.khushu.data.repository.QuranAudioRepository
import com.kaizen.khushu.data.worker.QuranAudioWorker
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class QuranAudioViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val AYAH_LOADING_TIMEOUT_MS = 15_000L
    }

    sealed interface AudioState {
        data object Idle : AudioState
        data object Loading : AudioState
        data object Playing : AudioState
        data object Paused : AudioState
        data class Error(val msg: String) : AudioState
    }

    val audioState = mutableStateOf<AudioState>(AudioState.Idle)
    val currentSurah = mutableStateOf<Int?>(null)
    val playingAyahIndex = mutableStateOf<Int?>(null)

    private var mediaController: MediaController? = null
    private var ayahLoadingTimeoutJob: Job? = null
    private var ayahLoadAttempt = 0L
    private var isAyahPlayback = false

    fun setController(controller: MediaController?) {
        this.mediaController = controller
        controller?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> audioState.value = AudioState.Loading
                    Player.STATE_READY -> {
                        cancelAyahLoadingTimeout()
                        audioState.value = AudioState.Playing
                    }

                    Player.STATE_ENDED -> handleAyahCompletion()
                    Player.STATE_IDLE -> {
                        if (audioState.value !is AudioState.Error) audioState.value = AudioState.Idle
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    cancelAyahLoadingTimeout()
                    audioState.value = AudioState.Playing
                } else if (audioState.value !is AudioState.Error) {
                    audioState.value = AudioState.Paused
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                cancelAyahLoadingTimeout()
                audioState.value = AudioState.Error("Could not load this verse.")
            }
        })
    }

    private val workManager = WorkManager.getInstance(application)

    private var currentBlocks: List<ContentBlock>? = null
    private var isSequenceMode: Boolean = false
    private var currentReciterId: String? = null

    private fun getAudioDir(reciterId: String): File {
        val dir = File(getApplication<Application>().filesDir, "audio/$reciterId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isReciterDownloaded(reciterId: String): Boolean {
        val dir = getAudioDir(reciterId)
        val files = dir.listFiles { _, name -> name.endsWith(".mp3") }
        return files?.size == 114
    }

    fun getReciterDownloadProgress(reciterId: String): Flow<Pair<Float, Int>?> {
        val tag = "reciter_download_$reciterId"
        return workManager.getWorkInfosByTagLiveData(tag).asFlow().map { infos ->
            val info = infos.firstOrNull()
            if (info?.state == WorkInfo.State.RUNNING) {
                val progress = info.progress.getFloat("progress", 0f)
                val count = info.progress.getInt("count", 0)
                progress to count
            } else if (info?.state == WorkInfo.State.SUCCEEDED) {
                1f to 114
            } else {
                null
            }
        }
    }

    fun playSurah(surahNumber: Int, url: String, reciterId: String) {
        stop()
        isAyahPlayback = false
        currentReciterId = reciterId
        isSequenceMode = false
        playingAyahIndex.value = null

        val localSurahFile = File(getAudioDir(reciterId), "$surahNumber.mp3")
        val dataSource = if (localSurahFile.exists()) localSurahFile.absolutePath else url

        playInternal(surahNumber, dataSource, reciterId)
    }

    private fun getAyahUrl(surahNumber: Int, ayahIndex: Int, blocks: List<ContentBlock>, reciterId: String): String? {
        val block = blocks.getOrNull(ayahIndex) as? AyahBlock ?: return null
        val paddedSurah = surahNumber.toString().padStart(3, '0')
        val paddedAyah = block.ayah.toString().padStart(3, '0')
        val fileName = "$paddedSurah$paddedAyah.mp3"

        val relativePath = when (reciterId) {
            "mishari" -> "Alafasy/mp3/$fileName"
            "abdulbaset" -> "AbdulBaset/Mujawwad/mp3/$fileName"
            "sudais" -> "Sudais/mp3/$fileName"
            "husary" -> "Husary_64kbps/$fileName"
            "minshawi" -> "Minshawi/Murattal/mp3/$fileName"
            else -> "Alafasy/mp3/$fileName"
        }

        return if (reciterId == "husary") {
            "https://mirrors.quranicaudio.com/everyayah/$relativePath"
        } else {
            "https://audio.qurancdn.com/$relativePath"
        }
    }

    fun playAyah(
        surahNumber: Int,
        ayahIndex: Int,
        blocks: List<ContentBlock>,
        reciterId: String,
        sequence: Boolean
    ) {
        stop()
        isAyahPlayback = true
        currentBlocks = blocks
        isSequenceMode = sequence
        playingAyahIndex.value = ayahIndex
        currentReciterId = reciterId

        val url = getAyahUrl(surahNumber, ayahIndex, blocks, reciterId) ?: return
        playInternal(surahNumber, url, reciterId)
    }

    private fun playInternal(surahNumber: Int, dataSource: String, reciterId: String) {
        currentSurah.value = surahNumber
        currentReciterId = reciterId
        audioState.value = AudioState.Loading

        val controller = mediaController ?: run {
            if (isAyahPlayback) audioState.value = AudioState.Error("Could not load this verse.")
            return
        }
        try {
            if (isAyahPlayback) startAyahLoadingTimeout(controller)
            val mediaItem = MediaItem.fromUri(dataSource)
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        } catch (e: Exception) {
            cancelAyahLoadingTimeout()
            audioState.value = AudioState.Error(
                if (isAyahPlayback) "Could not load this verse." else e.message ?: "Failed to play audio"
            )
        }
    }

    private fun startAyahLoadingTimeout(controller: MediaController) {
        cancelAyahLoadingTimeout()
        val attempt = ayahLoadAttempt
        ayahLoadingTimeoutJob = viewModelScope.launch {
            delay(AYAH_LOADING_TIMEOUT_MS)
            if (attempt == ayahLoadAttempt && audioState.value == AudioState.Loading) {
                audioState.value = AudioState.Error("Could not load this verse.")
                controller.stop()
            }
        }
    }

    private fun cancelAyahLoadingTimeout() {
        ayahLoadAttempt++
        ayahLoadingTimeoutJob?.cancel()
        ayahLoadingTimeoutJob = null
    }

    private fun handleAyahCompletion() {
        if (isSequenceMode) {
            val currentIdx = playingAyahIndex.value ?: -1
            val nextIdx = findNextAyahIndex(currentIdx + 1)
            playingAyahIndex.value = nextIdx

            if (nextIdx != null) {
                val surahNum = currentSurah.value ?: return
                val reciterId = currentReciterId ?: return
                val blocks = currentBlocks ?: return
                val nextUrl = getAyahUrl(surahNum, nextIdx, blocks, reciterId) ?: return

                val controller = mediaController ?: return
                startAyahLoadingTimeout(controller)
                controller.setMediaItem(MediaItem.fromUri(nextUrl))
                controller.prepare()
                controller.play()
            } else {
                stop()
            }
        } else {
            stop()
        }
    }

    private fun findNextAyahIndex(startIndex: Int): Int? {
        val blocks = currentBlocks ?: return null
        if (startIndex < 0 || startIndex >= blocks.size) return null
        for (i in startIndex until blocks.size) {
            if (blocks[i] is AyahBlock) return i
        }
        return null
    }

    fun downloadReciter(reciterId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val inputData = Data.Builder().putString("reciterId", reciterId).build()
        val request = OneTimeWorkRequestBuilder<QuranAudioWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("reciter_download_$reciterId")
            .build()
        workManager.enqueue(request)
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun stop() {
        cancelAyahLoadingTimeout()
        isAyahPlayback = false
        mediaController?.stop()
        audioState.value = AudioState.Idle
        playingAyahIndex.value = null
        currentSurah.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
