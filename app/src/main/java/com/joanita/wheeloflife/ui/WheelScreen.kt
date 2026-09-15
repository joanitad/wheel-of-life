package com.joanita.wheeloflife.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joanita.wheeloflife.WheelViewModel
import com.joanita.wheeloflife.Weights
import com.joanita.wheeloflife.data.CategoryWithTasks
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class Segment(val cwt: CategoryWithTasks, val startDeg: Float, val sweepDeg: Float)

@Composable
fun WheelScreen(vm: WheelViewModel, onOpenCategory: (Long) -> Unit) {
    val cats by vm.categories.collectAsState()
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CategoryWithTasks?>(null) }

    val totalWeight = cats.sumOf { Weights.weight(it) }
    val segments = remember(cats) {
        var start = -90f
        cats.map { cwt ->
            val sweep = (Weights.weight(cwt) / cats.sumOf { Weights.weight(it) } * 360.0).toFloat()
            Segment(cwt, start, sweep).also { start += sweep }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wheel of Life", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Bigger slices = areas that need more of you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(12.dp))

        if (cats.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Add some life areas on the Areas tab to build your wheel.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Column
        }

        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Wheel(segments, rotation.value)
            // pointer at the top
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val p = Path().apply {
                    moveTo(cx - 24f, 0f)
                    lineTo(cx + 24f, 0f)
                    lineTo(cx, 56f)
                    close()
                }
                drawPath(p, Color(0xFF4A4A46))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !spinning && cats.isNotEmpty(),
            onClick = {
                val chosenId = vm.pickWeighted(cats)
                val seg = segments.first { it.cwt.category.id == chosenId }
                // land pointer (at -90° i.e. top) inside the chosen segment, at a random
                // spot within its middle 80% so it doesn't sit on a boundary
                val within = seg.startDeg + seg.sweepDeg * (0.1f + Random.nextFloat() * 0.8f)
                val current = rotation.value % 360f
                val target = rotation.value - current + 5 * 360f + (-90f - within)
                spinning = true
                scope.launch {
                    rotation.animateTo(
                        target,
                        tween(durationMillis = 4000, easing = CubicBezierEasing(0.15f, 0.9f, 0.25f, 1f))
                    )
                    spinning = false
                    result = seg.cwt
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (spinning) "Spinning…" else "SPIN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    result?.let { cwt ->
        val nextTask = cwt.tasks.filter { !it.done }
            .sortedWith(compareBy(nullsLast()) { it.deadline })
            .firstOrNull()
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text("Focus on: ${cwt.category.name}") },
            text = {
                Column {
                    if (nextTask != null) {
                        Text("Next up:", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(nextTask.title, style = MaterialTheme.typography.bodyLarge)
                        nextTask.deadline?.let {
                            val d = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            Text(
                                "Due ${d.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    } else {
                        Text("No open tasks here yet — add one!")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onOpenCategory(cwt.category.id); result = null }) { Text("Open tasks") }
            },
            dismissButton = { TextButton(onClick = { result = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun Wheel(segments: List<Segment>, rotationDeg: Float) {
    val labelColor = MaterialTheme.colorScheme.onBackground
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize().padding(8.dp)) {
        val d = min(size.width, size.height)
        val radius = d / 2f
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        val center = Offset(size.width / 2f, size.height / 2f)

        rotate(rotationDeg, pivot = center) {
            segments.forEach { seg ->
                drawArc(
                    color = WheelPalette[seg.cwt.category.colorIndex % WheelPalette.size],
                    startAngle = seg.startDeg,
                    sweepAngle = seg.sweepDeg,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(d, d)
                )
            }
            // labels along each slice's mid-angle
            segments.forEach { seg ->
                val mid = Math.toRadians((seg.startDeg + seg.sweepDeg / 2f).toDouble())
                val r = radius * 0.62f
                val x = center.x + (r * cos(mid)).toFloat()
                val y = center.y + (r * sin(mid)).toFloat()
                val segColor = WheelPalette[seg.cwt.category.colorIndex % WheelPalette.size]
                val paint = android.graphics.Paint().apply {
                    color = onWheelColor(segColor).let {
                        android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt())
                    }
                    textSize = with(density) { 13.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate(-rotationDeg, x, y) // keep text upright-ish relative to slice
                    drawText(seg.cwt.category.name, x, y, paint)
                    restore()
                }
            }
        }
        // hub
        drawCircle(Color(0xFFFAF6EE), radius * 0.12f, center)
        drawCircle(labelColor.copy(alpha = 0.5f), radius * 0.12f, center, style = Stroke(3f))
    }
}
