package africa.amoper.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AmoperBlue,
    onPrimary = AmoperWhite,
    secondary = AmoperBlueDark,
    background = AmoperWhite,
    surface = AmoperWhite,
    surfaceVariant = AmoperGrey,
    error = AmoperRed,
    onBackground = AmoperBlack,
    onSurface = AmoperBlack
)

private val DarkColors = darkColorScheme(
    primary = AmoperBlue,
    onPrimary = Color.White,
    secondary = AmoperBlueDark,
    background = AmoperBlack,
    surface = Color(0xFF17171C),
    error = AmoperRed
)

@Composable
fun AmoperTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
