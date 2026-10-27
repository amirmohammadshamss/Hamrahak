package ir.rosheno.launcher.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Decorative vectors: the adjacent Persian label supplies the accessible name. */
@Composable
fun LauncherIcon(name: String, color: Color) {
    val data = when (name) {
        "phone" -> "M6.62,10.79C8.06,13.62 10.38,15.94 13.21,17.38L15.41,15.18C15.69,14.9 16.08,14.82 16.43,14.93C17.55,15.3 18.75,15.5 20,15.5C20.55,15.5 21,15.95 21,16.5V20C21,20.55 20.55,21 20,21C10.61,21 3,13.39 3,4C3,3.45 3.45,3 4,3H7.5C8.05,3 8.5,3.45 8.5,4C8.5,5.25 8.7,6.45 9.07,7.57C9.18,7.92 9.1,8.31 8.82,8.59Z"
        "family" -> "M16,11C17.66,11 19,9.66 19,8C19,6.34 17.66,5 16,5C14.34,5 13,6.34 13,8C13,9.66 14.34,11 16,11ZM8,11C9.66,11 11,9.66 11,8C11,6.34 9.66,5 8,5C6.34,5 5,6.34 5,8C5,9.66 6.34,11 8,11ZM8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13ZM16,13C15.71,13 15.38,13.02 15.03,13.05C16.19,13.89 17,15.02 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13Z"
        "message" -> "M20,2H4C2.9,2 2,2.9 2,4V22L6,18H20C21.1,18 22,17.1 22,16V4C22,2.9 21.1,2 20,2ZM18,14H6V12H18ZM18,11H6V9H18ZM18,8H6V6H18Z"
        "photo" -> "M21,19V5C21,3.9 20.1,3 19,3H5C3.9,3 3,3.9 3,5V19C3,20.1 3.9,21 5,21H19C20.1,21 21,20.1 21,19ZM8.5,13.5L11,16.51L14.5,12L19,18H5Z"
        "camera" -> "M9,3L7.17,5H4C2.9,5 2,5.9 2,7V19C2,20.1 2.9,21 4,21H20C21.1,21 22,20.1 22,19V7C22,5.9 21.1,5 20,5H16.83L15,3ZM12,18C9.24,18 7,15.76 7,13C7,10.24 9.24,8 12,8C14.76,8 17,10.24 17,13C17,15.76 14.76,18 12,18ZM12,10C10.34,10 9,11.34 9,13C9,14.66 10.34,16 12,16C13.66,16 15,14.66 15,13C15,11.34 13.66,10 12,10Z"
        "home" -> "M10,20V14H14V20H19V12H22L12,3L2,12H5V20Z"
        "help" -> "M12,2C6.48,2 2,6.48 2,12C2,17.52 6.48,22 12,22C17.52,22 22,17.52 22,12C22,6.48 17.52,2 12,2ZM13,17H11V15H13ZM15.07,9.25L14.17,10.17C13.45,10.9 13,11.5 13,13H11V12.5C11,11.4 11.45,10.4 12.17,9.67L13.41,8.41C13.77,8.05 14,7.55 14,7C14,5.9 13.1,5 12,5C10.9,5 10,5.9 10,7H8C8,4.79 9.79,3 12,3C14.21,3 16,4.79 16,7C16,7.88 15.64,8.68 15.07,9.25Z"
        else -> "M3,3H10V10H3ZM14,3H21V10H14ZM3,14H10V21H3ZM14,14H21V21H14Z"
    }
    val path: Path = PathParser().parsePathString(data).toPath()
    Canvas(Modifier.size(32.dp)) { scale(size.width / 24, size.height / 24, pivot = androidx.compose.ui.geometry.Offset.Zero) { drawPath(path, color) } }
}
