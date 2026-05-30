package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldAccent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = EmeraldLight,
    onPrimaryContainer = Color(0xFF042100),
    secondary = EmeraldLight,
    onSecondary = LightText,
    background = SlateGrayBg,
    onBackground = LightText,
    surface = SlateGraySurface,
    onSurface = LightText,
    error = RoseError,
    onError = Color(0xFFFFFFFF),
    outline = SlateGrayBorder
  )

private val DarkColorScheme = LightColorScheme // Set light as dark fallback for a consistent Natural Tones appearance


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
