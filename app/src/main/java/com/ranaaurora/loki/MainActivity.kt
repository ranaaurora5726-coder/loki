package com.ranaaurora.loki

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
    val album: String
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
                    album = cursor.getString(albumColumn) ?: "Unknown Album"
                )
            )
        }
    }

    return tracks
}

@Composable
fun LokiHome() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val tracks = remember { mutableStateListOf<Track>() }
    var status by remember { mutableStateOf("Your music realm awaits") }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
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

                Spacer(Modifier.height(24.dp))

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

                Button(
                    onClick = {
                        if (permissionGranted) {
                            scanMusic()
                        } else {
                            permissionLauncher.launch(audioPermission())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("SCAN MUSIC LIBRARY")
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
                                "Scan your device to discover music",
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
                                        SurfaceColor,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Gold,
                                    modifier = Modifier.size(28.dp)
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
                            }
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
