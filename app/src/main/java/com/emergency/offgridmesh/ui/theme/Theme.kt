package com.emergency.offgridmesh.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EmergencyDarkColorScheme = darkColorScheme(
    primary = EmergencyRed,
    onPrimary = TextPrimaryDark,
    primaryContainer = EmergencyRedDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TacticalAmber,
    onSecondary = TextPrimaryDark,
    tertiary = MeshGreen,
    onTertiary = TextPrimaryDark,
    background = MeshBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = MeshSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = MeshSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = MeshBorderDark
)

@Composable
fun OffGridMeshTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = EmergencyDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
