package com.kotobawall.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
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
 val key by browser.keyStatus.collectAsStateWithLifecycle()
 LaunchedEffect(browser) {browser.open()}
 val context=LocalContext.current
 val uri=LocalUriHandler.current
 var deleting by remember {mutableStateOf<SavedWallpaper?>(null)}
 var explainImport by remember {mutableStateOf(false)}
 var showFilters by remember {mutableStateOf(false)}
 // Search lives on this screen now, so it re-seeds whenever the source or the active query changes.
 var search by rememberSaveable(catalog.provider,catalog.query) {mutableStateOf(catalog.query)}
 val canSearch=key.ready && !key.saving && !catalog.loading && (catalog.provider!=WallpaperProvider.PEXELS || key.present)
 // Both Save and Preview write a file into the collection, so both stop at the cap.
 val full=saved.size>=12
 val storage=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted ->
  if(granted) vm.importCurrentWallpaper(onSelected) else vm.messages.tryEmit("Permission declined. Use Choose original instead.")
 }
 LazyVerticalGrid(columns=GridCells.Adaptive(156.dp),modifier=modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),
  horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item(span={GridItemSpan(maxLineSpan)}) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    listOf("Discover","Saved").forEach {name ->
     val shape=RoundedCornerShape(12.dp);val size=Modifier.weight(1f).heightIn(min=44.dp)
     if(section==name) Button(onClick={section=name},shape=shape,modifier=size) {Text(name,style=MaterialTheme.typography.labelLarge)}
     else OutlinedButton(onClick={section=name},shape=shape,modifier=size) {Text(name,style=MaterialTheme.typography.labelLarge)}
    }
   }
  }
  if(section=="Discover") {
   item(span={GridItemSpan(maxLineSpan)}) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      Text(catalog.provider.label,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
      // Tinted only for a non-default source, since the search box is visible on the screen now.
      val custom=catalog.provider!=WallpaperProvider.PICSUM
      Surface(shape=RoundedCornerShape(16.dp),
       color=if(custom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
       IconButton(onClick={showFilters=true},modifier=Modifier.size(52.dp)) {
        Icon(AppIcons.Filter,"Sources",
         tint=if(custom) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
       }
      }
     }
     if(catalog.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
     if(catalog.error.isNotBlank()) Text(catalog.error,color=MaterialTheme.colorScheme.error)
     if(full) Text("Collection is full. Remove one in Saved to add more.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
    }
   }
   // Only Pexels supports keyword search and shape filters, so Unsplash shows none of this.
   if(catalog.provider==WallpaperProvider.PEXELS) item(span={GridItemSpan(maxLineSpan)}) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
     OutlinedTextField(value=search,onValueChange={search=it.take(100)},singleLine=true,
      shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth(),
      placeholder={Text("Search backgrounds",maxLines=1,overflow=TextOverflow.Ellipsis)},
      leadingIcon={Icon(AppIcons.Search,null)},
      trailingIcon={if(search.isNotEmpty()) IconButton(onClick={search="";if(canSearch) browser.search("")}) {Icon(AppIcons.Close,"Clear search")}},
      keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
      keyboardActions=KeyboardActions(onSearch={if(canSearch) browser.search(search)}))
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      linkedMapOf("Featured" to "","Minimal" to "minimal abstract","Night city" to "city lights night","Space" to "stars galaxy","Architecture" to "architecture","Ocean" to "ocean coast","Textures" to "abstract texture").forEach {(label,query) ->
       FilterChip(selected=catalog.query==query,onClick={search=query;browser.search(query)},enabled=canSearch,label={Text(label)})
      }
     }
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      linkedMapOf("Portrait" to "portrait","Landscape" to "landscape","Square" to "square","Any" to "").forEach {(label,value) ->
       FilterChip(selected=catalog.orientation==value,onClick={browser.search(catalog.query,value)},
        enabled=canSearch && catalog.query.isNotBlank(),label={Text(label)})
      }
     }
    }
   }
   items(catalog.items,key={"online:"+it.id}) {photo ->
    OutlinedCard {
     WallpaperThumbnail(model=photo.thumbnail,contentDescription=photo.description.ifBlank {"Photo by ${photo.author}"},
      contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().aspectRatio(0.7f))
     Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment=Alignment.CenterVertically) {
       Text(photo.author,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
       IconButton(onClick={uri.openUri(photo.photographerUrl.ifBlank {photo.sourceUrl})},modifier=Modifier.size(32.dp)) {
        Icon(AppIcons.OpenInNew,"Photographer and source")
       }
      }
      // Save keeps you in the grid; Preview also selects it and opens Studio.
      OutlinedButton(onClick={vm.collectOnlineWallpaper(photo)},enabled=!busy && !full,modifier=Modifier.fillMaxWidth()) {Text("Save")}
      Button(onClick={vm.saveOnlineWallpaper(photo,onSelected)},enabled=!busy && !full,modifier=Modifier.fillMaxWidth()) {Text("Preview")}
     }
    }
   }
   if(catalog.loaded) item(span={GridItemSpan(maxLineSpan)}) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
     TextButton(onClick={browser.load(catalog.page-1)},enabled=!catalog.loading && catalog.page>1) {Text("Previous")}
     Text("Page ${catalog.page}",style=MaterialTheme.typography.bodyMedium)
     TextButton(onClick={browser.load(catalog.page+1)},enabled=!catalog.loading && catalog.hasNext) {Text("Next")}
    }
   }
  }
  if(section=="Saved") {
   // The only survivor of the old My background section; the dialog behind it is unchanged.
   item(span={GridItemSpan(maxLineSpan)}) {
    OutlinedButton(onClick={explainImport=true},enabled=!busy,shape=RoundedCornerShape(16.dp),
     modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text("Import current lock-screen wallpaper")}
   }
   item(span={GridItemSpan(maxLineSpan)}) {OutlinedCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text("When the screen turns off",style=MaterialTheme.typography.titleMedium)
    listOf(false to "Keep chosen wallpaper",true to "Rotate saved wallpapers").forEach {(rotate,label) ->
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      RadioButton(selected=s.rotateWallpaper==rotate,onClick={vm.edit {it.copy(rotateWallpaper=rotate)}},enabled=!busy && (!rotate || saved.size>=2))
      Text(label,Modifier.weight(1f))
     }
    }
    Text("Rotation needs two saved wallpapers, plus screen-off updates in Schedule.",
     style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }}}
   val snapshot=last
   if(snapshot!=null) item(span={GridItemSpan(maxLineSpan)}) {OutlinedCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Last used",style=MaterialTheme.typography.titleMedium)
    if(snapshot.photo.isNotEmpty()) WallpaperThumbnail(model=File(context.filesDir,snapshot.photo),
     contentDescription="Last-used background without vocabulary",contentScale=ContentScale.Crop,
     modifier=Modifier.fillMaxWidth().height(120.dp))
    else Text("${snapshot.background} gradient",style=MaterialTheme.typography.bodyMedium)
    OutlinedButton(onClick={vm.useLastWallpaper(onSelected)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("Use last background")}
   }}}
   item(span={GridItemSpan(maxLineSpan)}) {Text("Collection \u00b7 ${saved.size}/12",style=MaterialTheme.typography.titleMedium)}
   if(saved.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {
    Text("Save a photo from Discover to start your collection.",style=MaterialTheme.typography.bodyMedium)
   }
   items(saved,key={"saved:"+it.file}) {entry ->
    OutlinedCard {
     WallpaperThumbnail(model=File(context.filesDir,entry.file),contentDescription=entry.title,
      contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().aspectRatio(0.7f))
     Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Text(entry.title,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleSmall)
      if(entry.author.isNotEmpty()) Text(entry.author,maxLines=1,overflow=TextOverflow.Ellipsis,
       style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Button(onClick={vm.chooseWallpaper(entry.file,onSelected)},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
       Text(if(s.photo==entry.file) "Selected" else "Use")
      }
      Row(verticalAlignment=Alignment.CenterVertically) {
       IconButton(onClick={deleting=entry},enabled=!busy && s.photo!=entry.file) {Icon(AppIcons.Delete,"Remove saved wallpaper")}
       if(entry.sourceUrl.startsWith("https://")) IconButton(onClick={uri.openUri(entry.sourceUrl)}) {Icon(AppIcons.OpenInNew,"Photo source")}
      }
     }
    }
   }
  }
  item(span={GridItemSpan(maxLineSpan)}) {
   Text("Photos are cropped to your screen, never stretched. Saved photos work offline.",
    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
 if(showFilters) WallpaperFilterSheet(browser,catalog) {showFilters=false}
 if(explainImport) AlertDialog(onDismissRequest={explainImport=false},title={Text("Import the current background?")},
  text={Text(if(Build.VERSION.SDK_INT>=33) "Android does not permit ordinary access to another app\u2019s wallpaper on this version. Kumo can reuse its own saved background when it matches the current lock screen; otherwise select the original photo. The clock is not part of the image." else "Android may ask for storage access to read your static wallpaper. No image is uploaded. If Android or a live wallpaper blocks access, choose the original image instead. The clock is not included.")},
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
