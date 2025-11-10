package com.example.reloj_locutor

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import androidx.lifecycle.lifecycleScope
import com.example.reloj_locutor.ui.theme.Reloj_LocutorTheme
import androidx.compose.ui.graphics.drawscope.translate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Reloj_LocutorTheme(onPlay = { h, m -> playHour(h, m) }) {
                UIPrincipal(onPlay = { h, m -> playHour(h, m) })
            }
        }
    }

    private fun playSegment(resId: Int, segmentIndex: Int, onEnd: () -> Unit = {}) {
        val mp = MediaPlayer.create(this, resId)
        val startMs = segmentIndex * 2000
        val overlap = -170 // solapamiento suave

        mp.setOnPreparedListener {
            mp.seekTo(startMs)
        }

        mp.setOnSeekCompleteListener {
            mp.start()

            // Usamos corutina en el hilo principal para cortar suavemente
            lifecycleScope.launch {
                delay((2000 - overlap).toLong()) // duración efectiva del segmento
                if (mp.isPlaying) mp.stop()
                mp.release()
                onEnd()
            }
        }
    }



    private fun playHour(hora: Int, minuto: Int) {
        val scope = lifecycleScope

        val formato12 = if (hora % 12 == 0) 12 else hora % 12
        val esUna = formato12 == 1

        // Selección de segmento del audio 1
        val segmentoSonEs = if (esUna) 1 else 0 // Es la =1, Son las=0
        val segmentoCon = 2

        // Tiempo del día (audio 3)
        val segmentoTiempo = when (hora) {
            in 0..11 -> 0
            in 12..19 -> 1
            else -> 2
        }

        // Número para audio 2 (hora verbal)
        val indiceHora = formato12 - 1
        val realHoraIndex = if (indiceHora <= 1) indiceHora else indiceHora + 1

        // Número para minutos (audio 2)
        val realMinIndex = when (minuto) {
            0 -> null
            1 -> null
            else -> {
                val base = minuto - 1
                if (base <= 1) base else base + 1
            }
        }

        // audio 4 selector
        val segmentoMinPalabra = when (minuto) {
            0 -> 2 // en punto
            1 -> 0 // un minuto
            else -> 1 // minutos
        }

        scope.launch {
            // "Son las" / "Es la"
            playSegment(R.raw.reloj_1, segmentoSonEs) {
                // hora en palabras
                playSegment(R.raw.reloj_2, realHoraIndex) {
                    // "de la mañana / tarde / noche"
                    playSegment(R.raw.reloj_3, segmentoTiempo) {
                        if (minuto == 0) {
                            // en punto
                            playSegment(R.raw.reloj_4, segmentoMinPalabra)
                        } else if (minuto == 1) {
                            // con + un minuto
                            playSegment(R.raw.reloj_1, segmentoCon) {
                                playSegment(R.raw.reloj_4, segmentoMinPalabra)
                            }
                        } else {
                            // con + número + minutos
                            playSegment(R.raw.reloj_1, segmentoCon) {
                                playSegment(R.raw.reloj_2, realMinIndex!!) {
                                    playSegment(R.raw.reloj_4, segmentoMinPalabra)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Reloj_LocutorTheme(
    onPlay: (Int, Int) -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme {
        content()
    }
}

@Composable
fun UIPrincipal(onPlay: (Int, Int) -> Unit) {
    val ahora = remember { mutableStateOf(LocalTime.now()) }

    // Actualizar cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ahora.value = LocalTime.now()
        }
    }

    val hora = ahora.value.hour
    val minuto = ahora.value.minute

    val formato12 = if (hora % 12 == 0) 12 else hora % 12
    val amPm = if (hora < 12) "am" else "pm"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(70.dp))
        // Reloj tipo Flip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            FlipBox(text = formato12.toString())
            Spacer(modifier = Modifier.width(16.dp))
            FlipBox(text = minuto.toString().padStart(2, '0'))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = amPm.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(45.dp))

        Button(
            onClick = { onPlay(hora, minuto) },
            modifier = Modifier
                .width(160.dp)
                .height(48.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
            Text(text = "Escuchar hora", fontSize = 16.sp)
        }
    }
}

@Composable
fun FlipBox(text: String) {
    Box(
        modifier = Modifier
            .size(120.dp, 130.dp)
            .background(Color(0xFF1A1A1A), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}