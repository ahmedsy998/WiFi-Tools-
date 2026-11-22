package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.ChannelCongestion
import com.example.domain.ProbeSample
import com.example.domain.WifiNetwork
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonBorderBright
import com.example.ui.theme.CrimsonPanelHighest
import com.example.ui.theme.CrimsonTextMuted
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCritical
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedBright
import com.example.ui.theme.PureWhite
import com.example.utils.SignalQualityUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedRadarSweep(
    networks: List<WifiNetwork>,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isScanning) 1600 else 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f * 0.92f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Concentric RF range rings
        listOf(1f, 0.72f, 0.45f, 0.2f).forEach { factor ->
            drawCircle(
                color = CrimsonBorderBright.copy(alpha = 0.55f),
                radius = radius * factor,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }

        // Crosshairs
        drawLine(
            color = CrimsonBorder.copy(alpha = 0.7f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1f
        )
        drawLine(
            color = CrimsonBorder.copy(alpha = 0.7f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1f
        )

        // Rotating sweep wedge
        rotate(degrees = sweepAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.78f to Color.Transparent,
                    0.96f to NeonRed.copy(alpha = if (isScanning) 0.42f else 0.22f),
                    1.0f to NeonRed.copy(alpha = if (isScanning) 0.85f else 0.45f),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
            drawLine(
                color = NeonRed,
                start = center,
                end = Offset(center.x + radius, center.y),
                strokeWidth = 2.2f
            )
        }

        // Plot real discovered AP blips deterministically by BSSID hash & RSSI distance
        networks.take(18).forEach { net ->
            val normalizedDist = (1f - (net.signalPercent / 100f)).coerceIn(0.14f, 0.92f)
            val angleRad = ((net.bssid.hashCode().and(0xFFFF) % 360) * PI / 180.0)
            val px = center.x + (radius * normalizedDist * cos(angleRad)).toFloat()
            val py = center.y + (radius * normalizedDist * sin(angleRad)).toFloat()
            val blipColor = when {
                net.securityScore < 50 -> CyberCritical
                net.securityScore < 75 -> CyberAmber
                else -> CyberGreen
            }
            drawCircle(
                color = blipColor.copy(alpha = 0.25f),
                radius = 9f,
                center = Offset(px, py)
            )
            drawCircle(
                color = blipColor,
                radius = 4f,
                center = Offset(px, py)
            )
        }

        // Center transceiver core
        drawCircle(
            color = NeonRed,
            radius = 5.5f,
            center = center
        )
    }
}

@Composable
fun SignalArcGauge(
    rssiDbm: Int,
    signalPercent: Int,
    subtitle: String,
    modifier: Modifier = Modifier,
    size: Dp = 152.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (signalPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "arcProgress"
    )
    val gaugeColor = when {
        signalPercent >= 70 -> CyberGreen
        signalPercent >= 40 -> CyberAmber
        else -> NeonRed
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            val inset = strokeWidth / 2f + 4f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Track
            drawArc(
                color = CrimsonPanelHighest,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Arc
            drawArc(
                brush = Brush.horizontalGradient(listOf(NeonRed, gaugeColor)),
                startAngle = 140f,
                sweepAngle = 260f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (rssiDbm <= -100) "--" else "$rssiDbm",
                style = MonoMetricLarge,
                color = PureWhite
            )
            Text(
                text = "dBm • $signalPercent%",
                style = MaterialTheme.typography.labelSmall,
                color = gaugeColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = CrimsonTextMuted
            )
        }
    }
}

@Composable
fun AnimatedSignalBars(
    rssiDbm: Int,
    modifier: Modifier = Modifier
) {
    val activeBars = SignalQualityUtils.rssiToBars(rssiDbm)
    val activeColor = when {
        activeBars >= 3 -> CyberGreen
        activeBars == 2 -> CyberAmber
        else -> NeonRed
    }

    Canvas(modifier = modifier.size(width = 26.dp, height = 20.dp)) {
        val barCount = 4
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val barHeight = size.height * ((i + 1).toFloat() / barCount)
            val x = i * (barWidth + gap)
            val y = size.height - barHeight
            val isLit = i < activeBars
            drawRoundRect(
                color = if (isLit) activeColor else CrimsonPanelHighest,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun ChannelCongestionChart(
    channels: List<ChannelCongestion>,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        ) {
            val count = channels.size
            val horizontalPadding = 6.dp.toPx()
            val usableWidth = size.width - horizontalPadding * 2
            val slotWidth = usableWidth / count.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.62f).coerceIn(4.dp.toPx(), 18.dp.toPx())
            val chartBottom = size.height - 6.dp.toPx()
            val maxBarHeight = chartBottom - 8.dp.toPx()

            // Horizontal reference lines (25%, 50%, 75%)
            listOf(0.25f, 0.5f, 0.75f).forEach { level ->
                val y = chartBottom - maxBarHeight * level
                drawLine(
                    color = CrimsonBorder.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            channels.forEachIndexed { index, ch ->
                val centerX = horizontalPadding + slotWidth * index + slotWidth / 2f
                val fraction = (ch.congestionScore / 100f).coerceIn(0.05f, 1f)
                val barHeight = maxBarHeight * fraction
                val barColor = when {
                    ch.congestionScore >= 65 -> CyberCritical
                    ch.congestionScore >= 35 -> CyberAmber
                    ch.isNonOverlappingStandard -> CyberGreen
                    else -> NeonRedBright.copy(alpha = 0.7f)
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, chartBottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = if (channels.size > 14) 3 else 1
            channels.forEachIndexed { index, ch ->
                if (index % step == 0 || index == channels.lastIndex) {
                    Text(
                        text = ch.channel.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ch.isNonOverlappingStandard && ch.congestionScore < 35) CyberGreen else CrimsonTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun LatencySparklineChart(
    samples: List<ProbeSample>,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        val maxLatency = (samples.mapNotNull { it.latencyMs }.maxOrNull() ?: 50L).coerceAtLeast(20L).toFloat()
        val padX = 12.dp.toPx()
        val padY = 12.dp.toPx()
        val usableW = (size.width - padX * 2).coerceAtLeast(1f)
        val usableH = (size.height - padY * 2).coerceAtLeast(1f)

        // Baseline
        drawLine(
            color = CrimsonBorder,
            start = Offset(padX, size.height - padY),
            end = Offset(size.width - padX, size.height - padY),
            strokeWidth = 1f
        )

        val stepX = if (samples.size > 1) usableW / (samples.size - 1) else 0f
        val linePath = Path()
        var started = false

        samples.forEachIndexed { idx, sample ->
            val x = if (samples.size == 1) size.width / 2f else padX + idx * stepX
            if (sample.reachable && sample.latencyMs != null) {
                val ratio = (sample.latencyMs.toFloat() / maxLatency).coerceIn(0f, 1f)
                val y = (size.height - padY) - ratio * usableH
                if (!started) {
                    linePath.moveTo(x, y)
                    started = true
                } else {
                    linePath.lineTo(x, y)
                }
            }
        }

        if (started) {
            drawPath(
                path = linePath,
                color = NeonRed,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        samples.forEachIndexed { idx, sample ->
            val x = if (samples.size == 1) size.width / 2f else padX + idx * stepX
            if (sample.reachable && sample.latencyMs != null) {
                val ratio = (sample.latencyMs.toFloat() / maxLatency).coerceIn(0f, 1f)
                val y = (size.height - padY) - ratio * usableH
                drawCircle(color = CyberGreen, radius = 4.5.dp.toPx(), center = Offset(x, y))
            } else {
                // Red X marker at baseline for dropped probe
                val y = size.height - padY
                drawCircle(color = CyberCritical, radius = 4.5.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
