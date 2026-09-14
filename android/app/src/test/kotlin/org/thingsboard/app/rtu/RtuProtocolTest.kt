package org.thingsboard.app.rtu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RtuProtocolTest {
    @Test
    fun commandFrameRoundTrip() {
        val command = "Read RtuInf"
        val frame = RtuProtocol.buildCommand(command)

        assertEquals(0xaa.toByte(), frame.first())
        assertEquals(0xff.toByte(), frame.last())
        assertEquals(command, RtuProtocol.decodeFrame(frame)?.payload?.toString(Charsets.UTF_8))
    }

    @Test
    fun encryptedFrameRoundTrip() {
        val frame = RtuProtocol.buildCommand("Read RtuInf")
        assertTrue(frame.contentEquals(RtuProtocol.decrypt(RtuProtocol.encrypt(frame))))
        val decoder = RtuProtocol.EncryptedStreamDecoder()
        val wire = RtuProtocol.encrypt(frame)
        assertTrue(decoder.append(wire.copyOfRange(0, 7)).isEmpty())
        assertEquals(listOf("Read RtuInf"), decoder.append(wire.copyOfRange(7, wire.size)))
    }

    @Test
    fun decoderSupportsFragmentedAndStickyFrames() {
        val first = RtuProtocol.buildCommand("one")
        val second = RtuProtocol.buildCommand("two")
        val decoder = RtuProtocol.StreamDecoder()

        assertTrue(decoder.append(first.copyOfRange(0, 5)).isEmpty())
        assertEquals(listOf("one", "two"), decoder.append(first.copyOfRange(5, first.size) + second))
    }

    @Test
    fun invalidChecksumIsRejected() {
        val frame = RtuProtocol.buildCommand("Read LotInf")
        frame[5] = (frame[5].toInt() xor 1).toByte()
        assertNull(RtuProtocol.decodeFrame(frame))
    }

    @Test
    fun chunkFramesAreReassembledInOrder() {
        val decoder = RtuProtocol.StreamDecoder()
        val a = RtuProtocol.buildChunk("first", 0, 2)
        val b = RtuProtocol.buildChunk("second", 1, 2)
        assertEquals(listOf("firstsecond"), decoder.append(a + b))
    }

    @Test
    fun parsesServiceEnableBitsCorrectly() {
        val fields = MutableList(29) { "0" }
        fields[12] = "001002003004"
        fields[13] = "00123"
        fields[14] = "2"
        fields[27] = "5"
        val lot = RtuProtocol.parseLot(fields.joinToString("#"))
        @Suppress("UNCHECKED_CAST")
        val services = lot["services"] as List<Map<String, Any?>>

        assertEquals(true, services[0]["enabled"])
        assertEquals(false, services[1]["enabled"])
        assertEquals(true, services[2]["enabled"])
    }
}
