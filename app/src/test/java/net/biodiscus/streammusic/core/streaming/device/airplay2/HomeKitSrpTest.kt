package net.biodiscus.streammusic.core.streaming.device.airplay2

import net.biodiscus.streammusic.core.streaming.device.airplay.bytes
import net.biodiscus.streammusic.core.streaming.device.airplay.hex
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** The expected values come from the srptools library, which also checked the proof with its own server. */
class HomeKitSrpTest {

    private val privateKey = bytes("0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20")
    private val salt = bytes("1254ab587b061d8d")
    private val serverPublicKey = bytes("dd544725ca76dce53256aba77a57d56ac71134355b3a2515aa80358a7bd6bebfe7688ccdfdb446a01056d5b285440ad801950b9362f9cb22758993f54eec1fdecb9b8d6a49e50095885ca7fd8b49dd3951299d4f7e40d654bd387a2630597389191a7e827e08cf2664859e3848bef379a4a6aa22747abf624c5317565159634e3b4ba7d769ddc26ce5a5897afba154a01ab10da2d1c72d9a222cf1cc457236787b8e227bb68a3dde4ee18dc194ea907642d036652061314223b1f49e1c5963941a3ae99d2680429fea417873a203b80360ff6c423bdfc1e2d0573ec9a81f8cdbbc8a997833ed387001bafce19267910d37b44d3d86c336a8f30b6eca7caef14045373369b03aa1b381934b720daae73bd22425e8dad699db6ffbc8c6ccbea912b2f33bf88a97d0c0ac28113bbc6e6af13a2be90193c1edaf067e4d30e290edd0d2e50200c56034835399f9e2f03a239343338811ef37fffb5acbb844c6e6cfed871a29c0a7ae34e862ef0493ca6ffeedfd6a8346d734c709c88ba06bedb5d080")

    @Test
    fun `makes the same public key as the reference`() {
        val client = HomeKitSrpClient("3939", privateKey)

        assertEquals(
            "bc0e7cf5dc3babf67dcedbb3b140aacc6cac43f4336b43bbd5de48d6ea7c8eda66924e354255225bccad9debe21182e6bb050f3ff3e6cfbb62c229379968c70ca436ad649a0b051373184215eef046f6f1f2256838f958581f6c7b2b85fa4afe326a0e8a951d4489305331aff88a136fd8d108bcc95fceb7e557c889c828bd23fb0702f053e1ca6470fb3c76bce4843fc005c7ea675740f8550212656cfc8919d9db805a434a68229e0d9dfe43fc16dc680a5ce74b77cf374353b05759bc1da3a9dabde30a4209381c87ca83d9483abdf66b86f9b1cbda9ad82c62712b87ce6fb7069b8fc8df344261821a06d0dc5106af76d4245f3f7737a94dbc484b415555dc401842d3011204553ba9f611b02bc38de26eba1a76bf8350205a62c436ba1c3c7c69d59318bd107fd1c1f5d846b3142e85a5d49e522655e020ed1bfe1e186cf923bf328f0b9b4c6a8aa3266ed9125bb98d63827110713be7803122ee4603c54ea31863ce4b10aff31f9073cf63b94733b4f066e72d4ec35687047d5d0db160",
            hex(client.publicKey),
        )
    }

    @Test
    fun `makes the proof the reference server accepts`() {
        val answer = HomeKitSrpClient("3939", privateKey).answerChallenge(serverPublicKey, salt)

        assertEquals("0fb1a1fbe16a80be64d57c777a8182ac7c975c831825350a2de6d9497551ca8dbe88533a5e579f60c4d17943a746a06bfdf48cb16ca037b9fa22336cbc3dae38", hex(answer.proof))
    }

    @Test
    fun `ends up with the session key of the reference server`() {
        val answer = HomeKitSrpClient("3939", privateKey).answerChallenge(serverPublicKey, salt)

        assertEquals("f603e2b1498d4071308202a04f800510832bbabe98d10c07586587c3c85792849d60b63ac569af093ed56542662b6010ac76b41bb026d8f639246d9dccfb6329", hex(answer.sessionKey))
    }

    @Test
    fun `another pin gives another proof`() {
        val answer = HomeKitSrpClient("1234", privateKey).answerChallenge(serverPublicKey, salt)

        assertEquals(false, hex(answer.proof) == "0fb1a1fbe16a80be64d57c777a8182ac7c975c831825350a2de6d9497551ca8dbe88533a5e579f60c4d17943a746a06bfdf48cb16ca037b9fa22336cbc3dae38")
    }

    @Test
    fun `a new client uses a new private key`() {
        assertEquals(false, hex(HomeKitSrpClient("3939").publicKey) == hex(HomeKitSrpClient("3939").publicKey))
    }

    @Test
    fun `refuses a public key of zero`() {
        try {
            HomeKitSrpClient("3939", privateKey).answerChallenge(ByteArray(384), salt)
            fail("Expected an IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // A zero key would make the shared secret known to anyone.
        }
    }
}
