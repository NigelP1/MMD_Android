package uk.makemydaylondon.mmd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Built around the Make My Day sky blue (#8CD6F0) and gold (#D7A41B)

private val Light = lightColorScheme(
    primary = Color(0xFF00677F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF785A00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF4F5B92),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE1FF),
    onTertiaryContainer = Color(0xFF07164B),
    background = Color(0xFFF6FAFD),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF6FAFD),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDBE4E9),
    onSurfaceVariant = Color(0xFF3F484C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F7),
    surfaceContainer = Color(0xFFEAEEF1),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDFE3E6),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFBFC8CC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8CD6F0),
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004E60),
    onPrimaryContainer = Color(0xFFB7EAFF),
    secondary = Color(0xFFEBC248),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFB8C4FF),
    onTertiary = Color(0xFF1F2C61),
    tertiaryContainer = Color(0xFF374379),
    onTertiaryContainer = Color(0xFFDDE1FF),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1417),
    onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF3F484C),
    onSurfaceVariant = Color(0xFFBFC8CC),
    surfaceContainerLowest = Color(0xFF0A0F11),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2023),
    surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerHighest = Color(0xFF303538),
    outline = Color(0xFF899296),
    outlineVariant = Color(0xFF3F484C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun MmdTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
