package com.kotobawall.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** A small original outline set instead of shipping the entire extended icon library. */
object AppIcons {
 private fun icon(name: String,data: String,filled: Boolean=false): ImageVector =
  ImageVector.Builder(name=name,defaultWidth=24.dp,defaultHeight=24.dp,viewportWidth=24f,viewportHeight=24f)
   .addPath(pathData=PathParser().parsePathString(data).toNodes(),
    fill=if(filled) SolidColor(Color.Black) else null,stroke=SolidColor(Color.Black),
    strokeLineWidth=1.8f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round).build()
 val AutoAwesome: ImageVector by lazy {icon("AutoAwesome","M12 3 L14.5 9.5 L21 12 L14.5 14.5 L12 21 L9.5 14.5 L3 12 L9.5 9.5 Z M20 2 V6 M18 4 H22")}
 val Info: ImageVector by lazy {icon("Info","M12 3 A9 9 0 1 1 12 21 A9 9 0 1 1 12 3 M12 11 V17 M12 7 V7.2")}
 val MenuBook: ImageVector by lazy {icon("MenuBook","M12 6 C9 3 5 3 2 4 V20 C5 19 9 19 12 22 C15 19 19 19 22 20 V4 C19 3 15 3 12 6 Z M12 6 V22 M5 8 H8 M16 8 H19 M5 12 H8 M16 12 H19")}
 val PhonelinkLock: ImageVector by lazy {icon("PhonelinkLock","M7 2 H17 V22 H7 Z M10 17 V12 H15 V17 Z M11 12 V10 A1.5 1.5 0 0 1 14 10 V12")}
 val PhotoLibrary: ImageVector by lazy {icon("PhotoLibrary","M7 3 H21 V17 H7 Z M3 7 V21 H17 M7 15 L11 10 L14 13 L17 9 L21 14 M16 6 V6.2")}
 val Schedule: ImageVector by lazy {icon("Schedule","M12 3 A9 9 0 1 1 12 21 A9 9 0 1 1 12 3 M12 7 V12 L16 14")}
 val Wallpaper: ImageVector by lazy {icon("Wallpaper","M3 8 V3 H8 M16 3 H21 V8 M21 16 V21 H16 M8 21 H3 V16 M4 17 L9 11 L13 15 L16 12 L20 17 M16 7 V7.2")}
 val Close: ImageVector by lazy {icon("Close","M6 6 L18 18 M18 6 L6 18")}
 val Download: ImageVector by lazy {icon("Download","M12 3 V16 M7 11 L12 16 L17 11 M4 17 V21 H20 V17")}
 val Lock: ImageVector by lazy {icon("Lock","M5 10 H19 V21 H5 Z M8 10 V7 A4 4 0 0 1 16 7 V10 M12 14 V17")}
 val NavigateNext: ImageVector by lazy {icon("NavigateNext","M9 5 L16 12 L9 19")}
 val OpenInFull: ImageVector by lazy {icon("OpenInFull","M4 10 V4 H10 M4 4 L10 10 M20 14 V20 H14 M20 20 L14 14")}
 val ArrowDropDown: ImageVector by lazy {icon("ArrowDropDown","M6 9 L12 15 L18 9")}
 val Search: ImageVector by lazy {icon("Search","M10.5 3 A7.5 7.5 0 1 1 10.5 18 A7.5 7.5 0 1 1 10.5 3 M16 16 L21 21")}
 val Delete: ImageVector by lazy {icon("Delete","M3 6 H21 M9 6 V3 H15 V6 M6 6 L7 21 H17 L18 6 M10 10 V17 M14 10 V17")}
 val OpenInNew: ImageVector by lazy {icon("OpenInNew","M14 3 H21 V10 M21 3 L11 13 M10 4 H4 V20 H20 V14")}
 val CheckCircle: ImageVector by lazy {icon("CheckCircle","M12 3 A9 9 0 1 1 12 21 A9 9 0 1 1 12 3 M7 12 L10 15 L17 8")}
 val Star: ImageVector by lazy {icon("Star","M12 2.5 L15 8.5 L21.7 9.5 L16.85 14.2 L18 21 L12 17.8 L6 21 L7.15 14.2 L2.3 9.5 L9 8.5 Z",true)}
 val StarBorder: ImageVector by lazy {icon("StarBorder","M12 2.5 L15 8.5 L21.7 9.5 L16.85 14.2 L18 21 L12 17.8 L6 21 L7.15 14.2 L2.3 9.5 L9 8.5 Z")}
}
