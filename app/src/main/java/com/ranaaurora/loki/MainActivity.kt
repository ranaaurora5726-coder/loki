package com.ranaaurora.loki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Emerald = Color(0xFF18A66A)
private val Gold = Color(0xFFC9A45C)
private val Black = Color(0xFF050806)
private val Surface = Color(0xFF101713)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LokiHome() }
    }
}

@Composable
fun LokiHome() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Emerald,
            secondary = Gold,
            background = Black,
            surface = Surface,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Black) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 28.dp)
            ) {
                Text(
                    text = "LOKI",
                    color = Gold,
                    fontSize = 15.sp,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "THE GOD OF SOUND",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "YOUR MUSIC. YOUR REALM.",
                    color = Emerald,
                    fontSize = 10.sp,
                    letterSpacing = 2.5.sp
                )
                Spacer(Modifier.height(34.dp))
                Text("YOUR LIBRARY", color = Gold, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Surface, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Emerald,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Your music realm awaits", color = Color.White, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Music library will appear here",
                            color = Color(0xFF9AA79F),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Gold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("NOTHING PLAYING", color = Color.White, fontSize = 13.sp)
                        Text("Select a track from your library", color = Color(0xFF9AA79F), fontSize = 11.sp)
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
