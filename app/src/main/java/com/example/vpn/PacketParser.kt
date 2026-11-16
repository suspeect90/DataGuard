package com.example.vpn

data class ParsedIpv4Packet(
    val protocol: String,
    val destinationIp: String,
    val destinationPort: Int,
    val totalLength: Int
)

object PacketParser {
    fun parseIpv4(buffer: ByteArray, length: Int): ParsedIpv4Packet? {
        if (length < 20 || buffer.size < length) return null
        val version = (buffer[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null

        val ihlBytes = (buffer[0].toInt() and 0x0F) * 4
        if (ihlBytes < 20 || length < ihlBytes) return null

        val protocolCode = buffer[9].toInt() and 0xFF
        val protocolName = when (protocolCode) {
            6 -> "TCP"
            17 -> "UDP"
            1 -> "ICMP"
            else -> "IP/$protocolCode"
        }

        val destIp = buildString {
            append(buffer[16].toInt() and 0xFF)
            append('.')
            append(buffer[17].toInt() and 0xFF)
            append('.')
            append(buffer[18].toInt() and 0xFF)
            append('.')
            append(buffer[19].toInt() and 0xFF)
        }

        val destPort = if ((protocolCode == 6 || protocolCode == 17) && length >= ihlBytes + 4) {
            ((buffer[ihlBytes + 2].toInt() and 0xFF) shl 8) or (buffer[ihlBytes + 3].toInt() and 0xFF)
        } else {
            0
        }

        return ParsedIpv4Packet(
            protocol = protocolName,
            destinationIp = destIp,
            destinationPort = destPort,
            totalLength = length
        )
    }
}
