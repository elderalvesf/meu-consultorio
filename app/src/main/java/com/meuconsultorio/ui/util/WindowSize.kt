package com.meuconsultorio.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowSizeType { Compact, Medium, Expanded }

@Composable
fun rememberWindowSizeType(): WindowSizeType {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> WindowSizeType.Expanded
        widthDp >= 600 -> WindowSizeType.Medium
        else -> WindowSizeType.Compact
    }
}

@Composable
fun isTablet(): Boolean {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return widthDp >= 600
}
