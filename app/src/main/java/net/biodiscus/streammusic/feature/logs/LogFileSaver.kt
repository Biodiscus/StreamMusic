package net.biodiscus.streammusic.feature.logs

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Returns whether the text was saved. The uri comes from the system's "save as" dialog. */
internal suspend fun saveTextToUri(contentResolver: ContentResolver, uri: Uri, text: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val output = contentResolver.openOutputStream(uri)
            if (output == null) {
                Timber.w("Could not open %s for writing", uri)
                return@withContext false
            }
            output.use { stream -> stream.write(text.toByteArray(Charsets.UTF_8)) }
            true
        } catch (error: IOException) {
            Timber.w(error, "Could not save the log to %s", uri)
            false
        }
    }
