package com.aurora.launcher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Slowly drifting colour blobs on a dark base. Shifts a little as you swipe between pages. */
@Composable
fun AuroraBackground(scroll: () -> Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val a by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "a"
    )
    val b by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(31000, easing = LinearEasing), RepeatMode.Restart),
        label = "b"
    )
    val c by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(41000, easing = LinearEasing), RepeatMode.Restart),
        label = "c"
    )

    Canvas(modifier.fillMaxSize()) {
        drawRect(Color(0xFF07070D))
        val w = size.width
        val h = size.height
        val twoPi = (2.0 * PI).toFloat()
        val shift = scroll() * w * 0.12f

        fun blob(color: Color, cx: Float, cy: Float, r: Float) = drawBlob(color, cx, cy, r)

        blob(Color(0xCC6C4DFF), w * (0.20f + 0.22f * sin(a * twoPi)) - shift, h * (0.22f + 0.10f * cos(a * twoPi)), w * 0.95f)
        blob(Color(0xB300C2FF), w * (0.85f + 0.18f * cos(b * twoPi)) - shift * 1.4f, h * (0.45f + 0.12f * sin(b * twoPi)), w * 0.85f)
        blob(Color(0xB3FF4D9D), w * (0.35f + 0.25f * sin(c * twoPi + 1.3f)) - shift * 0.7f, h * (0.82f + 0.08f * cos(c * twoPi)), w * 0.9f)
        blob(Color(0x663DFFB5), w * (0.75f + 0.15f * sin(b * twoPi + 2f)) - shift * 1.1f, h * (0.12f + 0.08f * sin(a * twoPi + 0.5f)), w * 0.6f)
    }
}

private fun DrawScope.drawBlob(color: Color, cx: Float, cy: Float, radius: Float) {
    val center = Offset(cx, cy)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
