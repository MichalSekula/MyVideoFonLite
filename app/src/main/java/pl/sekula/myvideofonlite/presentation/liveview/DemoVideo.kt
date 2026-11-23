package pl.sekula.myvideofonlite.presentation.liveview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pl.sekula.myvideofonlite.presentation.common.formatDateTime

/** Symulowany obraz kamery dla trybu demo – wyraźnie oznaczony jako symulacja. */
@Composable
fun DemoVideo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "demo")
    val x by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "visitor",
    )
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    Box(modifier.background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF263238))))
            // Ścieżka i furtka
            drawRect(Color(0xFF455A64), topLeft = Offset(0f, size.height * 0.75f), size = Size(size.width, size.height * 0.25f))
            drawRect(
                Color(0xFF6D4C41),
                topLeft = Offset(size.width * 0.4f, size.height * 0.3f),
                size = Size(size.width * 0.2f, size.height * 0.45f),
            )
            // "Gość"
            val cx = size.width * x
            drawCircle(Color(0xFFB0BEC5), radius = size.minDimension * 0.06f, center = Offset(cx, size.height * 0.5f))
            drawRect(
                Color(0xFF90A4AE),
                topLeft = Offset(cx - size.minDimension * 0.06f, size.height * 0.57f),
                size = Size(size.minDimension * 0.12f, size.height * 0.18f),
            )
        }
        Text(
            "DEMO • obraz symulowany",
            color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        Text(
            formatDateTime(now),
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        )
    }
}
