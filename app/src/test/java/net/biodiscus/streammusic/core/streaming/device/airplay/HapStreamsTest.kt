package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class HapStreamsTest {

    private val key = bytes("6465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f80818283")
    private val shortMessageFrames = bytes("05007036073b1cda7df7e0e899c089a0fd52566e2a62c605000263ed59381fe6ec0849530aa700818d333a96a2ba")
    private val longMessage = bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fa000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fa000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fa000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fa000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fa000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4")
    private val longMessageFrames = bytes("00041852695477ed0d456675e8992095db6a88a079f06cbd54c9a0bfeb9b08601443b83d6bf199a8d53b6d114b6c4b764ae87a3bc2cc82291e54c0cea3b71d387e1228de27a125849505e016710e5282a79c0d844388321442d1dc2aa56200fd8c90302306fb96cddf581c69d6a7278b9040bf691b6a80728278c2769058f96e055e4be7b86945730fa3b0f263efee6e073e22653823beea6cd092461223c11e09f9acc16cd7372f88e1a3dfe069a727e8b01c8f6014e91d382f77678b50c07c718daa832eac37c9d9c38a69ef103444aa561027bc7fd6323882ed4782a3fe4e09e37ae89fd3cc9e5c3647774778deafd925b58eaa996c6c989fd46928b124c373336d62669e4a1b3f85423456036ade7662b033441c3737484a5262882d0cc87d2943045432222402dc29755ce526c30907d4c93165bf49de81db9436f31da22457e24b286d084284937e4a31931f8f5c0c3202e7daa3f0f1d1fef9978e0dc8b3c0fea9aa7d339bf5ec03e86e61d912b84d16525f2dff023ea7f06ddc8bb969cbe1d4eb261097e408b5698b9af54373af6325b7baf92455cea523c4bedc7dc20efd8ec8ed6ff1178ec76b9cd414d93635690ba70110e23cf1092d4814f1fd40e4913ba15dfba42d56f22d16c5d2fc17c8b216f35a3549aaff0d0a3517b8e0a1f406b97f4afe578296dbce384b040b44dbfa02a069ba308c6b89fd6fd50e00040bb1e17962f422fb644c4d7842bed913ff1b1940f64c604d11738ff7c5aa8e31f0488c119f9765c7f5c7a2de3ce1a6441d0ec9c7734d32273f714121a9c482afd2a14310f4ed2f693dc34068de1ef8bf23615bdfa77d5b0bc142185090ca745b8417dcf13db4dde0df055c2c0aaaf798042620e765a203da593829e58a4f4c5c0a1a3ba736a620c1e94ae6c56248993efa137391d226645ea212a866e54764793e9e9fb4ad1759053467cc8b1399c88a4175d7b0062e6c859c04a00e4606b2dc903522fe297ab5967cef06ec68eff986626ce552c99a1e69693ffd5882dd676875b8fc71b1b892b57d3a1b5e4f18ffb59e3a7b30347a196df04adb38c9e8ee0a81290184900263bdf02bab5a6e099855975a9967be07916e968d3052895fffe5854ea776aac8a2630fd6b89c9a1ce66e5ced66bbc2bc00fc5b67bb3298759b5e4e0ee501e1fc4f4080c3b7cf9d8128951731b383f446006a2868060a9ab8edaba66d06660b503de2ac7707353b846b360157922e698c3830fa33ca7703482ebaf689374dc4d6fb558897aaff989d2ef6a01d5747db410b2a53509ac658b55961429a7e7652367074d30928eb086e9805b174e27b0049234406d74d5f14b4725d604fd85f256614feeb443eaef9f9211de05026411c1d50bcee39bef20c1a2b2c244cc2e648d2b457f2b38f86918e8c5c00783078c921215d0288aa91579e410478c629a130e4a1a83d30d458b740dea7aab5dc016119892244445869652a7b08b002d095bbb81ca5d25a71a2f1ab80999a35c1c0a2a6eeb905093dce63730ee6a4cff526900dbb9936dba4f0d3bda5ba47714c38ab3ca6d831b952937fc773ba102256b2e50c43d604118159bfb77df69eae3adeb03afac2eb515b2bbd6acd3811f730d5061d92a8462377b33b122f25ccf90bfd8cd6505f190784507ebd0b222f77a3e2d869b9fcc4e558b0377652e8ee6456c0acd91b77412f41d6ff5a92a02f178f0b38174d1962ca0b3837212cdabbf021084149d831b5f178a481c24f48089f382eb8e856b76f260b76c008e65893c8156f326fe199db34c4d051c11b466ccd36e724eebc8159ef885ab17b991f23ef1463aa2efb0f4e384347a73d6e5d80c372257e6a008d9244efceb7ddd4fda66ad9224930bcedaa788a4490dba76df2b4e7dd84a6033a502336f1f8498d7fdc96559970c5378cb6715675d76b35f16ef11ce81c830553be7afae8faf14f42890f0b5467e73ada909e0815ba3adb3747c7308e694661d8af1e5237466ad4e3f2a07dcf4c42adadc147d6beb3ed5423ad6afe87cce818cc3e60b22647113f9f778ffeef069a5e8c70674188a18225405dd1cb4ab7bea897b992bc7463bafa8f1542f19ee48f0186779c9d4f4ef218b47d503e1a04e8269fc3bf9bd7cfc54f9914f2dc13a67bdcd0ceef6bec99710bcc")

    @Test
    fun `writes one frame per write like the reference implementation`() {
        val destination = ByteArrayOutputStream()
        val output = HapOutputStream(destination, key)

        output.write("hello".toByteArray())
        output.write("world".toByteArray())

        assertArrayEquals(shortMessageFrames, destination.toByteArray())
    }

    @Test
    fun `splits what is longer than 1024 bytes over frames`() {
        val destination = ByteArrayOutputStream()

        HapOutputStream(destination, key).write(longMessage)

        assertArrayEquals(longMessageFrames, destination.toByteArray())
    }

    @Test
    fun `reads the frames of the reference implementation`() {
        val input = HapInputStream(ByteArrayInputStream(shortMessageFrames), key)

        assertEquals("helloworld", String(input.readBytes()))
    }

    @Test
    fun `puts the frames of a long message back together`() {
        val input = HapInputStream(ByteArrayInputStream(longMessageFrames), key)

        assertArrayEquals(longMessage, input.readBytes())
    }

    @Test
    fun `reads one byte at a time`() {
        val input = HapInputStream(ByteArrayInputStream(shortMessageFrames), key)

        assertEquals('h'.code, input.read())
        assertEquals('e'.code, input.read())
    }

    @Test
    fun `reads end of stream when no frame follows`() {
        val input = HapInputStream(ByteArrayInputStream(ByteArray(0)), key)

        assertEquals(-1, input.read())
    }

    @Test
    fun `fails when the connection ends inside a frame`() {
        val input = HapInputStream(ByteArrayInputStream(shortMessageFrames.copyOf(10)), key)

        assertFails { input.read() }
    }

    @Test
    fun `fails on a frame that was changed`() {
        val changed = shortMessageFrames.copyOf().also { it[3] = (it[3].toInt() xor 1).toByte() }

        assertFails { HapInputStream(ByteArrayInputStream(changed), key).read() }
    }

    @Test
    fun `fails on frames read with the wrong key`() {
        val otherKey = ByteArray(32)

        assertFails { HapInputStream(ByteArrayInputStream(shortMessageFrames), otherKey).read() }
    }

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected an IOException")
        } catch (expected: IOException) {
            // The data was not accepted.
        }
    }
}
