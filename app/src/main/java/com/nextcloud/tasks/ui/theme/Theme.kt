package com.nextcloud.tasks.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = NextcloudBlue,
        onPrimary = Color.White,
        primaryContainer = NextcloudBlueDark,
        onPrimaryContainer = NextcloudBlueLight,
        secondary = NextcloudBlueLight,
        onSecondary = Color.White,
        secondaryContainer = NextcloudBlueDark,
        onSecondaryContainer = NextcloudBlueLight,
        tertiary = NextcloudBlueLight,
        error = NextcloudError,
        onError = Color.White,
        errorContainer = Color(0xFF5C1919),
        onErrorContainer = Color(0xFFF2B8B5),
        background = NextcloudDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = Color(0xFF2C2C2C),
        onSurfaceVariant = TextSecondaryDark,
        outline = Color(0xFF3A3A3A),
        outlineVariant = Color(0xFF2C2C2C),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = NextcloudBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD3E4F4),
        onPrimaryContainer = NextcloudBlueDark,
        secondary = NextcloudBlueDark,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD3E4F4),
        onSecondaryContainer = NextcloudBlueDark,
        tertiary = NextcloudInfo,
        error = NextcloudError,
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = NextcloudLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = Color(0xFFE7E7E7),
        onSurfaceVariant = TextSecondaryLight,
        outline = Color(0xFFCACACA),
        outlineVariant = Color(0xFFE0E0E0),
    )

@Composable
fun NextcloudTasksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val activity = LocalContext.current as? Activity
    activity?.window?.let { window ->
        val decorView = window.decorView
        WindowCompat.getInsetsController(window, decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = colorScheme.background.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = colorScheme.surface.toArgb()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
