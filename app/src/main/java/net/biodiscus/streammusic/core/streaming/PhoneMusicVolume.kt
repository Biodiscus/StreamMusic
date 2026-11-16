package net.biodiscus.streammusic.core.streaming

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal const val SILENT_VOLUME_LEVEL = 0

/** The volume of the phone's own music, which is what the speaker or headphones of the phone play. */
internal interface PhoneMusicVolume {
    var level: Int
}

internal class AudioManagerPhoneMusicVolume @Inject constructor(
    @ApplicationContext context: Context,
) : PhoneMusicVolume {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override var level: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        set(newLevel) = audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newLevel, AudioManager.FLAG_SHOW_UI)
}
