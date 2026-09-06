package com.kotobawall.app

import android.graphics.Bitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun StudioScreen(vm: WallViewModel,s: WallSettings,bitmap: Bitmap?,error: String,busy: Boolean,
 modifier: Modifier=Modifier,export: ()->Unit) {
 val configuration=LocalConfiguration.current
 LaunchedEffect(configuration.screenWidthDp,configuration.screenHeightDp) {vm.refreshPreview()}
 val draft by vm.typographyDraft.collectAsStateWithLifecycle()
 val typography=draft ?: s.typography
 val dirty=typography!=s.typography
 var position by remember(s.position) {mutableFloatStateOf(s.position)}
 var scale by remember(s.scale) {mutableFloatStateOf(s.scale)}
 var panel by remember(s.panel) {mutableFloatStateOf(s.panel)}
 var expanded by rememberSaveable {mutableStateOf(false)}
 var clockGuide by rememberSaveable {mutableStateOf(true)}
 val live=s.copy(position=position,scale=scale,panel=panel,typography=typography)
 val library by vm.words.collectAsStateWithLifecycle()
 val word=library.getOrElse(s.wordIndex) {library.first()}
 val hasWords=library.any {WordPolicy.eligible(it,s)}
 val previewPane: @Composable (Modifier)->Unit={paneModifier ->
  Column(paneModifier.padding(horizontal=16.dp,vertical=4.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text("Live preview",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
    IconButton(onClick={expanded=true}) {Icon(Icons.Outlined.OpenInFull,"Expand wallpaper preview")}
    IconButton(onClick={vm.next()},enabled=!busy) {Icon(Icons.Outlined.NavigateNext,"Preview next word")}
   }
   WallpaperPreview(bitmap,live,word,error,clockGuide,Modifier.fillMaxWidth().weight(1f))
  }
 }
 val controls: @Composable (Modifier)->Unit={controlsModifier ->
  LazyColumn(controlsModifier,contentPadding=PaddingValues(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   item {
    if(!hasWords) Text("No eligible words. Download a selected JLPT level or adjust filters in Words.",color=MaterialTheme.colorScheme.error)
    Text("Position & style",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    Text("Drag a slider—the preview stays visible.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     linkedMapOf("Top" to 0f,"Middle" to 0.5f,"Bottom" to 1f).forEach {(name,target) ->
      FilterChip(selected=kotlin.math.abs(position-target)<0.01f,onClick={position=target;vm.edit {it.copy(position=target)}},enabled=!busy,label={Text(name)})
     }
    }
    LiveSlider("Text position · top to bottom",position,0f..1f,!busy,{position=it}) {vm.edit {it.copy(position=position)}}
    LiveSlider("Text size",scale,0.75f..1.4f,!busy,{scale=it}) {vm.edit {it.copy(scale=scale)}}
    LiveSlider("Dark panel",panel,0f..0.8f,!busy,{panel=it}) {vm.edit {it.copy(panel=panel)}}
   }
   item {TypographyEditor(typography,!busy,dirty,onChange={vm.editTypography(it)},onSave={vm.saveTypography(typography)})}
   item {
    StudioSwitch("Show clock guide",clockGuide,true) {clockGuide=it}
    Text("The clock guide is not saved. Keep room for your phone’s notifications and fingerprint sensor.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   item {
    OutlinedButton(onClick=export,enabled=!busy && bitmap!=null && hasWords && !dirty,modifier=Modifier.fillMaxWidth()) {
     Icon(Icons.Outlined.Download,null);Spacer(Modifier.width(8.dp));Text(if(dirty) "Save line layout before export" else "Export wallpaper PNG")
    }
    Text("Position, overall size and panel save on release. Save the line layout separately to use it in automatic updates.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
 Column(modifier.fillMaxSize()) {
  BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
   if(maxWidth>maxHeight) {
    Row(Modifier.fillMaxSize()) {previewPane(Modifier.weight(0.44f).fillMaxHeight());VerticalDivider();controls(Modifier.weight(0.56f).fillMaxHeight())}
   } else {
    Column(Modifier.fillMaxSize()) {previewPane(Modifier.weight(0.5f).fillMaxWidth());HorizontalDivider();controls(Modifier.weight(0.5f).fillMaxWidth())}
   }
  }
  Surface(tonalElevation=2.dp) {
   Button(onClick={vm.apply(typography)},enabled=!busy && bitmap!=null && hasWords,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp).heightIn(min=48.dp)) {
    Icon(Icons.Outlined.Lock,null);Spacer(Modifier.width(8.dp));Text(if(busy) "Working…" else if(dirty) "Save & apply to lock screen" else "Apply to lock screen")
   }
  }
 }
 if(expanded) Dialog(onDismissRequest={expanded=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxSize()) {Column(Modifier.safeDrawingPadding().padding(16.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Text("Wallpaper preview",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
    IconButton(onClick={expanded=false}) {Icon(Icons.Outlined.Close,"Close expanded preview")}
   }
   WallpaperPreview(bitmap,live,word,error,clockGuide,Modifier.weight(1f).fillMaxWidth())
   LiveSlider("Text position",position,0f..1f,!busy,{position=it}) {vm.edit {it.copy(position=position)}}
  }}
 }
}
@Composable
private fun WallpaperPreview(bitmap: Bitmap?,s: WallSettings,word: Word,error: String,clock: Boolean,modifier: Modifier) {
 val context=LocalContext.current;val renderer=remember(context) {WallpaperRenderer(context)}
 BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
  if(error.isNotEmpty()) Text(error,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)
  else if(bitmap==null) CircularProgressIndicator()
  else {
   val ratio=bitmap.width.toFloat()/bitmap.height;val width=minOf(maxWidth,maxHeight*ratio);val height=width/ratio
   Box(Modifier.width(width).height(height).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
    Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
    Canvas(Modifier.fillMaxSize().semantics {contentDescription="Wallpaper: ${word.written}, ${word.reading}, ${word.meaning}"}) {
     drawIntoCanvas {renderer.drawText(it.nativeCanvas,s,word,size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1))}
    }
    if(clock) Text("9:41",modifier=Modifier.align(Alignment.TopCenter).padding(top=height*0.11f),color=Color.White,fontSize=(width.value*0.19f).sp,fontWeight=FontWeight.Light)
   }
  }
 }
}
@Composable
private fun LiveSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,enabled: Boolean,onChange: (Float)->Unit,onFinish: ()->Unit) {
 Column(Modifier.padding(top=12.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
   Text("${(value*100).roundToInt()}%",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Slider(value=value,onValueChange=onChange,onValueChangeFinished=onFinish,valueRange=range,enabled=enabled,modifier=Modifier.semantics {contentDescription=label})
 }
}
@Composable
private fun StudioSwitch(label: String,checked: Boolean,enabled: Boolean,onChange: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().heightIn(min=52.dp),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f).padding(end=12.dp));Switch(checked=checked,onCheckedChange=onChange,enabled=enabled)}
}
