package name.lechners.sudomnia.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import name.lechners.sudomnia.data.ThemeMode

private fun icon(name: String, build: ImageVector.Builder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(build).build()

private val ink = SolidColor(Color.Black)

private fun ImageVector.Builder.stroked(path: String) = addPath(
    pathData = addPathNodes(path),
    stroke = ink,
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
)

private fun ImageVector.Builder.filled(path: String) =
    addPath(pathData = addPathNodes(path), fill = ink)

/** A disc with rays. */
private val Sun = icon("ThemeLight") {
    stroked("M12 8a4 4 0 1 0 0 8a4 4 0 0 0 0-8z")
    stroked("M12 2.5v2.5M12 19v2.5M2.5 12h2.5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8")
}

/** A crescent. */
private val Moon = icon("ThemeDark") {
    filled("M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.4 5.4 0 0 1-4.4 2.26 5.4 5.4 0 0 1-3.14-9.8C13.92 3.04 13.46 3 13 3z")
}

/** A ring around an "A": "whichever the device says". */
private val Auto = icon("ThemeAuto") {
    stroked("M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0-18z")
    stroked("M8 16.5L12 7.5L16 16.5M9.6 13.5h4.8")
}

/** Three sliders: the aids settings. */
val AidsIcon: ImageVector = icon("Aids") {
    stroked("M4 7h4M14 7h6M4 12h8M18 12h2M4 17h2M12 17h8")
    stroked("M11 4.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 0 0 0-5z")
    stroked("M15 9.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 0 0 0-5z")
    stroked("M9 14.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 0 0 0-5z")
}

/** A curved arrow back. */
val UndoIcon: ImageVector = icon("Undo") {
    stroked("M9 14L4 9l5-5")
    stroked("M4 9h10.5a5.5 5.5 0 0 1 0 11H11")
}

/** A curved arrow forward. */
val RedoIcon: ImageVector = icon("Redo") {
    stroked("M15 14l5-5l-5-5")
    stroked("M20 9H9.5a5.5 5.5 0 0 0 0 11H13")
}

val PauseIcon: ImageVector = icon("Pause") {
    stroked("M9 5v14M15 5v14")
}

val StatsIcon: ImageVector = icon("Stats") {
    stroked("M5 20V12M12 20V5M19 20V9")
}

val NewGameIcon: ImageVector = icon("NewGame") {
    stroked("M12 5v14M5 12h14")
}

/** A backspace key. */
val EraseIcon: ImageVector = icon("Erase") {
    stroked("M21 5H9l-6 7l6 7h12z")
    stroked("M12.5 9.5l5 5M17.5 9.5l-5 5")
}

/** A lifebuoy: help when stuck. */
val HintIcon: ImageVector = icon("Hint") {
    stroked("M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0-18z")
    stroked("M12 8.5a3.5 3.5 0 1 0 0 7a3.5 3.5 0 0 0 0-7z")
    stroked("M5.6 5.6l3.9 3.9M14.5 14.5l3.9 3.9M18.4 5.6l-3.9 3.9M9.5 14.5l-3.9 3.9")
}

val GithubIcon: ImageVector = icon("Github") {
    stroked("M9 19c-4 1.5-4-2-6-2.5M15 21v-3.5c0-1 .1-1.4-.5-2c2.8-.3 5.5-1.4 5.5-6a4.6 4.6 0 0 0-1.3-3.2a4.2 4.2 0 0 0-.1-3.2s-1.1-.3-3.5 1.3a12.3 12.3 0 0 0-6.2 0C6.5 2.8 5.4 3.1 5.4 3.1a4.2 4.2 0 0 0-.1 3.2A4.6 4.6 0 0 0 4 9.5c0 4.6 2.7 5.7 5.5 6c-.6.6-.6 1.2-.5 2V21")
}

val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Auto
        ThemeMode.LIGHT -> Sun
        ThemeMode.DARK -> Moon
    }
