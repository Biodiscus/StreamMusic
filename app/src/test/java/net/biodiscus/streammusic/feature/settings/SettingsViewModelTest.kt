package net.biodiscus.streammusic.feature.settings

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.AacQuality
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.DeviceGroup
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.SilenceTimeout
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.ThemeMode
import net.biodiscus.streammusic.core.model.audioFormatOf
import net.biodiscus.streammusic.testing.FakeDeviceGroupRepository
import net.biodiscus.streammusic.testing.FakeProtocolChoiceRepository
import net.biodiscus.streammusic.testing.FakeSettingsRepository
import net.biodiscus.streammusic.testing.MainDispatcherRule
import net.biodiscus.streammusic.testing.aGroup
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()
    private val groupRepository = FakeDeviceGroupRepository()
    private val protocolChoiceRepository = FakeProtocolChoiceRepository()
    private val viewModel = SettingsViewModel(settingsRepository, groupRepository, protocolChoiceRepository)

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

    @Test
    fun `choosing a theme is saved`() {
        viewModel.onThemeModeChosen(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settingsRepository.currentSettings.themeMode)
    }

    @Test
    fun `switching Mute this phone off is saved`() {
        viewModel.onMutesPhoneWhileStreamingChanged(false)

        assertEquals(false, settingsRepository.currentSettings.mutesPhoneWhileStreaming)
    }

    @Test
    fun `switching Reconnect dropped devices off is saved`() {
        viewModel.onReconnectsDroppedDevicesChanged(false)

        assertEquals(false, settingsRepository.currentSettings.reconnectsDroppedDevices)
    }

    @Test
    fun `choosing an AAC quality is saved`() {
        viewModel.onAacQualityChosen(AacQuality.HIGH)

        assertEquals(AacQuality.HIGH, settingsRepository.currentSettings.aacQuality)
    }

    @Test
    fun `choosing when to stop on silence is saved`() {
        viewModel.onStopsStreamingAfterSilenceChosen(SilenceTimeout.FIFTEEN_MINUTES)

        assertEquals(SilenceTimeout.FIFTEEN_MINUTES, settingsRepository.currentSettings.stopsStreamingAfterSilence)
    }

    @Test
    fun `resetting all data puts every setting back to its default`() {
        viewModel.onThemeModeChosen(ThemeMode.DARK)
        viewModel.onProtocolFamilyToggled(ProtocolFamily.SONOS, isEnabled = false)

        viewModel.onResetAllDataConfirmed()

        assertEquals(AppSettings(), settingsRepository.currentSettings)
    }

    @Test
    fun `resetting all data deletes every group`() {
        groupRepository.saveGroup(aGroup(name = "Downstairs"))
        groupRepository.saveGroup(aGroup(name = "Garden"))

        viewModel.onResetAllDataConfirmed()

        assertEquals(emptyList<DeviceGroup>(), groupRepository.savedGroups)
    }

    @Test
    fun `resetting all data forgets the protocols picked per device`() {
        protocolChoiceRepository.save("10.0.0.2", DeviceProtocol.DLNA)

        viewModel.onResetAllDataConfirmed()

        assertEquals(emptyMap<String, DeviceProtocol>(), protocolChoiceRepository.savedChoices)
    }
}
