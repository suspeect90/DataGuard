package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FirewallPacketEvent
import com.example.data.NetworkSnapshot
import com.example.data.NetworkTransportStatus
import com.example.ui.theme.LocalDataGuardColors
import kotlinx.coroutines.delay

@Composable
fun DataGuardBrandLogo(
    isVpnActive: Boolean,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val accentColor by animateColorAsState(
        targetValue = if (isVpnActive) dgColors.shieldActive else dgColors.wifiAccent,
        animationSpec = tween(400),
        label = "brand_accent"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val shieldPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.06f)
                    lineTo(w * 0.90f, h * 0.24f)
                    lineTo(w * 0.90f, h * 0.54f)
                    cubicTo(w * 0.90f, h * 0.78f, w * 0.72f, h * 0.92f, w * 0.50f, h * 0.97f)
                    cubicTo(w * 0.28f, h * 0.92f, w * 0.10f, h * 0.78f, w * 0.10f, h * 0.54f)
                    lineTo(w * 0.10f, h * 0.24f)
                    close()
                }
                drawPath(
                    path = shieldPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            dgColors.wifiAccent.copy(alpha = 0.12f)
                        )
                    )
                )
                drawPath(
                    path = shieldPath,
                    color = accentColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                val boltPath = Path().apply {
                    moveTo(w * 0.55f, h * 0.25f)
                    lineTo(w * 0.34f, h * 0.55f)
                    lineTo(w * 0.50f, h * 0.55f)
                    lineTo(w * 0.45f, h * 0.78f)
                    lineTo(w * 0.66f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.48f)
                    close()
                }
                drawPath(path = boltPath, color = accentColor)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("DATA")
                        }
                        withStyle(
                            SpanStyle(
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("GUARD")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.8.sp)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.40f))
                ) {
                    Text(
                        text = if (isVpnActive) "ACTIF" else "VPN",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = "Bouclier Réseau Natif",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NetworkStatusBadge(
    snapshot: NetworkSnapshot,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val (accent, icon, label) = when (snapshot.status) {
        NetworkTransportStatus.WIFI_FREE -> Triple(dgColors.wifiAccent, Icons.Rounded.Wifi, "Wi-Fi")
        NetworkTransportStatus.WIFI_METERED -> Triple(dgColors.wifiAccent, Icons.Rounded.Wifi, "Wi-Fi Lim.")
        NetworkTransportStatus.CELLULAR -> Triple(dgColors.cellularWarning, Icons.Rounded.CellTower, "4G/5G")
        NetworkTransportStatus.OFFLINE -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.CloudOff, "Hors-ligne")
    }

    val cdText = stringResource(R.string.cd_network_status)
    Surface(
        modifier = modifier.semantics { contentDescription = "$cdText : ${snapshot.status.label}" },
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FirewallRadarHero(
    isActive: Boolean,
    isStandbyNoInstalledTargets: Boolean,
    matchedInstalledCount: Int,
    totalBlockedRulesCount: Int,
    networkSnapshot: NetworkSnapshot,
    sessionStartMs: Long?,
    onToggleFirewall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val haptics = LocalHapticFeedback.current
    var elapsedSeconds by remember(isActive, sessionStartMs) { mutableLongStateOf(0L) }
    LaunchedEffect(isActive, sessionStartMs) {
        if (isActive && sessionStartMs != null) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val primaryColor by animateColorAsState(
        targetValue = if (isActive) dgColors.shieldActive else dgColors.wifiAccent,
        animationSpec = tween(400),
        label = "shield_primary"
    )
    val glowColor by animateColorAsState(
        targetValue = if (isActive) dgColors.shieldActiveGlow else dgColors.wifiAccent,
        animationSpec = tween(400),
        label = "shield_glow"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    val cdShield = stringResource(R.string.cd_shield_status)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isActive) 0.65f else 0.35f),
                    dgColors.cardBorder,
                    dgColors.wifiAccent.copy(alpha = 0.25f)
                )
            )
        ),
        tonalElevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_shield_hero_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (isActive) 0.42f else 0.24f)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(158.dp)
                        .semantics {
                            contentDescription = "$cdShield : ${if (isActive) "Actif" else "Inactif"}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.minDimension / 2f

                        drawCircle(
                            color = primaryColor.copy(alpha = if (isActive) 0.22f else 0.14f),
                            radius = maxRadius * 0.94f,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = if (isActive) 0.32f else 0.18f),
                            radius = maxRadius * 0.72f,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        if (isActive) {
                            val ringRadius = maxRadius * (0.46f + pulseProgress * 0.50f)
                            val ringAlpha = (1f - pulseProgress) * 0.52f
                            drawCircle(
                                color = glowColor.copy(alpha = ringAlpha),
                                radius = ringRadius,
                                center = center,
                                style = Stroke(width = 2.8.dp.toPx())
                            )

                            drawArc(
                                color = glowColor.copy(alpha = 0.85f),
                                startAngle = sweepAngle,
                                sweepAngle = 72f,
                                useCenter = false,
                                topLeft = Offset(center.x - maxRadius * 0.83f, center.y - maxRadius * 0.83f),
                                size = Size(maxRadius * 1.66f, maxRadius * 1.66f),
                                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape,
                        color = if (isActive) {
                            primaryColor.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
                        },
                        border = BorderStroke(
                            2.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = if (isActive) 0.95f else 0.5f),
                                    dgColors.wifiAccent.copy(alpha = 0.45f)
                                )
                            )
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = if (isActive) glowColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when {
                        isActive && isStandbyNoInstalledTargets -> "PROTECTION VPN EN VEILLE"
                        isActive -> "PROTECTION VPN ACTIVÉE"
                        else -> "PROTECTION VPN DÉSACTIVÉE"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isActive) glowColor else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        isActive && isStandbyNoInstalledTargets ->
                            "Tunnel prêt ($totalBlockedRulesCount règles). Aucune appli ciblée détectée localement."
                        isActive ->
                            "$matchedInstalledCount appli(s) filtrée(s) en tunnel local • 0 octet transmis"
                        else ->
                            "$totalBlockedRulesCount règle(s) prête(s) à bloquer les données en arrière-plan"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                ConnectionStatusPanel(
                    isVpnActive = isActive,
                    isStandby = isStandbyNoInstalledTargets,
                    networkSnapshot = networkSnapshot,
                    elapsedSeconds = elapsedSeconds,
                    modifier = Modifier.testTag("connection_status_panel")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFirewall()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("toggle_firewall_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) dgColors.blockedCrimson else dgColors.shieldActive,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isActive) {
                            "DÉSACTIVER LA PROTECTION VPN"
                        } else {
                            "ACTIVER LA PROTECTION VPN"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusPanel(
    isVpnActive: Boolean,
    isStandby: Boolean,
    networkSnapshot: NetworkSnapshot,
    elapsedSeconds: Long,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val vpnColor = when {
        isVpnActive && !isStandby -> dgColors.shieldActive
        isVpnActive -> dgColors.wifiAccent
        else -> dgColors.blockedCrimson
    }
    val networkColor = when (networkSnapshot.status) {
        NetworkTransportStatus.WIFI_FREE,
        NetworkTransportStatus.WIFI_METERED -> dgColors.wifiAccent
        NetworkTransportStatus.CELLULAR -> dgColors.cellularWarning
        NetworkTransportStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val networkIcon = when (networkSnapshot.status) {
        NetworkTransportStatus.WIFI_FREE,
        NetworkTransportStatus.WIFI_METERED -> Icons.Rounded.Wifi
        NetworkTransportStatus.CELLULAR -> Icons.Rounded.CellTower
        NetworkTransportStatus.OFFLINE -> Icons.Rounded.CloudOff
    }
    val formattedTimer = remember(elapsedSeconds) {
        val mins = elapsedSeconds / 60L
        val secs = elapsedSeconds % 60L
        String.format("%02d:%02d", mins, secs)
    }
    val speedLabel = remember(networkSnapshot) {
        if (networkSnapshot.downstreamKbps >= 1000) {
            "${networkSnapshot.downstreamKbps / 1000} Mbps"
        } else if (networkSnapshot.downstreamKbps > 0) {
            "${networkSnapshot.downstreamKbps} kbps"
        } else {
            null
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isVpnActive) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = vpnColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Statut VPN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when {
                            isVpnActive && isStandby -> "Veille • $formattedTimer"
                            isVpnActive -> "Connecté • $formattedTimer"
                            else -> "Déconnecté"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = vpnColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = networkIcon,
                    contentDescription = null,
                    tint = networkColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Connexion actuelle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (speedLabel != null) {
                            "${networkSnapshot.status.label} ($speedLabel)"
                        } else {
                            networkSnapshot.status.label
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = networkColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TrafficPulseSparklineCard(
    isVpnActive: Boolean,
    recentEvents: List<FirewallPacketEvent>,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    val bars = remember(recentEvents) {
        val sample = recentEvents.take(18).reversed()
        if (sample.isEmpty()) {
            List(18) { 0.12f }
        } else {
            val maxBytes = sample.maxOf { it.bytesDropped }.coerceAtLeast(120)
            val padded = List(18 - sample.size) { 0.10f } + sample.map {
                (it.bytesDropped.toFloat() / maxBytes.toFloat()).coerceIn(0.18f, 1f)
            }
            padded
        }
    }
    val activeColor = if (recentEvents.isNotEmpty()) dgColors.blockedCrimson else dgColors.shieldActive

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, dgColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activité de filtrage en direct",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = activeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = when {
                            recentEvents.isNotEmpty() -> "${recentEvents.size} paquets interceptés"
                            isVpnActive -> "En écoute"
                            else -> "En pause"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = activeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                val count = bars.size
                val gap = 5.dp.toPx()
                val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(3.dp.toPx())
                bars.forEachIndexed { index, ratio ->
                    val barHeight = (size.height * ratio).coerceAtLeast(4.dp.toPx())
                    val x = index * (barWidth + gap)
                    val y = size.height - barHeight
                    val barColor = if (recentEvents.isEmpty()) {
                        if (isVpnActive) dgColors.shieldActive.copy(alpha = 0.35f) else dgColors.cardBorder
                    } else {
                        if (ratio > 0.55f) dgColors.blockedCrimson else dgColors.wifiAccent.copy(alpha = 0.8f)
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val dgColors = LocalDataGuardColors.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, dgColors.cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StorageQuotaRing(
    usedMb: Int,
    quotaMb: Int,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp
) {
    val dgColors = LocalDataGuardColors.current
    val rawRatio = if (quotaMb > 0) (usedMb.toFloat() / quotaMb.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = rawRatio,
        animationSpec = spring(stiffness = 280f),
        label = "storage_arc"
    )
    val ringColor = when {
        rawRatio >= 0.85f -> dgColors.blockedCrimson
        rawRatio >= 0.60f -> dgColors.cellularWarning
        else -> dgColors.wifiAccent
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedRatio > 0.005f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedRatio,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(rawRatio * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$usedMb Mo",
                style = MaterialTheme.typography.labelSmall,
                color = ringColor
            )
        }
    }
}

@Composable
fun AppIconThumbnail(
    appName: String,
    iconDrawable: Drawable?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(iconDrawable) {
        iconDrawable?.let { drawableToBitmap(it) }
    }
    val cdAppIcon = stringResource(R.string.cd_app_icon)

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$cdAppIcon $appName",
            modifier = modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Surface(
            modifier = modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = accentColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = appName.take(2).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor
                )
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = drawable.intrinsicWidth.coerceIn(48, 128)
    val height = drawable.intrinsicHeight.coerceIn(48, 128)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun formatBytesFrench(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes o"
        bytes < 1024L * 1024L -> String.format("%.1f Ko", bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> String.format("%.2f Mo", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f Go", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
