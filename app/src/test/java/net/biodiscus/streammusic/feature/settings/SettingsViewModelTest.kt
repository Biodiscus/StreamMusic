package net.biodiscus.streammusic.feature.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.audioFormatOf
import net.biodiscus.streammusic.testing.FakeSettingsRepository
import net.biodiscus.streammusic.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()
    private val viewModel = SettingsViewModel(settingsRepository)

    @Test
    fun `switching a protocol family off disables its protocol`() = runTest {
        viewModel.settings.test {
            awaitItem()

            viewModel.onProtocolFamilyToggled(ProtocolFamily.AIRPLAY_2, isEnabled = false)

            val disabledProtocols = awaitItem().disabledProtocols
            assertEquals(setOf(DeviceProtocol.AIRPLAY_2), disabledProtocols)
        }
    }

    @Test
    fun `switching Hide single-app capture on is saved`() {
        viewModel.onHidesSingleAppCaptureChanged(true)

        assertEquals(true, settingsRepository.currentSettings.hidesSingleAppCapture)
    }

    @Test
    fun `choosing an audio format is saved for that family`() {
        viewModel.onAudioFormatChosen(ProtocolFamily.DLNA, StreamAudioFormat.WAV)

        assertEquals(StreamAudioFormat.WAV, settingsRepository.currentSettings.audioFormatOf(ProtocolFamily.DLNA))
    }

    @Test
    fun `changing the AirPlay audio delay is saved`() {
        viewModel.onAirPlayAudioDelayChanged(1_500)

        assertEquals(1_500, settingsRepository.currentSettings.airPlayAudioDelayMillis)
    }

    @Test
    fun `switching Disconnect on mute on is saved`() {
        viewModel.onDisconnectsChromecastOnMuteChanged(true)

        assertEquals(true, settingsRepository.currentSettings.disconnectsChromecastOnMute)
    }
}
