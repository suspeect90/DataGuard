package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppDataConsumption
import com.example.data.DataUsageSnapshot
import com.example.data.UsageChartMode
import com.example.data.UsagePeriod
import com.example.data.UsageTimelinePoint
import com.example.ui.theme.LocalDataGuardColors
import kotlin.math.roundToInt

@Composable
fun DataUsageChartCard(
    snapshot: DataUsageSnapshot,
    selectedPeriod: UsagePeriod,
    chartMode: UsageChartMode,
    onSelectPeriod: (UsagePeriod) -> Unit,
    onSelectChartMode: (UsageChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val pulseTransition = rememberInfiniteTransition(label = "live_dot")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    var selectedTimelineIndex by remember(snapshot.timelinePoints.size, selectedPeriod) {
        mutableStateOf<Int?>(null)
    }
    var selectedBarIndex by remember(snapshot.perAppConsumption.size, selectedPeriod) {
        mutableStateOf<Int?>(null)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, dgColors.cardBorder),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Trafic par application",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = dgColors.shieldActive.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .alpha(dotAlpha)
                                        .background(dgColors.shieldActive, CircleShape)
                                )
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dgColors.shieldActive
                                )
                            }
                        }
                    }
                    Text(
                        text = if (snapshot.totalLiveBytesPerSec > 0L) {
                            "Débit actuel : ${formatBytesFrench(snapshot.totalLiveBytesPerSec)}/s"
                        } else {
                            "Échantillonnage actif (${snapshot.perAppConsumption.size} applis)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartModeIconButton(
                        selected = chartMode == UsageChartMode.AREA_TIMELINE,
                        icon = Icons.Rounded.ShowChart,
                        contentDescription = "Graphique en courbe",
                        onClick = { onSelectChartMode(UsageChartMode.AREA_TIMELINE) },
                        modifier = Modifier.testTag("chart_mode_area_button")
                    )
                    ChartModeIconButton(
                        selected = chartMode == UsageChartMode.BAR_BY_APP,
                        icon = Icons.Rounded.BarChart,
                        contentDescription = "Graphique en barres par application",
                        onClick = { onSelectChartMode(UsageChartMode.BAR_BY_APP) },
                        modifier = Modifier.testTag("chart_mode_bar_button")
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                UsagePeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = selectedPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = UsagePeriod.entries.size
                        ),
                        modifier = Modifier.testTag("usage_period_${period.name.lowercase()}")
                    ) {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (chartMode == UsageChartMode.AREA_TIMELINE) {
                InteractiveAreaTimelineChart(
                    points = snapshot.timelinePoints,
                    selectedIndex = selectedTimelineIndex,
                    onSelectIndex = { selectedTimelineIndex = it },
                    mobileColor = dgColors.cellularWarning,
                    wifiColor = dgColors.wifiAccent,
                    blockedColor = dgColors.shieldActive
                )
            } else {
                InteractivePerAppBarChart(
                    apps = snapshot.perAppConsumption.take(6),
                    selectedIndex = selectedBarIndex,
                    onSelectIndex = { selectedBarIndex = it },
                    mobileColor = dgColors.cellularWarning,
                    wifiColor = dgColors.wifiAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(
                    color = dgColors.cellularWarning,
                    label = "4G/5G (${formatBytesFrench(snapshot.totalMobileBytes)})",
                    modifier = Modifier.weight(1f)
                )
                ChartLegendItem(
                    color = dgColors.wifiAccent,
                    label = "Wi-Fi (${formatBytesFrench(snapshot.totalWifiBytes)})",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChartModeIconButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val bg by animateColorAsState(
        targetValue = if (selected) dgColors.wifiAccent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "chart_mode_bg"
    )
    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        color = bg,
        border = BorderStroke(
            1.dp,
            if (selected) dgColors.wifiAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) dgColors.wifiAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InteractiveAreaTimelineChart(
    points: List<UsageTimelinePoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    mobileColor: Color,
    wifiColor: Color,
    blockedColor: Color
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (points.size < 2) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Collecte des mesures réseau en cours...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = labelColor
                )
            }
        }
        return
    }

    val maxByte = remember(points) {
        points.maxOf { maxOf(it.mobileBytes, it.wifiBytes, it.blockedBytes) }.coerceAtLeast(1024L)
    }

    val activeIdx = selectedIndex?.coerceIn(0, points.lastIndex) ?: points.lastIndex
    val inspectedPoint = points[activeIdx]

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, gridColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inspectedPoint.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "4G: ${formatBytesFrench(inspectedPoint.mobileBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = mobileColor
                    )
                    Text(
                        text = "Wi-Fi: ${formatBytesFrench(inspectedPoint.wifiBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = wifiColor
                    )
                    if (inspectedPoint.blockedBytes > 0L) {
                        Text(
                            text = "Bloqué: ${formatBytesFrench(inspectedPoint.blockedBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = blockedColor
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatBytesFrench(maxByte),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    text = formatBytesFrench(maxByte / 2L),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    text = "0 o",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(172.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points.size) {
                            detectTapGestures { offset ->
                                val idx = ((offset.x / size.width.coerceAtLeast(1)) * (points.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                        .pointerInput(points.size) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val idx = ((change.position.x / size.width.coerceAtLeast(1)) * (points.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                ) {
                    val padTop = 10.dp.toPx()
                    val padBottom = 10.dp.toPx()
                    val chartHeight = (size.height - padTop - padBottom).coerceAtLeast(10f)
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (step in 0..2) {
                        val y = padTop + chartHeight * (step / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    fun buildSeriesOffsets(selector: (UsageTimelinePoint) -> Long): List<Offset> {
                        return points.mapIndexed { i, pt ->
                            val ratio = (selector(pt).toFloat() / maxByte.toFloat()).coerceIn(0f, 1f)
                            val x = i * stepX
                            val y = padTop + chartHeight * (1f - ratio)
                            Offset(x, y)
                        }
                    }

                    fun drawSmoothAreaSeries(offsets: List<Offset>, strokeColor: Color) {
                        if (offsets.size < 2) return
                        val linePath = Path().apply {
                            moveTo(offsets.first().x, offsets.first().y)
                            for (i in 0 until offsets.lastIndex) {
                                val p0 = offsets[i]
                                val p1 = offsets[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(offsets.last().x, padTop + chartHeight)
                            lineTo(offsets.first().x, padTop + chartHeight)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    strokeColor.copy(alpha = 0.28f),
                                    strokeColor.copy(alpha = 0.01f)
                                ),
                                startY = padTop,
                                endY = padTop + chartHeight
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = strokeColor,
                            style = Stroke(
                                width = 2.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    val wifiOffsets = buildSeriesOffsets { it.wifiBytes }
                    val mobileOffsets = buildSeriesOffsets { it.mobileBytes }

                    drawSmoothAreaSeries(wifiOffsets, wifiColor)
                    drawSmoothAreaSeries(mobileOffsets, mobileColor)

                    val guideX = activeIdx * stepX
                    drawLine(
                        color = labelColor.copy(alpha = 0.65f),
                        start = Offset(guideX, padTop),
                        end = Offset(guideX, padTop + chartHeight),
                        strokeWidth = 1.2.dp.toPx()
                    )

                    val wifiPt = wifiOffsets[activeIdx]
                    val mobilePt = mobileOffsets[activeIdx]
                    drawCircle(color = wifiColor, radius = 5.dp.toPx(), center = wifiPt)
                    drawCircle(color = mobileColor, radius = 5.dp.toPx(), center = mobilePt)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Text(
                text = points[points.size / 2].label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Text(
                text = points.last().label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
        }
    }
}

@Composable
private fun InteractivePerAppBarChart(
    apps: List<AppDataConsumption>,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    mobileColor: Color,
    wifiColor: Color
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (apps.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Aucune application installée détectée.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = labelColor
                )
            }
        }
        return
    }

    val maxTotal = remember(apps) {
        apps.maxOf { it.totalBytes }.coerceAtLeast(1024L)
    }
    val activeIdx = selectedIndex?.coerceIn(0, apps.lastIndex) ?: 0
    val selectedApp = apps[activeIdx]

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, gridColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedApp.appName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "4G: ${formatBytesFrench(selectedApp.mobileBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = mobileColor
                    )
                    Text(
                        text = "Wi-Fi: ${formatBytesFrench(selectedApp.wifiBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = wifiColor
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp)
                .pointerInput(apps.size) {
                    detectTapGestures { offset ->
                        val slotWidth = size.width.toFloat() / apps.size.coerceAtLeast(1)
                        val idx = (offset.x / slotWidth).toInt().coerceIn(0, apps.lastIndex)
                        onSelectIndex(idx)
                    }
                }
        ) {
            val padTop = 8.dp.toPx()
            val padBottom = 8.dp.toPx()
            val chartHeight = (size.height - padTop - padBottom).coerceAtLeast(10f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (step in 0..2) {
                val y = padTop + chartHeight * (step / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            val slotWidth = size.width / apps.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.48f).coerceIn(14.dp.toPx(), 34.dp.toPx())

            apps.forEachIndexed { index, app ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                val mobileHeight = (app.mobileBytes.toFloat() / maxTotal.toFloat()) * chartHeight
                val wifiHeight = (app.wifiBytes.toFloat() / maxTotal.toFloat()) * chartHeight

                if (index == activeIdx) {
                    drawRoundRect(
                        color = gridColor.copy(alpha = 0.35f),
                        topLeft = Offset(index * slotWidth + 4.dp.toPx(), padTop),
                        size = Size(slotWidth - 8.dp.toPx(), chartHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }

                val baseY = padTop + chartHeight
                if (wifiHeight > 1f) {
                    drawRoundRect(
                        color = wifiColor,
                        topLeft = Offset(x, baseY - wifiHeight),
                        size = Size(barWidth, wifiHeight.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
                if (mobileHeight > 1f) {
                    drawRoundRect(
                        color = mobileColor,
                        topLeft = Offset(x, (baseY - wifiHeight - mobileHeight).coerceAtLeast(padTop)),
                        size = Size(barWidth, mobileHeight.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            apps.forEachIndexed { index, app ->
                Text(
                    text = app.appName.take(7),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == activeIdx) MaterialTheme.colorScheme.onSurface else labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
