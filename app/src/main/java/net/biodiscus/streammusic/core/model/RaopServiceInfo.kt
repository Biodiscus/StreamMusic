package net.biodiscus.streammusic.core.model

private const val NO_ENCRYPTION = 0
private const val RSA_ENCRYPTION = 1

/** What an AirPlay receiver announces about the audio it accepts. */
data class RaopServiceInfo(
    val encryptionTypes: Set<Int>,
    val needsPassword: Boolean,
    val supportsTransientPairing: Boolean = false,
) {
    /** Newer receivers only accept FairPlay or MFi encryption, which AirPlay 1 streaming here cannot do. */
    val acceptsAirPlay1Audio: Boolean =
        !needsPassword && (NO_ENCRYPTION in encryptionTypes || RSA_ENCRYPTION in encryptionTypes)

    /** Pairing with a fixed pin, without the user typing a code. Receivers that ask for a code are not supported. */
    val acceptsAirPlay2Audio: Boolean = !needsPassword && supportsTransientPairing

    val shouldEncryptAudio: Boolean = RSA_ENCRYPTION in encryptionTypes

    fun canReceiveAudioWith(protocol: DeviceProtocol): Boolean =
        when (protocol) {
            DeviceProtocol.AIRPLAY -> acceptsAirPlay1Audio
            DeviceProtocol.AIRPLAY_2 -> acceptsAirPlay2Audio
            else -> true
        }
}
