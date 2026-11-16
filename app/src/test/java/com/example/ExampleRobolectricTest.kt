package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.vpn.PacketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DataGuard", appName)
    }

    @Test
    fun `parse valid ipv4 tcp packet extracts destination ip and port`() {
        val packet = ByteArray(40)
        packet[0] = 0x45.toByte() // IPv4, IHL=5 (20 bytes)
        packet[9] = 6.toByte()    // TCP
        // Destination IP: 104.244.42.1
        packet[16] = 104.toByte()
        packet[17] = 244.toByte()
        packet[18] = 42.toByte()
        packet[19] = 1.toByte()
        // TCP destination port: 443 (0x01BB) at offset 22..23
        packet[22] = 0x01.toByte()
        packet[23] = 0xBB.toByte()

        val parsed = PacketParser.parseIpv4(packet, packet.size)
        assertNotNull(parsed)
        assertEquals("TCP", parsed?.protocol)
        assertEquals("104.244.42.1", parsed?.destinationIp)
        assertEquals(443, parsed?.destinationPort)
        assertEquals(40, parsed?.totalLength)
    }

    @Test
    fun `parse truncated packet returns null`() {
        val shortPacket = ByteArray(12)
        assertNull(PacketParser.parseIpv4(shortPacket, shortPacket.size))
    }
}
