package com.example.vpn

import com.example.data.FirewallPacketEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

data class VpnRuntimeState(
    val isActive: Boolean = false,
    val isStandbyNoInstalledTargets: Boolean = false,
    val matchedInstalledCount: Int = 0,
    val sessionStartMs: Long? = null,
    val sessionPacketsBlocked: Long = 0L,
    val sessionBytesBlocked: Long = 0L,
    val recentEvents: List<FirewallPacketEvent> = emptyList(),
    val lastError: String? = null
)

object FirewallStateHolder {
    private val eventIdGen = AtomicLong(1L)
    private val _state = MutableStateFlow(VpnRuntimeState())
    val state: StateFlow<VpnRuntimeState> = _state.asStateFlow()

    val DEFAULT_BLOCKED_PACKAGES: Set<String> = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.instagram.android",
        "com.facebook.katana"
    )

    @Volatile
    private var targetPackages: Set<String> = DEFAULT_BLOCKED_PACKAGES

    fun setTargetPackages(packages: Set<String>) {
        targetPackages = packages.toSet()
    }

    fun getTargetPackages(): Set<String> = targetPackages

    fun onVpnStarted(matchedInstalledCount: Int) {
        _state.update {
            it.copy(
                isActive = true,
                isStandbyNoInstalledTargets = matchedInstalledCount == 0,
                matchedInstalledCount = matchedInstalledCount,
                sessionStartMs = it.sessionStartMs ?: System.currentTimeMillis(),
                lastError = null
            )
        }
    }

    fun onPacketBlocked(parsed: ParsedIpv4Packet) {
        val now = System.currentTimeMillis()
        val event = FirewallPacketEvent(
            id = eventIdGen.getAndIncrement(),
            timestampMs = now,
            protocol = parsed.protocol,
            destinationIp = parsed.destinationIp,
            destinationPort = parsed.destinationPort,
            bytesDropped = parsed.totalLength
        )
        _state.update { current ->
            val updatedEvents = (listOf(event) + current.recentEvents).take(40)
            current.copy(
                sessionPacketsBlocked = current.sessionPacketsBlocked + 1L,
                sessionBytesBlocked = current.sessionBytesBlocked + parsed.totalLength.toLong(),
                recentEvents = updatedEvents
            )
        }
    }

    fun onVpnStopped(error: String? = null) {
        _state.update {
            it.copy(
                isActive = false,
                isStandbyNoInstalledTargets = false,
                matchedInstalledCount = 0,
                sessionStartMs = null,
                lastError = error
            )
        }
    }

    fun clearLogs() {
        _state.update { it.copy(recentEvents = emptyList()) }
    }
}
