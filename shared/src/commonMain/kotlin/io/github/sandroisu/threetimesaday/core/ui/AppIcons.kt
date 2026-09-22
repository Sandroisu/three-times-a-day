package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal object AppIcons {
    val Back = icon("Back", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.42,-1.41L7.83,13H20z", autoMirror = true)
    val Check = icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")
    val Clock = icon("Clock", "M12,2a10,10 0,1 0,0 20a10,10 0,0 0,0 -20zM12,20a8,8 0,1 1,0 -16a8,8 0,0 1,0 16zM12.5,7H11v6l5.25,3.15 .75,-1.23 -4.5,-2.67z")
    val Medication = icon("Medication", "M4.22,11.29l7.07,-7.07a6,6 0,0 1,8.49 8.49l-7.07,7.07a6,6 0,0 1,-8.49 -8.49zM5.64,12.71a4,4 0,0 0,5.65 5.65l2.83,-2.83 -5.65,-5.65zM9.88,8.46l5.65,5.66 2.83,-2.83a4,4 0,0 0,-5.65 -5.65z")
    val Chevron = icon("Chevron", "M9.29,6.71L14.59,12l-5.3,5.29L10.71,18.71 17.41,12 10.71,5.29z", autoMirror = true)
    val ExpandMore = icon("ExpandMore", "M7.41,8.59L12,13.17l4.59,-4.58L18,10l-6,6 -6,-6z")
    val Warning = icon("Warning", "M12,2L1,21h22zM12,6l7.53,13H4.47zM11,10h2v5h-2zM11,16h2v2h-2z")

    private fun icon(
        name: String,
        path: String,
        autoMirror: Boolean = false,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black)).build()
}
