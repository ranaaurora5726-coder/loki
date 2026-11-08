package com.ranaaurora.loki

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.DisposableEffect
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Emerald = Color(0xFF18A66A)
private val Gold = Color(0xFFC9A45C)
private val Black = Color(0xFF050806)
private val SurfaceColor = Color(0xFF101713)

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LokiHome() }
    }
}

private fun audioPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun loadTracks(context: android.content.Context): List<Track> {
    val tracks = mutableListOf<Track>()

    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    context.contentResolver.query(
        collection,
        projection,
        selection,
        null,
        "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
    )?.use { cursor ->

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

        while (cursor.moveToNext()) {
            tracks.add(
                Track(
                    id = cursor.getLong(idColumn),
                    title = cursor.getString(titleColumn) ?: "Unknown Title",
                    artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                    album = cursor.getString(albumColumn) ?: "Unknown Album",
                    uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
                )
            )
        }
    }

    return tracks
}


@Composable
private fun TrackArtwork(track: Track, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val artwork by produceState<Bitmap?>(
        initialValue = null,
        key1 = track.uri
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, track.uri)
                val bytes = retriever.embeddedPicture
                retriever.release()
                bytes?.let {
                    android.graphics.BitmapFactory.decodeByteArray(
                        it, 0, it.size
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier.background(
            SurfaceColor,
            RoundedCornerShape(24.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!.asImageBitmap(),
                contentDescription = "Album Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(90.dp)
            )
        }
    }
}


private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun LokiHome() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val tracks = remember { mutableStateListOf<Track>() }
    var status by remember { mutableStateOf("Your music realm awaits") }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var progressMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                currentTrack = tracks.getOrNull(player.currentMediaItemIndex)
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            progressMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            delay(500)
        }
    }

    var permissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(audioPermission()) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    fun scanMusic() {
        try {
            tracks.clear()
            tracks.addAll(loadTracks(context))
            status = if (tracks.isEmpty()) {
                "No music files found on your device"
            } else {
                "${tracks.size} songs found in your library"
            }
        } catch (e: Exception) {
            status = "Unable to scan music files"
        }
    }

    fun playTrack(track: Track) {
        try {
            val index = tracks.indexOfFirst { it.id == track.id }
            if (index >= 0) {
                player.setMediaItems(
                    tracks.map { MediaItem.fromUri(it.uri) },
                    index,
                    0L
                )
                player.prepare()
                player.play()
                currentTrack = track
            }
        } catch (e: Exception) {
            status = "Unable to play this track"
        }
    }

    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        }
    }

    fun playPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else {
            player.seekTo(0L)
            player.play()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            scanMusic()
        } else {
            status = "Audio permission is required to scan music"
        }
    }

    BackHandler(enabled = showFullPlayer) {
        showFullPlayer = false
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Emerald,
            secondary = Gold,
            background = Black,
            surface = SurfaceColor,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Black
        ) {
            if (showFullPlayer && currentTrack != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showFullPlayer = false }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Gold
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            "NOW PLAYING",
                            color = Gold,
                            fontSize = 11.sp,
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.weight(1f))

                        Spacer(Modifier.size(48.dp))
                    }

                    Spacer(Modifier.height(18.dp))

                    TrackArtwork(
                        track = currentTrack!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                    )

                    Spacer(Modifier.height(28.dp))

                    Text(
                        currentTrack!!.title,
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        currentTrack!!.artist,
                        color = Gold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(26.dp))

                    Slider(
                        value = progressMs.toFloat().coerceIn(
                            0f,
                            durationMs.toFloat().coerceAtLeast(1f)
                        ),
                        onValueChange = {
                            progressMs = it.toLong()
                        },
                        onValueChangeFinished = {
                            player.seekTo(progressMs)
                        },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Gold,
                            activeTrackColor = Emerald,
                            inactiveTrackColor = SurfaceColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(progressMs),
                            color = Color(0xFF9AA79F),
                            fontSize = 11.sp
                        )
                        Text(
                            formatTime(durationMs),
                            color = Color(0xFF9AA79F),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { playPrevious() }) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Gold,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (player.isPlaying) player.pause()
                                else player.play()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    Emerald,
                                    RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                contentDescription = "Play or Pause",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        IconButton(onClick = { playNext() }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Gold,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        "LOKI  •  YOUR MUSIC. YOUR REALM.",
                        color = Color(0xFF69766E),
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "LOKI",
                                color = Gold,
                                fontSize = 15.sp,
                                letterSpacing = 6.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "THE GOD OF SOUND",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "YOUR MUSIC. YOUR REALM.",
                                color = Emerald,
                                fontSize = 10.sp,
                                letterSpacing = 2.5.sp
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = Gold
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(SurfaceColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Scan Music Library") },
                                    onClick = {
                                        menuExpanded = false
                                        if (permissionGranted) {
                                            scanMusic()
                                        } else {
                                            permissionLauncher.launch(audioPermission())
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Refresh Library") },
                                    onClick = {
                                        menuExpanded = false
                                        if (permissionGranted) scanMusic()
                                        else permissionLauncher.launch(audioPermission())
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "YOUR LIBRARY",
                            color = Gold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "${tracks.size} TRACKS",
                            color = Emerald,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (tracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(
                                    SurfaceColor,
                                    RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Emerald,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    status,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Open the menu to scan your music",
                                    color = Color(0xFF9AA79F),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (currentTrack?.id == track.id)
                                                Color(0xFF14251C)
                                            else
                                                SurfaceColor,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { playTrack(track) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrackArtwork(
                                        track = track,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            track.artist,
                                            color = Color(0xFF9AA79F),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }

                                    if (currentTrack?.id == track.id && isPlaying) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Now playing",
                                            tint = Emerald
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (currentTrack != null) {
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    SurfaceColor,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { showFullPlayer = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtwork(
                                track = currentTrack!!,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentTrack!!.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    currentTrack!!.artist,
                                    color = Color(0xFF9AA79F),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (player.isPlaying) player.pause()
                                    else player.play()
                                }
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause
                                    else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Emerald,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "DESIGNED & DEVELOPED BY RANA HOSSEN",
                        color = Color(0xFF69766E),
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
