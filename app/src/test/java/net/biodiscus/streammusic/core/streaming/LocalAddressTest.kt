package net.biodiscus.streammusic.core.streaming

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAddressTest {

    @Test
    fun `the local address facing the loopback address is the loopback address`() {
        assertEquals("127.0.0.1", localAddressFacing("127.0.0.1"))
    }
}
