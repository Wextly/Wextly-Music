package com.wextly.player

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlinx.coroutines.delay

private val Accent = Color(0xFF7C5CFF)
private val Background = Color(0xFF14142B)
private val Surface = Color(0xFF1C1C38)
private val NavBackground = Color(0xFF101026)

@Composable
fun WextlyApp(vm: PlayerViewModel) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Background,
            surface = Surface,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
    ) {
        var tab by remember { mutableIntStateOf(0) }
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = NavBackground) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                        label = { Text("Плеер") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                        label = { Text("Библиотека") },
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Настройки") },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> PlayerScreen(vm)
                    1 -> LibraryScreen(vm, onPlayed = { tab = 0 })
                    else -> SettingsScreen()
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun PlayerScreen(vm: PlayerViewModel) {
    val ui by vm.ui.collectAsState()
    var position by remember { mutableLongStateOf(0L) }
    var dragValue by remember { mutableStateOf<Float?>(null) }

    // Положение в треке опрашиваем 4 раза в секунду, пока открыт экран
    LaunchedEffect(Unit) {
        while (true) {
            if (dragValue == null) position = vm.position()
            delay(250)
        }
    }

    val duration = ui.durationMs.coerceAtLeast(1L)
    val shown = dragValue ?: (position.toFloat() / duration).coerceIn(0f, 1f)
    val bitmap = remember(ui.artPath) {
        ui.artPath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Accent, Color(0xFF24124D)))),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(96.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            ui.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            ui.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(12.dp))
        Slider(
            value = shown,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { vm.seekTo((it * duration).toLong()) }
                dragValue = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                formatTime((shown * duration).toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                if (ui.durationMs > 0) formatTime(ui.durationMs) else "--:--",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.toggleShuffle() }) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Перемешать",
                    tint = if (ui.shuffle) Accent else Color.White.copy(alpha = 0.5f),
                )
            }
            IconButton(onClick = { vm.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Назад", modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .clickable { vm.togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (ui.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (ui.isPlaying) "Пауза" else "Играть",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Далее", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { vm.cycleRepeat() }) {
                Icon(
                    if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Повтор",
                    tint = if (ui.repeatMode == Player.REPEAT_MODE_OFF) {
                        Color.White.copy(alpha = 0.5f)
                    } else {
                        Accent
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        if (ui.hasMedia) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface)
                    .padding(14.dp),
            ) {
                Text(
                    "Трек ${ui.index + 1} из ${ui.count}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Text(
                    if (ui.nextTitle != null) "Далее: ${ui.nextTitle}" else "Дальше ничего не играет",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun LibraryScreen(vm: PlayerViewModel, onPlayed: () -> Unit) {
    val tracks by vm.tracks.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.addUris(uris)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Библиотека",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить музыку")
            }
        }
        Text(
            "Треков: ${tracks.size}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(8.dp))

        if (tracks.isEmpty()) {
            Text(
                "Здесь пока пусто. Нажми «+» вверху и выбери музыку на телефоне.",
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    onClick = {
                        vm.playTracks(tracks, index)
                        onPlayed()
                    },
                    onDelete = { vm.removeTrack(track.id) },
                )
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Убрать из библиотеки",
                tint = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Wextly Player 0.1.0 (этап 1)", color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        Text(
            "Плеер работает в фоне, показывается в шторке и на экране блокировки, " +
                "управляется кнопками наушников и Bluetooth. Плейлисты, поиск и темы " +
                "появятся на следующих этапах.",
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}
