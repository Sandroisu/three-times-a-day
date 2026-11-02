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
    val Settings = icon("Settings", "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z")
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
