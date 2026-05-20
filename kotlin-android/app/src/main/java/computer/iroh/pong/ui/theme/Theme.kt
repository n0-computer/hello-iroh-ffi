package computer.iroh.pong.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IrohPurple = Color(0xFF7A52E0)
private val IrohPurpleDark = Color(0xFF3A1A8E)

private val LightColors = lightColorScheme(
    primary = IrohPurple,
    onPrimary = Color.White,
    secondary = IrohPurpleDark,
    background = Color.White,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = IrohPurple,
    onPrimary = Color.White,
    secondary = IrohPurpleDark,
    background = Color(0xFF101018),
    surface = Color(0xFF18181F),
)

@Composable
fun IrohPongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
