package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MetricPoint
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseError
import com.example.ui.theme.VendorCisco
import com.example.ui.theme.VendorGeneric
import com.example.ui.theme.VendorLinksys
import com.example.ui.theme.VendorMikrotik
import com.example.ui.theme.VendorOpenWrt
import com.example.ui.theme.VendorRuijie
import java.util.Locale

/**
 * Format bytes/sec or kbps into friendly unit
 */
fun formatSpeed(kbps: Double): String {
    return when {
        kbps >= 1_000_000 -> String.format(Locale.US, "%.2f Gbps", kbps / 1_000_000.0)
        kbps >= 1_000 -> String.format(Locale.US, "%.2f Mbps", kbps / 1_000.0)
        else -> String.format(Locale.US, "%.0f Kbps", kbps)
    }
}

/**
 * Formats uptime seconds to "Xd Xh Xm"
 */
fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m ${secs}s"
        else -> "${minutes}m ${secs}s"
    }
}

@Composable
fun VendorBadge(vendor: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (vendor.uppercase()) {
        "MIKROTIK" -> Triple(VendorMikrotik.copy(alpha = 0.2f), VendorMikrotik, "MIKROTIK")
        "CISCO" -> Triple(VendorCisco.copy(alpha = 0.2f), VendorCisco, "CISCO")
        "RUIJIE" -> Triple(VendorRuijie.copy(alpha = 0.2f), VendorRuijie, "RUIJIE")
        "OPENWRT" -> Triple(VendorOpenWrt.copy(alpha = 0.2f), VendorOpenWrt, "OPENWRT")
        "LINKSYS" -> Triple(VendorLinksys.copy(alpha = 0.2f), VendorLinksys, "LINKSYS")
        "GENERIC" -> Triple(VendorGeneric.copy(alpha = 0.2f), VendorGeneric, "GENERIC")
        else -> Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, vendor.uppercase())
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun OnlineStatusBadge(isOnline: Boolean, isSimulated: Boolean = false) {
    val color = if (isOnline) EmeraldGreen else RoseError
    val text = if (isOnline) {
        if (isSimulated) "ONLINE (SIM)" else "ONLINE"
    } else {
        "OFFLINE"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CpuGaugeMeter(
    cpuPercent: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 110
) {
    val animatedPercent by animateFloatAsState(
        targetValue = cpuPercent.toFloat().coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 600),
        label = "cpu_anim"
    )

    val gaugeColor = when {
        cpuPercent > 80 -> RoseError
        cpuPercent > 50 -> AmberWarning
        else -> CyanNeon
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Background arc (240 degrees)
            drawArc(
                color = trackColor,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweep = (animatedPercent / 100f) * 240f
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to CyanNeon,
                    0.6f to AmberWarning,
                    1.0f to RoseError
                ),
                startAngle = 150f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${cpuPercent}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = gaugeColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "CPU LOAD",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * High-tech Dual-Curve Canvas Chart for Live Download (Cyan) & Upload (Emerald) traffic.
 */
@Composable
fun RealtimeBandwidthGraph(
    metrics: List<MetricPoint>,
    currentDownKbps: Double,
    currentUpKbps: Double,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Live rates and Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BANDWIDTH REAL-TIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Live Telemetry SNMP Interface",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Download Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(CyanNeon, CircleShape))
                        Column {
                            Text(
                                text = "RX (Down)",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatSpeed(currentDownKbps),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Upload Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(EmeraldGreen, CircleShape))
                        Column {
                            Text(
                                text = "TX (Up)",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatSpeed(currentUpKbps),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas drawing curves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 20f
                    val chartHeight = h - paddingBottom

                    // Draw 4 horizontal grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (chartHeight / gridLines) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    if (metrics.size < 2) return@Canvas

                    // Find max value for scale
                    val maxVal = metrics.maxOfOrNull { maxOf(it.downloadKbps, it.uploadKbps) }
                        ?.coerceAtLeast(5000.0) ?: 10000.0

                    val stepX = w / (metrics.size - 1)

                    // Paths for Download
                    val downPath = Path()
                    val downFill = Path()
                    downFill.moveTo(0f, chartHeight)

                    // Paths for Upload
                    val upPath = Path()
                    val upFill = Path()
                    upFill.moveTo(0f, chartHeight)

                    metrics.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val downY = chartHeight - ((pt.downloadKbps / maxVal).toFloat() * chartHeight).coerceIn(0f, chartHeight)
                        val upY = chartHeight - ((pt.uploadKbps / maxVal).toFloat() * chartHeight).coerceIn(0f, chartHeight)

                        if (i == 0) {
                            downPath.moveTo(x, downY)
                            downFill.lineTo(x, downY)

                            upPath.moveTo(x, upY)
                            upFill.lineTo(x, upY)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevDownPt = metrics[i - 1]
                            val prevDownY = chartHeight - ((prevDownPt.downloadKbps / maxVal).toFloat() * chartHeight).coerceIn(0f, chartHeight)
                            val prevUpPt = metrics[i - 1]
                            val prevUpY = chartHeight - ((prevUpPt.uploadKbps / maxVal).toFloat() * chartHeight).coerceIn(0f, chartHeight)

                            // Cubic bezier for smooth lines
                            val cx1 = prevX + (x - prevX) / 2f
                            downPath.cubicTo(cx1, prevDownY, cx1, downY, x, downY)
                            downFill.cubicTo(cx1, prevDownY, cx1, downY, x, downY)

                            upPath.cubicTo(cx1, prevUpY, cx1, upY, x, upY)
                            upFill.cubicTo(cx1, prevUpY, cx1, upY, x, upY)
                        }
                    }

                    downFill.lineTo(w, chartHeight)
                    downFill.close()

                    upFill.lineTo(w, chartHeight)
                    upFill.close()

                    // Draw Download fill & line
                    drawPath(
                        path = downFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanNeon.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )
                    drawPath(
                        path = downPath,
                        color = CyanNeon,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Upload fill & line
                    drawPath(
                        path = upFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(EmeraldGreen.copy(alpha = 0.20f), Color.Transparent),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )
                    drawPath(
                        path = upPath,
                        color = EmeraldGreen,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Bottom axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("2 menit lalu", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("1 menit lalu", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sekarang (Live)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
            }
        }
    }
}
