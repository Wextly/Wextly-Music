package com.wextly.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Всё, что нужно экрану плеера для отрисовки. */
data class PlayerUi(
    val hasMedia: Boolean = false,
    val title: String = "Трек не выбран",
    val artist: String = "",
    val artPath: String? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val nextTitle: String? = null,
    val index: Int = 0,
    val count: Int = 0,
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val store = LibraryStore(app)

    private val _tracks = MutableStateFlow(store.load())
    val tracks: StateFlow<List<Track>> = _tracks

    private val _ui = MutableStateFlow(PlayerUi())
    val ui: StateFlow<PlayerUi> = _ui

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
        }
    }

    init {
        // Подключаемся к службе воспроизведения (она сама запустится)
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener(
            {
                try {
                    controller = future.get().also { it.addListener(listener) }
                    syncState()
                } catch (e: Exception) {
                    controller = null
                }
            },
            ContextCompat.getMainExecutor(app),
        )
    }

    private fun syncState() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val track = item?.mediaId?.let { id -> _tracks.value.firstOrNull { it.id == id } }
        val nextTitle = if (c.hasNextMediaItem()) {
            c.getMediaItemAt(c.nextMediaItemIndex).mediaMetadata.title?.toString()
        } else {
            null
        }
        val duration = c.duration
        _ui.value = PlayerUi(
            hasMedia = item != null,
            title = item?.mediaMetadata?.title?.toString() ?: "Трек не выбран",
            artist = item?.mediaMetadata?.artist?.toString() ?: "",
            artPath = track?.artPath,
            isPlaying = c.isPlaying,
            durationMs = if (duration == C.TIME_UNSET || duration < 0) {
                track?.durationMs ?: 0L
            } else {
                duration
            },
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            nextTitle = nextTitle,
            index = c.currentMediaItemIndex,
            count = c.mediaItemCount,
        )
    }

    // ---------- управление воспроизведением ----------

    fun position(): Long = controller?.currentPosition ?: 0L

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    /** Запускает список треков как очередь, начиная с выбранного. */
    fun playTracks(list: List<Track>, startIndex: Int) {
        if (list.isEmpty()) return
        viewModelScope.launch {
            val items = withContext(Dispatchers.Default) { list.map { buildItem(it) } }
            val c = controller ?: return@launch
            c.setMediaItems(items, startIndex.coerceIn(0, items.size - 1), 0L)
            c.prepare()
            c.play()
        }
    }

    private fun buildItem(t: Track): MediaItem {
        val art = t.artPath?.let { path -> runCatching { File(path).readBytes() }.getOrNull() }
        val meta = MediaMetadata.Builder()
            .setTitle(t.title)
            .setArtist(t.artist)
            .apply {
                if (art != null) setArtworkData(art, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
            .build()
        return MediaItem.Builder()
            .setMediaId(t.id)
            .setUri(Uri.parse(t.uri))
            .setMediaMetadata(meta)
            .build()
    }

    // ---------- библиотека ----------

    fun addUris(uris: List<Uri>) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        // Запоминаем доступ к файлу, чтобы он работал и после перезапуска
                        app.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (e: Exception) {
                        // не все источники дают постоянный доступ
                    }
                    store.readTrack(uri)
                }
            }
            val known = _tracks.value.map { it.id }.toSet()
            val merged = _tracks.value + added.filter { it.id !in known }
            _tracks.value = merged
            withContext(Dispatchers.IO) { store.save(merged) }
        }
    }

    fun removeTrack(id: String) {
        val merged = _tracks.value.filter { it.id != id }
        _tracks.value = merged
        viewModelScope.launch(Dispatchers.IO) { store.save(merged) }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
