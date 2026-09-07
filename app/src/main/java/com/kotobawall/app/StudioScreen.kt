package com.kotobawall.app

import android.graphics.Bitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private const val TAB_POSITION=0
private const val TAB_LINES=1
private const val TAB_EXPORT=2

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
 var tab by rememberSaveable {mutableIntStateOf(TAB_POSITION)}
 val live=s.copy(position=position,scale=scale,panel=panel,typography=typography)
 val library by vm.words.collectAsStateWithLifecycle()
 val word=library.getOrElse(s.wordIndex) {library.first()}
 val eligible=library.filter {WordPolicy.eligible(it,s)}
 val hasWords=eligible.isNotEmpty()
 // Arrows walk the eligible list in both directions, so the preview can be checked word by word.
 val step: (Int)->Unit={delta ->
  if(eligible.isNotEmpty()) {
   val at=eligible.indexOfFirst {it.id==word.id}.coerceAtLeast(0)
   vm.selectWord(eligible[((at+delta)%eligible.size+eligible.size)%eligible.size].id)
  }
 }
 val previewPane: @Composable (Modifier)->Unit={paneModifier ->
  Column(paneModifier.padding(start=20.dp,end=20.dp,top=4.dp,bottom=8.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text(if(tab==TAB_LINES) "Text preview" else "Live preview",style=MaterialTheme.typography.titleMedium,
     fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
    if(tab!=TAB_LINES) {
     IconButton(onClick={step(-1)},enabled=!busy) {Icon(AppIcons.NavigateBefore,"Preview previous word")}
     IconButton(onClick={step(1)},enabled=!busy) {Icon(AppIcons.NavigateNext,"Preview next word")}
    }
    IconButton(onClick={expanded=true}) {Icon(AppIcons.OpenInFull,"Expand wallpaper preview")}
   }
   Spacer(Modifier.height(4.dp))
   if(tab==TAB_LINES) LineTextPreview(bitmap,live,word,error,!busy,Modifier.fillMaxWidth().weight(1f),{step(-1)},{step(1)})
   else WallpaperPreview(bitmap,live,word,error,clockGuide,Modifier.fillMaxWidth().weight(1f))
  }
 }
 // The sheet keeps its rounded top and hairline fixed while only the tab content scrolls.
 val controls: @Composable (Modifier,Boolean)->Unit={sheetModifier,wide ->
  Surface(modifier=if(wide) sheetModifier else sheetModifier.sheetTopEdge(28.dp,Color.White.copy(alpha=0.32f)),
   tonalElevation=3.dp,
   shape=if(wide) RoundedCornerShape(topStart=28.dp,bottomStart=28.dp) else RoundedCornerShape(topStart=28.dp,topEnd=28.dp)) {
   // Portrait sizes the sheet to its own controls, so a short tab such as Export leaves no dead
   // space above the apply button and hands that height back to the preview instead.
   Column(if(wide) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
    val tabContent=if(wide) Modifier.weight(1f) else Modifier.fillMaxWidth()
    key(tab) {
     val scroll=rememberScrollState()
     val fade=(scroll.value/70f).coerceIn(0f,1f)
     Column(tabContent.topFade(fade,28.dp).verticalScroll(scroll)
      .padding(start=20.dp,end=20.dp,top=18.dp,bottom=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
      if(!hasWords) Text("No eligible words. Download a selected JLPT level or adjust filters in Words.",color=MaterialTheme.colorScheme.error)
      when(tab) {
       TAB_POSITION -> {
        LiveSlider("Text position",position,0f..1f,!busy,{position=it}) {vm.edit {it.copy(position=position)}}
        LiveSlider("Text size",scale,0.75f..1.4f,!busy,{scale=it}) {vm.edit {it.copy(scale=scale)}}
        LiveSlider("Dark panel",panel,0f..0.8f,!busy,{panel=it}) {vm.edit {it.copy(panel=panel)}}
       }
       TAB_LINES -> {
        TypographyEditor(typography,!busy,dirty,onChange={vm.editTypography(it)},onSave={vm.saveTypography(typography)})
        Text("The preview frames just your text lines. The arrows step through your words, so you can check each one before applying.",
         style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       }
       else -> {
        StudioSwitch("Show clock guide",clockGuide,true) {clockGuide=it}
        OutlinedButton(onClick=export,enabled=!busy && bitmap!=null && hasWords && !dirty,shape=RoundedCornerShape(16.dp),
         modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
         Icon(AppIcons.Download,null);Spacer(Modifier.width(8.dp));Text(if(dirty) "Save line layout before export" else "Export wallpaper PNG")
        }
        Text("The clock is a preview guide only and is never drawn into the wallpaper.",
         style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       }
      }
     }
    }
    Button(onClick={vm.apply(typography)},enabled=!busy && bitmap!=null && hasWords,shape=RoundedCornerShape(28.dp),
     modifier=Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=6.dp,bottom=14.dp).heightIn(min=56.dp)) {
     Icon(AppIcons.Lock,null);Spacer(Modifier.width(10.dp))
     Text(if(busy) "Working\u2026" else if(dirty) "Save & apply to lock screen" else "Apply to lock screen",style=MaterialTheme.typography.titleMedium)
    }
   }
  }
 }
 BoxWithConstraints(modifier.fillMaxSize()) {
  // BoxWithConstraintsScope and ColumnScope share the LayoutScopeMarker DSL marker, so the
  // available height has to be captured here to stay reachable inside the Column below.
  val screenHeight=maxHeight
  if(maxWidth>maxHeight) Row(Modifier.fillMaxSize()) {
   previewPane(Modifier.weight(0.44f).fillMaxHeight())
   Column(Modifier.weight(0.56f).fillMaxHeight()) {
    StudioTabs(tab,!busy) {tab=it}
    controls(Modifier.fillMaxWidth().weight(1f),true)
   }
  } else Column(Modifier.fillMaxSize()) {
   // The sheet is measured first and wraps its controls; the preview keeps everything left over.
   previewPane(Modifier.weight(1f).fillMaxWidth())
   StudioTabs(tab,!busy) {tab=it}
   controls(Modifier.fillMaxWidth().heightIn(max=screenHeight*0.62f),false)
  }
 }
 if(expanded) Dialog(onDismissRequest={expanded=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxSize()) {Column(Modifier.safeDrawingPadding().padding(16.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Text("Wallpaper preview",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
    IconButton(onClick={step(-1)},enabled=!busy) {Icon(AppIcons.NavigateBefore,"Preview previous word")}
    IconButton(onClick={step(1)},enabled=!busy) {Icon(AppIcons.NavigateNext,"Preview next word")}
    IconButton(onClick={expanded=false}) {Icon(AppIcons.Close,"Close expanded preview")}
   }
   WallpaperPreview(bitmap,live,word,error,clockGuide,Modifier.weight(1f).fillMaxWidth())
   LiveSlider("Text position",position,0f..1f,!busy,{position=it}) {vm.edit {it.copy(position=position)}}
  }}
 }
}
/** Traces a hairline along the two rounded top corners only, so the sheet has no side edges. */
private fun Modifier.sheetTopEdge(radius: Dp,color: Color)=drawWithContent {
 drawContent()
 val r=radius.toPx();val line=1.5.dp.toPx();val inset=line/2
 val path=Path().apply {
  moveTo(inset,r+inset)
  arcTo(Rect(inset,inset,inset+2*r,inset+2*r),180f,90f,false)
  lineTo(size.width-inset-r,inset)
  arcTo(Rect(size.width-inset-2*r,inset,size.width-inset,inset+2*r),270f,90f,false)
 }
 drawPath(path,color,style=Stroke(width=line))
}
/** Fades scrolled content out under the sheet edge; the strip grows as scrolling starts. */
private fun Modifier.topFade(amount: Float,height: Dp)=
 graphicsLayer {compositingStrategy=CompositingStrategy.Offscreen}.drawWithContent {
  drawContent()
  if(amount>0.01f) {
   val end=height.toPx()*amount
   drawRect(brush=Brush.verticalGradient(listOf(Color.Transparent,Color.Black),startY=0f,endY=end),
    size=Size(size.width,end),blendMode=BlendMode.DstIn)
  }
 }
@Composable
private fun StudioTabs(selected: Int,enabled: Boolean,onSelect: (Int)->Unit) {
 Row(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=2.dp,bottom=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  listOf("Position","Line designer","Export").forEachIndexed {index,label ->
   val shape=RoundedCornerShape(12.dp);val height=Modifier.heightIn(min=44.dp);val padding=PaddingValues(horizontal=4.dp)
   if(index==selected) Button(onClick={onSelect(index)},enabled=enabled,shape=shape,contentPadding=padding,modifier=Modifier.weight(1f).then(height)) {
    Text(label,maxLines=1,style=MaterialTheme.typography.labelLarge)
   } else OutlinedButton(onClick={onSelect(index)},enabled=enabled,shape=shape,contentPadding=padding,modifier=Modifier.weight(1f).then(height)) {
    Text(label,maxLines=1,style=MaterialTheme.typography.labelLarge)
   }
  }
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
/**
 * Line designer preview.
 *
 * Everything happens inside one canvas in pixel space: the wallpaper is measured at the canvas
 * width, the renderer reports where it will put the text with textBounds, and that block is then
 * centred and scaled into the band. Nothing is laid out oversized and offset, so there are no
 * parent constraints to coerce and no mirrored copy of the renderer's maths to drift out of sync.
 *
 * The zoom never exceeds the width of the text column, which is what keeps left, centre and right
 * aligned rows inside the frame; it is capped again by the band height so four large lines still fit.
 */
@Composable
private fun LineTextPreview(bitmap: Bitmap?,s: WallSettings,word: Word,error: String,enabled: Boolean,
 modifier: Modifier,onPrevious: ()->Unit,onNext: ()->Unit) {
 val context=LocalContext.current;val renderer=remember(context) {WallpaperRenderer(context)}
 val t=s.typography
 val empty=t.rows.take(t.lineCount).none {t.text(it,word).isNotBlank()}
 Box(modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black),contentAlignment=Alignment.Center) {
  if(error.isNotEmpty()) Text(error,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)
  else if(bitmap==null) CircularProgressIndicator()
  else {
   Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize().blur(18.dp),contentScale=ContentScale.Crop)
   Canvas(Modifier.fillMaxSize().semantics {contentDescription="Text preview: ${word.written}, ${word.reading}, ${Romaji.display(word)}, ${word.meaning}"}) {
    val w=size.width.toInt().coerceAtLeast(1)
    val h=(size.width*bitmap.height/bitmap.width).toInt().coerceAtLeast(1)
    val bounds=renderer.textBounds(s,word,w,h)
    if(bounds!=null) {
     val zoom=minOf(size.width/bounds.width.coerceAtLeast(1f),size.height*0.86f/bounds.height.coerceAtLeast(1f)).coerceIn(1f,2.4f)
     drawIntoCanvas {canvas ->
      val native=canvas.nativeCanvas
      val restore=native.save()
      native.translate(size.width/2,size.height/2)
      native.scale(zoom,zoom)
      native.translate(-(bounds.left+bounds.width/2),-(bounds.top+bounds.height/2))
      renderer.drawText(native,s,word,w,h)
      native.restoreToCount(restore)
     }
    }
   }
   if(empty) Text("Add text to a line to preview it here.",Modifier.padding(24.dp),color=Color.White)
   IconButton(onClick=onPrevious,enabled=enabled,modifier=Modifier.align(Alignment.CenterStart).padding(start=2.dp)) {
    Icon(AppIcons.NavigateBefore,"Preview previous word",tint=Color.White)
   }
   IconButton(onClick=onNext,enabled=enabled,modifier=Modifier.align(Alignment.CenterEnd).padding(end=2.dp)) {
    Icon(AppIcons.NavigateNext,"Preview next word",tint=Color.White)
   }
  }
 }
}
/** Compact slider row: the label and value share one line so three sliders leave room for the preview. */
@Composable
private fun LiveSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,enabled: Boolean,onChange: (Float)->Unit,onFinish: ()->Unit) {
 Column {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
   Text("${(value*100).roundToInt()}%",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Slider(value=value,onValueChange=onChange,onValueChangeFinished=onFinish,valueRange=range,enabled=enabled,
   modifier=Modifier.fillMaxWidth().height(36.dp).semantics {contentDescription=label})
 }
}
@Composable
private fun StudioSwitch(label: String,checked: Boolean,enabled: Boolean,onChange: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().heightIn(min=52.dp),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f).padding(end=12.dp),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold);Switch(checked=checked,onCheckedChange=onChange,enabled=enabled)}
}
