package net.biodiscus.streammusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The app's own green, the same colour as the splash screen. It does not follow the wallpaper, so the app looks the same on every phone. */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A7F46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F2C8),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF51634F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4E8D0),
    onSecondaryContainer = Color(0xFF0F1F10),
    tertiary = Color(0xFF39656B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF001F23),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFDF7),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFFBFDF7),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainer = Color(0xFFEBF0E8),
    surfaceContainerHigh = Color(0xFFE5EAE2),
    surfaceContainerHighest = Color(0xFFE0E4DC),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7CDA9A),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF0F5C30),
    onPrimaryContainer = Color(0xFFB7F2C8),
    secondary = Color(0xFFB8CCB5),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3A4B39),
    onSecondaryContainer = Color(0xFFD4E8D0),
    tertiary = Color(0xFFA2CED5),
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF1F4D54),
    onTertiaryContainer = Color(0xFFBCEBF1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111411),
    onBackground = Color(0xFFE1E3DD),
    surface = Color(0xFF111411),
    onSurface = Color(0xFFE1E3DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFF0C0F0C),
    surfaceContainerLow = Color(0xFF191C19),
    surfaceContainer = Color(0xFF1D201D),
    surfaceContainerHigh = Color(0xFF272B27),
    surfaceContainerHighest = Color(0xFF323632),
)

@Composable
fun StreamMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
