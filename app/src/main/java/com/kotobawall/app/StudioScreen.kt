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
 // Each tab needs a different amount of preview: Position keeps its controls unscrolled,
 // Export shows the wallpaper large, and Line designer only needs the zoomed text band.
 val previewShare=when(tab) {TAB_LINES->0.32f;TAB_EXPORT->0.50f;else->0.40f}
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
   if(tab==TAB_LINES) ZoomedPreview(bitmap,live,word,error,!busy,Modifier.fillMaxWidth().weight(1f),{step(-1)},{step(1)})
   else WallpaperPreview(bitmap,live,word,error,clockGuide,Modifier.fillMaxWidth().weight(1f))
  }
 }
 // The sheet keeps its rounded top and hairline fixed while only the tab content scrolls.
 val controls: @Composable (Modifier,Boolean)->Unit={sheetModifier,wide ->
  Surface(modifier=if(wide) sheetModifier else sheetModifier.sheetTopEdge(28.dp,Color.White.copy(alpha=0.32f)),
   tonalElevation=3.dp,
   shape=if(wide) RoundedCornerShape(topStart=28.dp,bottomStart=28.dp) else RoundedCornerShape(topStart=28.dp,topEnd=28.dp)) {
   Column(Modifier.fillMaxSize()) {
    Box(Modifier.weight(1f)) {
     key(tab) {
      val scroll=rememberScrollState()
      val fade=(scroll.value/70f).coerceIn(0f,1f)
      Column(Modifier.fillMaxSize().topFade(fade,28.dp).verticalScroll(scroll)
       .padding(start=20.dp,end=20.dp,top=20.dp,bottom=10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
       if(!hasWords) Text("No eligible words. Download a selected JLPT level or adjust filters in Words.",color=MaterialTheme.colorScheme.error)
       when(tab) {
        TAB_POSITION -> {
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
        }
        TAB_LINES -> {
         TypographyEditor(typography,!busy,dirty,onChange={vm.editTypography(it)},onSave={vm.saveTypography(typography)})
         Text("The arrows on the preview step through your words, so you can check how each line looks before applying.",
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
  if(maxWidth>maxHeight) Row(Modifier.fillMaxSize()) {
   previewPane(Modifier.weight(0.44f).fillMaxHeight())
   Column(Modifier.weight(0.56f).fillMaxHeight()) {
    StudioTabs(tab,!busy) {tab=it}
    controls(Modifier.fillMaxWidth().weight(1f),true)
   }
  } else Column(Modifier.fillMaxSize()) {
   previewPane(Modifier.weight(previewShare).fillMaxWidth())
   StudioTabs(tab,!busy) {tab=it}
   controls(Modifier.weight(1f-previewShare).fillMaxWidth(),false)
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
private fun PositionChoice(label: String,selected: Boolean,enabled: Boolean,modifier: Modifier,onClick: ()->Unit) {
 val shape=RoundedCornerShape(12.dp);val height=Modifier.heightIn(min=48.dp);val padding=PaddingValues(horizontal=6.dp)
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
/**
 * Height of the rendered text block, mirroring WallpaperRenderer.drawText so the zoomed
 * preview can crop around the text instead of guessing where it landed. The renderer scales
 * everything from unit = width/360, and StaticLayout lines run about 1.35x their text size.
 */
private fun textBlockHeight(s: WallSettings,word: Word,width: Dp): Dp {
 val t=s.typography
 val unit=width.value/360f
 val rows=t.rows.take(t.lineCount).filter {t.text(it,word).isNotBlank()}
 if(rows.isEmpty()) return 0.dp
 val scale=s.scale.coerceIn(0.75f,1.4f)
 val text=rows.sumOf {(it.size.coerceIn(12f,60f)*unit*scale*1.35f).toDouble()}.toFloat()
 return (text+t.spacing.coerceIn(0f,24f)*unit*(rows.size-1)+20f*unit*2f).dp
}
/**
 * Zooms into the text band for the line designer. The wallpaper is laid out larger than the
 * viewport with requiredSize, because Modifier.size would be coerced back down by the parent,
 * and then shifted so the text block sits in the middle of the band. The zoom is chosen from
 * the block height, so two lines fill the band without four lines overflowing it.
 */
@Composable
private fun ZoomedPreview(bitmap: Bitmap?,s: WallSettings,word: Word,error: String,enabled: Boolean,
 modifier: Modifier,onPrevious: ()->Unit,onNext: ()->Unit) {
 val context=LocalContext.current;val renderer=remember(context) {WallpaperRenderer(context)}
 Box(modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black),contentAlignment=Alignment.Center) {
  if(error.isNotEmpty()) Text(error,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)
  else if(bitmap==null) CircularProgressIndicator()
  else BoxWithConstraints(Modifier.fillMaxSize()) {
   val unzoomed=textBlockHeight(s,word,maxWidth)
   val zoom=if(unzoomed.value<=0f) 1.6f else (maxHeight*0.72f/unzoomed).coerceIn(1.15f,2.6f)
   val width=maxWidth*zoom
   val height=width*bitmap.height/bitmap.width
   val block=unzoomed*zoom
   val margin=(width.value*22f/360f).dp
   val top=margin+(height-block-margin*2).coerceAtLeast(0.dp)*s.position.coerceIn(0f,1f)
   val offset=(top+block/2-maxHeight/2).coerceIn(0.dp,(height-maxHeight).coerceAtLeast(0.dp))
   Box(Modifier.requiredSize(width,height).offset(x=(maxWidth-width)/2,y=-offset)) {
    Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize().blur(16.dp),contentScale=ContentScale.FillBounds)
    Canvas(Modifier.fillMaxSize().semantics {contentDescription="Text preview: ${word.written}, ${word.reading}, ${Romaji.display(word)}, ${word.meaning}"}) {
     drawIntoCanvas {renderer.drawText(it.nativeCanvas,s,word,size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1))}
    }
   }
   IconButton(onClick=onPrevious,enabled=enabled,modifier=Modifier.align(Alignment.CenterStart).padding(start=2.dp)) {
    Icon(AppIcons.NavigateBefore,"Preview previous word",tint=Color.White)
   }
   IconButton(onClick=onNext,enabled=enabled,modifier=Modifier.align(Alignment.CenterEnd).padding(end=2.dp)) {
    Icon(AppIcons.NavigateNext,"Preview next word",tint=Color.White)
   }
  }
 }
}
@Composable
private fun LiveSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,enabled: Boolean,onChange: (Float)->Unit,onFinish: ()->Unit) {
 Column {
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
