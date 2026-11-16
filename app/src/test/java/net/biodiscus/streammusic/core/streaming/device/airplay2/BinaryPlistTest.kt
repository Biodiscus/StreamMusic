package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import net.biodiscus.streammusic.core.streaming.device.airplay.bytes
import net.biodiscus.streammusic.core.streaming.device.airplay.hex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** The encoded values were written by Python's plistlib. */
class BinaryPlistTest {

    private val setupBodyFromPython = bytes("62706c6973743030d401020304050607085864657669636549445a74696d696e67506f72745f101469734d756c746953656c656374416972506c61795773747265616d735f101141413a42423a43433a44443a45453a464611c35009a109d80a0b0c0d0e0f101112131415161718195b617564696f466f726d61745263745373686b5373706654747970655a6c6174656e63794d61785472617465536e656711080010014f1020000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f11016010601200015888233ff800000000000013fffffffffffffffb08111a253c44585b5c5e6f7b7e82868b969b9fa2a4c7caccd1da0000000000000101000000000000001a000000000000000000000000000000e3")
    private val responseFromPython = bytes("62706c6973743030d90102030405060708090a0b0c141516171819596576656e74506f72745a74696d696e67506f72745773747265616d73546e616d6554666c616753626967546875676555656d70747959656d7074794469637411c8221000a10dd30e0f101112135b636f6e74726f6c506f72745864617461506f727454747970651117711117701060690053007000650061006b00650072002000e908120001117013000000012a05f200a0d0081b2530383d42464b515b5e606269757e8386898b9e9fa4adae0000000000000101000000000000001a000000000000000000000000000000af")

    @Test
    fun `reads a request written by the reference implementation`() {
        val plist = decodeBinaryPlist(setupBodyFromPython) as Map<*, *>

        assertEquals("AA:BB:CC:DD:EE:FF", plist["deviceID"])
        assertEquals(50000L, plist["timingPort"])
        assertEquals(true, plist["isMultiSelectAirPlay"])
    }

    @Test
    fun `reads lists, dictionaries, data, reals and negative numbers`() {
        val plist = decodeBinaryPlist(setupBodyFromPython) as Map<*, *>
        val stream = (plist["streams"] as List<*>).single() as Map<*, *>

        assertEquals(0x800L, stream["audioFormat"])
        assertEquals(1.5, stream["rate"])
        assertEquals(-5L, stream["neg"])
        assertArrayEquals(ByteArray(32) { it.toByte() }, stream["shk"] as ByteArray)
    }

    @Test
    fun `reads a response with unicode text, big numbers and empty containers`() {
        val plist = decodeBinaryPlist(responseFromPython) as Map<*, *>

        assertEquals("Speaker é", plist["name"])
        assertEquals(51234L, plist["eventPort"])
        assertEquals(70000L, plist["big"])
        assertEquals(5_000_000_000L, plist["huge"])
        assertEquals(false, plist["flag"])
        assertEquals(emptyList<Any?>(), plist["empty"])
        assertEquals(emptyMap<String, Any?>(), plist["emptyDict"])
    }

    @Test
    fun `reads what it wrote`() {
        val original = linkedMapOf(
            "text" to "hello",
            "unicode" to "héllo",
            "number" to 42,
            "big" to 5_000_000_000L,
            "negative" to -7L,
            "real" to 0.25,
            "yes" to true,
            "no" to false,
            "data" to byteArrayOf(1, 2, 3),
            "list" to listOf("a", 1, listOf(2)),
            "nested" to linkedMapOf("key" to "value"),
        )

        val read = decodeBinaryPlist(encodeBinaryPlist(original)) as Map<*, *>

        assertEquals("hello", read["text"])
        assertEquals("héllo", read["unicode"])
        assertEquals(42L, read["number"])
        assertEquals(5_000_000_000L, read["big"])
        assertEquals(-7L, read["negative"])
        assertEquals(0.25, read["real"])
        assertEquals(true, read["yes"])
        assertEquals(false, read["no"])
        assertArrayEquals(byteArrayOf(1, 2, 3), read["data"] as ByteArray)
        assertEquals(listOf("a", 1L, listOf(2L)), read["list"])
        assertEquals(mapOf("key" to "value"), read["nested"])
    }

    @Test
    fun `writes a small dictionary exactly like the reference implementation`() {
        val written = encodeBinaryPlist(linkedMapOf("a" to 1))

        assertEquals(
            "62706c6973743030d1010251611001080b0d000000000000010100000000000000030000000000000000000000000000000f",
            hex(written),
        )
    }

    @Test
    fun `writes strings and data longer than 14 bytes`() {
        val long = "x".repeat(40)

        val read = decodeBinaryPlist(encodeBinaryPlist(listOf(long, ByteArray(300)))) as List<*>

        assertEquals(long, read[0])
        assertEquals(300, (read[1] as ByteArray).size)
    }

    @Test
    fun `refuses data that is not a property list`() {
        try {
            decodeBinaryPlist("not a plist at all, definitely not".toByteArray())
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertEquals("Not a binary property list", expected.message)
        }
    }
}
