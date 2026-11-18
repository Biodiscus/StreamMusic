package net.biodiscus.streammusic.core.protocolchoices

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.biodiscus.streammusic.core.model.DeviceProtocol

private const val PREFERENCES_FILE_NAME = "protocol_choices"
private const val CHOICES_KEY = "choices"

@Singleton
internal class SharedPreferencesProtocolChoiceRepository @Inject constructor(
    @ApplicationContext context: Context,
) : ProtocolChoiceRepository {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val clears = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun load(): Map<String, DeviceProtocol> = decodeProtocolChoices(preferences.getString(CHOICES_KEY, null))

    override fun save(deviceId: String, protocol: DeviceProtocol) {
        val choices = load() + (deviceId to protocol)
        preferences.edit().putString(CHOICES_KEY, encodeProtocolChoices(choices)).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
        clears.tryEmit(Unit)
    }

    override fun observeClears(): Flow<Unit> = clears
}
