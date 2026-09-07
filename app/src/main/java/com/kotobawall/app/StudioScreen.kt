package com.kotobawall.app

import android.graphics.Bitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val CardShape=RoundedCornerShape(24.dp)

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
 // The preview pane stays pinned above the controls so a slider drag is always visible.
 val previewPane: @Composable (Modifier)->Unit={paneModifier ->
  OutlinedCard(paneModifier.padding(horizontal=16.dp,vertical=8.dp),shape=CardShape) {
   Column(Modifier.fillMaxSize().padding(start=16.dp,end=8.dp,top=8.dp,bottom=16.dp)) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     Text("Live preview",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
     IconButton(onClick={expanded=true}) {Icon(AppIcons.OpenInFull,"Expand wallpaper preview")}
     IconButton(onClick={vm.next()},enabled=!busy) {Icon(AppIcons.NavigateNext,"Preview next word")}
    }
    Spacer(Modifier.height(4.dp))
    WallpaperPreview(bitmap,live,word,error,clockGuide,Modifier.fillMaxWidth().weight(1f).padding(end=8.dp))
   }
  }
 }
 val controls: @Composable (Modifier)->Unit={controlsModifier ->
  LazyColumn(controlsModifier,contentPadding=PaddingValues(start=16.dp,end=16.dp,top=4.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   if(!hasWords) item {
    Text("No eligible words. Download a selected JLPT level or adjust filters in Words.",color=MaterialTheme.colorScheme.error)
   }
   item {OutlinedCard(shape=CardShape) {Column(Modifier.padding(16.dp)) {
    Text("Position & style",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
     linkedMapOf("Top" to 0f,"Middle" to 0.5f,"Bottom" to 1f).forEach {(name,target) ->
      PositionChoice(name,kotlin.math.abs(position-target)<0.01f,!busy,Modifier.weight(1f)) {
       position=target;vm.edit {it.copy(position=target)}
      }
     }
    }
    LiveSlider("Text position",position,0f..1f,!busy,{position=it}) {vm.edit {it.copy(position=position)}}
    LiveSlider("Text size",scale,0.75f..1.4f,!busy,{scale=it}) {vm.edit {it.copy(scale=scale)}}
    LiveSlider("Dark panel",panel,0f..0.8f,!busy,{panel=it}) {vm.edit {it.copy(panel=panel)}}
   }}}
   item {TypographyEditor(typography,!busy,dirty,onChange={vm.editTypography(it)},onSave={vm.saveTypography(typography)})}
   item {OutlinedCard(shape=CardShape) {Column(Modifier.padding(16.dp)) {
    StudioSwitch("Show clock guide",clockGuide,true) {clockGuide=it}
    Text("The clock guide is a preview aid only and is never drawn into the wallpaper. Leave room for your phone\u2019s notifications and fingerprint sensor.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick=export,enabled=!busy && bitmap!=null && hasWords && !dirty,shape=RoundedCornerShape(20.dp),
     modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
     Icon(AppIcons.Download,null);Spacer(Modifier.width(8.dp));Text(if(dirty) "Save line layout before export" else "Export wallpaper PNG")
    }
   }}}
   item {
    Text("Position, text size and panel save when you release a slider. Save the line layout separately so automatic updates use it too.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
 Column(modifier.fillMaxSize()) {
  BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
   if(maxWidth>maxHeight) Row(Modifier.fillMaxSize()) {
    previewPane(Modifier.weight(0.44f).fillMaxHeight());controls(Modifier.weight(0.56f).fillMaxHeight())
   } else Column(Modifier.fillMaxSize()) {
    previewPane(Modifier.weight(0.52f).fillMaxWidth());controls(Modifier.weight(0.48f).fillMaxWidth())
   }
  }
  Surface(tonalElevation=2.dp) {
   Button(onClick={vm.apply(typography)},enabled=!busy && bitmap!=null && hasWords,shape=RoundedCornerShape(28.dp),
    modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp).heightIn(min=56.dp)) {
    Icon(AppIcons.Lock,null);Spacer(Modifier.width(10.dp))
    Text(if(busy) "Working\u2026" else if(dirty) "Save & apply to lock screen" else "Apply to lock screen",style=MaterialTheme.typography.titleMedium)
   }
  }
 }
 if(expanded) Dialog(onDismissRequest={expanded=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxSize()) {Column(Modifier.safeDrawingPadding().padding(16.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Text("Wallpaper preview",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
    IconButton(onClick={expanded=false}) {Icon(AppIcons.Close,"Close expanded preview")}
   }
   WallpaperPreview(bitmap,live,word,error,clockGuide,Modifier.weight(1f).fillMaxWidth())
   LiveSlider("Text position",position,0f..1f,!busy,{position=it}) {vm.edit {it.copy(position=position)}}
  }}
 }
}
@Composable
private fun PositionChoice(label: String,selected: Boolean,enabled: Boolean,modifier: Modifier,onClick: ()->Unit) {
 val shape=RoundedCornerShape(16.dp);val height=Modifier.heightIn(min=48.dp);val padding=PaddingValues(horizontal=6.dp)
 if(selected) Button(onClick=onClick,enabled=enabled,shape=shape,contentPadding=padding,modifier=modifier.then(height)) {
  Text(label,maxLines=1,style=MaterialTheme.typography.titleSmall)
 } else OutlinedButton(onClick=onClick,enabled=enabled,shape=shape,contentPadding=padding,modifier=modifier.then(height)) {
  Text(label,maxLines=1,style=MaterialTheme.typography.titleSmall)
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
   Box(Modifier.width(width).height(height).clip(RoundedCornerShape(20.dp)).background(Color.Black)) {
    Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
    Canvas(Modifier.fillMaxSize().semantics {contentDescription="Wallpaper: ${word.written}, ${word.reading}, ${Romaji.display(word)}, ${word.meaning}"}) {
     drawIntoCanvas {renderer.drawText(it.nativeCanvas,s,word,size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1))}
    }
    if(clock) Text("9:41",modifier=Modifier.align(Alignment.TopCenter).padding(top=height*0.11f),color=Color.White,fontSize=(width.value*0.19f).sp,fontWeight=FontWeight.Light)
   }
  }
 }
}
@Composable
private fun LiveSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,enabled: Boolean,onChange: (Float)->Unit,onFinish: ()->Unit) {
 Column(Modifier.padding(top=10.dp)) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Text(label,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
   Text("${(value*100).roundToInt()}%",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Slider(value=value,onValueChange=onChange,onValueChangeFinished=onFinish,valueRange=range,enabled=enabled,modifier=Modifier.semantics {contentDescription=label})
 }
}
@Composable
private fun StudioSwitch(label: String,checked: Boolean,enabled: Boolean,onChange: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().heightIn(min=52.dp),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f).padding(end=12.dp),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold);Switch(checked=checked,onCheckedChange=onChange,enabled=enabled)}
}
