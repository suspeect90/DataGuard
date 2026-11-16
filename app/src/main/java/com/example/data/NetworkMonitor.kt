package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observeNetwork(): Flow<NetworkSnapshot> = callbackFlow {
        fun readSnapshot(): NetworkSnapshot {
            val activeNetwork = connectivityManager.activeNetwork
                ?: return NetworkSnapshot(NetworkTransportStatus.OFFLINE, 0)
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                ?: return NetworkSnapshot(NetworkTransportStatus.OFFLINE, 0)

            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet) {
                return NetworkSnapshot(NetworkTransportStatus.OFFLINE, 0)
            }

            val bandwidth = caps.linkDownstreamBandwidthKbps.coerceAtLeast(0)
            val status = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val isUnmetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    if (isUnmetered) NetworkTransportStatus.WIFI_FREE else NetworkTransportStatus.WIFI_METERED
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    NetworkTransportStatus.CELLULAR
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    NetworkTransportStatus.WIFI_FREE
                }
                else -> NetworkTransportStatus.OFFLINE
            }
            return NetworkSnapshot(status = status, downstreamKbps = bandwidth)
        }

        trySend(readSnapshot())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readSnapshot())
            }

            override fun onLost(network: Network) {
                trySend(readSnapshot())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(readSnapshot())
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }.distinctUntilChanged()
}
