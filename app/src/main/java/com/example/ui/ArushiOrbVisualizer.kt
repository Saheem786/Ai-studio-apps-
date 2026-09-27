package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArushiCyan
import com.example.ui.theme.ArushiCyanGlow
import com.example.ui.theme.ArushiErrorRed
import com.example.ui.theme.ArushiMagenta
import com.example.ui.theme.ArushiPinkGlow
import com.example.ui.theme.ArushiPurplePrimary
import com.example.ui.theme.ArushiPurpleSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArushiOrbVisualizer(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Continuous rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse for Idle
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Smooth amplitude transition
    val animatedAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        animatedAmp.animateTo(
            targetValue = amplitude,
            animationSpec = tween(durationMillis = 80)
        )
    }

    val primaryColor = when (state) {
        AssistantState.IDLE -> ArushiPurplePrimary
        AssistantState.CONNECTING -> ArushiCyan
        AssistantState.LISTENING -> ArushiCyanGlow
        AssistantState.SPEAKING -> ArushiMagenta
        AssistantState.ERROR -> ArushiErrorRed
    }

    val secondaryColor = when (state) {
        AssistantState.IDLE -> ArushiPurpleSecondary
        AssistantState.CONNECTING -> ArushiPurplePrimary
        AssistantState.LISTENING -> ArushiCyan
        AssistantState.SPEAKING -> ArushiPurplePrimary
        AssistantState.ERROR -> Color(0xFFFB7185)
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 4.4f
            val ampScale = animatedAmp.value * 28f
            val pulseScale = if (state == AssistantState.IDLE) breathingPulse else 1f

            val currentRadius = (baseRadius + ampScale) * pulseScale

            // 1. Ambient outer glowing ripples
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                for (i in 1..3) {
                    val rippleRadius = currentRadius + (i * 22f * (0.6f + animatedAmp.value))
                    drawCircle(
                        color = primaryColor.copy(alpha = (0.22f / i) * (0.5f + animatedAmp.value)),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = (3.5f - i).coerceAtLeast(1f))
                    )
                }
            } else if (state == AssistantState.IDLE) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.08f),
                    radius = baseRadius * 1.35f * breathingPulse,
                    center = center
                )
            }

            // 2. Rotating orbital energy rays
            val angleRad = Math.toRadians(rotation.toDouble())
            val orbitalGrad = Brush.sweepGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.85f),
                    secondaryColor.copy(alpha = 0.95f),
                    ArushiPinkGlow.copy(alpha = 0.8f),
                    primaryColor.copy(alpha = 0.85f)
                ),
                center = center
            )

            drawCircle(
                brush = orbitalGrad,
                radius = currentRadius + 8f,
                center = center,
                style = Stroke(width = 4f + (animatedAmp.value * 6f))
            )

            // 3. Central Glowing Orb Core
            val coreGradient = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    secondaryColor.copy(alpha = 0.85f),
                    primaryColor.copy(alpha = 0.95f),
                    primaryColor.copy(alpha = 0.3f)
                ),
                center = center,
                radius = currentRadius
            )

            drawCircle(
                brush = coreGradient,
                radius = currentRadius,
                center = center
            )

            // 4. Dynamic frequency wave spikes around the circumference
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val pointCount = 36
                for (p in 0 until pointCount) {
                    val pAngle = (p.toDouble() / pointCount) * 2.0 * PI + angleRad
                    val waveMod = sin(pAngle * 4.0 + (angleRad * 2.0)) * (animatedAmp.value * 22f)
                    val pRadius = currentRadius + waveMod.toFloat()
                    val x = center.x + (pRadius * cos(pAngle)).toFloat()
                    val y = center.y + (pRadius * sin(pAngle)).toFloat()

                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = 2.5f + (animatedAmp.value * 3f),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
