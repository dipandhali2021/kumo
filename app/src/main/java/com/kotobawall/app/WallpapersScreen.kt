package com.kotobawall.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import java.io.File

@Composable
fun WallpapersScreen(vm: WallViewModel,s: WallSettings,busy: Boolean,modifier: Modifier,pick: ()->Unit,onSelected: ()->Unit) {
 var section by rememberSaveable {mutableStateOf("Discover")}
 val saved by vm.savedWallpapers.collectAsStateWithLifecycle()
 val last by vm.lastWallpaper.collectAsStateWithLifecycle()
 val browser: WallpaperBrowserViewModel=viewModel()
 val catalog by browser.state.collectAsStateWithLifecycle()
 LaunchedEffect(browser) {browser.open()}
 var cropY by remember(s.cropY) {mutableFloatStateOf(s.cropY)}
 val context=LocalContext.current
 val uri=LocalUriHandler.current
 var deleting by remember {mutableStateOf<SavedWallpaper?>(null)}
 var explainImport by remember {mutableStateOf(false)}
 val storage=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted ->
  if(granted) vm.importCurrentWallpaper(onSelected) else vm.messages.tryEmit("Permission declined. Use Choose original photo instead.")
 }
 LazyVerticalGrid(columns=GridCells.Adaptive(156.dp),modifier=modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),
  horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item(span={GridItemSpan(maxLineSpan)}) {
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Wallpapers",style=MaterialTheme.typography.headlineSmall)
    Text("Backgrounds for your Japanese word cards.",style=MaterialTheme.typography.bodyMedium)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     listOf("Discover","Saved","My background").forEach {name ->
      FilterChip(selected=section==name,onClick={section=name},label={Text(name)})
     }
    }
   }
  }
  if(section=="Discover") {
  item(span={GridItemSpan(maxLineSpan)}) {
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    WallpaperDiscoveryControls(browser,catalog)
    if(busy) Text("Working… your saved collection stays available offline.")
   }
  }
  items(catalog.items,key={"online:"+it.id}) {photo ->
   OutlinedCard {
    WallpaperThumbnail(model=photo.thumbnail,contentDescription=photo.description.ifBlank {"Photo by ${photo.author}"},contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().aspectRatio(0.7f))
    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text(photo.author,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
     Button(onClick={vm.saveOnlineWallpaper(photo,onSelected)},enabled=!busy && saved.size<12,modifier=Modifier.fillMaxWidth()) {Text("Save & preview")}
     if(photo.provider==WallpaperProvider.PEXELS) TextButton(onClick={uri.openUri(photo.photographerUrl.ifBlank {photo.sourceUrl})}) {Text("Photographer")}
     TextButton(onClick={uri.openUri(photo.sourceUrl)}) {Text(if(photo.provider==WallpaperProvider.PEXELS) "View on Pexels" else "Photo source")}
    }
   }
  }
  if(catalog.loaded) item(span={GridItemSpan(maxLineSpan)}) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    TextButton(onClick={browser.load(catalog.page-1)},enabled=!catalog.loading && catalog.page>1) {Text("Previous")}
    Text("Page ${catalog.page}")
    TextButton(onClick={browser.load(catalog.page+1)},enabled=!catalog.loading && catalog.hasNext) {Text("Next")}
   }
  }
  }
  if(section=="Saved") {
  item(span={GridItemSpan(maxLineSpan)}) {OutlinedCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("When the screen turns off",style=MaterialTheme.typography.titleMedium)
   listOf(false to "Keep chosen wallpaper static",true to "Rotate saved wallpapers + words").forEach {(rotate,label) ->
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     RadioButton(selected=s.rotateWallpaper==rotate,onClick={vm.edit {it.copy(rotateWallpaper=rotate)}},enabled=!busy && (!rotate || saved.size>=2))
     Text(label,Modifier.weight(1f))
    }
   }
   Text("Save at least two backgrounds for rotation, then enable screen-off updates in Schedule. This setting alone does not start the service. Rotation uses saved images offline, not a download on every wake.",style=MaterialTheme.typography.bodySmall)
  }}}
  item(span={GridItemSpan(maxLineSpan)}) {OutlinedCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Last used",style=MaterialTheme.typography.titleMedium)
   val snapshot=last
   if(snapshot==null) Text("Apply a wallpaper to save its clean background here.")
   else {
    if(snapshot.photo.isNotEmpty()) WallpaperThumbnail(model=File(context.filesDir,snapshot.photo),contentDescription="Last-used background without vocabulary",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().height(140.dp))
    else Text("${snapshot.background} gradient")
    Text("Saved without vocabulary text. It survives app restarts and stays separate from the collection.",style=MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick={vm.useLastWallpaper(onSelected)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("Use last background")}
   }
  }}}
  item(span={GridItemSpan(maxLineSpan)}) {
   Text("Saved collection · ${saved.size}/12",style=MaterialTheme.typography.titleMedium)
  }
  if(saved.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text("Choose a photo or save an online image to start your collection.")}
  items(saved,key={"saved:"+it.file}) {entry ->
   OutlinedCard {
    WallpaperThumbnail(model=File(context.filesDir,entry.file),contentDescription=entry.title,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().aspectRatio(0.7f))
    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text(entry.title,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleSmall)
     if(entry.author.isNotEmpty()) Text(entry.author,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
     Button(onClick={vm.chooseWallpaper(entry.file,onSelected)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text(if(s.photo==entry.file) "Preview selected" else "Use")}
     Row {
      IconButton(onClick={deleting=entry},enabled=!busy && s.photo!=entry.file) {Icon(Icons.Outlined.Delete,"Remove saved wallpaper")}
      if(entry.sourceUrl.startsWith("https://")) IconButton(onClick={uri.openUri(entry.sourceUrl)}) {Icon(Icons.Outlined.OpenInNew,"Photo source and photographer")}
     }
    }
   }
  }
  }
  if(section=="My background") {
  item(span={GridItemSpan(maxLineSpan)}) {
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Your backgrounds",style=MaterialTheme.typography.headlineSmall)
    Text("Use your own image, import a readable lock-screen background, or choose a free online photo. Preview before applying.")
    OutlinedButton(onClick={explainImport=true},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("Import current lock-screen wallpaper")}
    Button(onClick=pick,enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("Choose original photo")}
    Text("Android 13+ blocks normal access to wallpapers set by other apps. A matching background previously applied by Kotoba Wall can be restored without reading the system image.",style=MaterialTheme.typography.bodySmall)
   }
  }
  item(span={GridItemSpan(maxLineSpan)}) {OutlinedCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Background adjustments",style=MaterialTheme.typography.titleMedium)
   Text("Photo selection, gradients and crop controls now live here. Studio focuses on your vocabulary and text layout.",style=MaterialTheme.typography.bodySmall)
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    WallpaperRenderer.palettes.keys.forEach {name ->
     FilterChip(selected=s.photo.isEmpty() && s.background==name,onClick={vm.palette(name)},enabled=!busy,label={Text(name)})
    }
   }
   if(s.photo.isNotEmpty()) {
    Text("Vertical crop · top to bottom")
    Slider(value=cropY,onValueChange={cropY=it},onValueChangeFinished={vm.edit {it.copy(cropY=cropY)}},valueRange=0f..1f,enabled=!busy)
    TextButton(onClick={cropY=0.5f;vm.edit {it.copy(cropX=0.5f,cropY=0.5f)}},enabled=!busy) {Text("Center photo")}
   }
   OutlinedButton(onClick=onSelected,enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("Open Studio preview")}
  }}}
  }
  item(span={GridItemSpan(maxLineSpan)}) {Text("Images are cropped—not stretched—to the current display proportions. Foldable screens and manufacturer wallpaper cropping need device testing. Collections are limited to 12 images; app wallpaper storage is bounded.",style=MaterialTheme.typography.bodySmall)}
 }
 if(explainImport) AlertDialog(onDismissRequest={explainImport=false},title={Text("Import the current background?")},
  text={Text(if(Build.VERSION.SDK_INT>=33) "Android does not permit ordinary access to another app’s wallpaper on this version. Kotoba Wall can reuse its own saved background when it matches the current lock screen; otherwise select the original photo. The clock is not part of the image." else "Android may ask for storage access to read your static wallpaper. No image is uploaded. If Android or a live wallpaper blocks access, choose the original image instead. The clock is not included.")},
  confirmButton={TextButton(onClick={
   explainImport=false
   if(Build.VERSION.SDK_INT<33 && ContextCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
    storage.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
   else vm.importCurrentWallpaper(onSelected)
  }) {Text("Try import")}},dismissButton={TextButton(onClick={explainImport=false;pick()}) {Text("Choose original")}})
 deleting?.let {entry -> AlertDialog(onDismissRequest={deleting=null},title={Text("Remove saved wallpaper?")},
  text={Text("This removes it from your offline rotation collection. Your original photo and the separate Last used background are kept.")},
  confirmButton={TextButton(onClick={vm.removeWallpaper(entry.file);deleting=null}) {Text("Remove")}},dismissButton={TextButton(onClick={deleting=null}) {Text("Cancel")}})}
}

@Composable
private fun WallpaperThumbnail(model: Any,contentDescription: String,contentScale: ContentScale,modifier: Modifier) {
 SubcomposeAsyncImage(model=model,contentDescription=contentDescription,contentScale=contentScale,modifier=modifier,
  loading={Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator(Modifier.size(24.dp))}},
  error={Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {Text("Preview unavailable",Modifier.padding(8.dp))}})
}
