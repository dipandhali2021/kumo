package com.kotobawall.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class WallViewModel(app: Application): AndroidViewModel(app) {
 private val repo=(app as KotobaApplication).repository
 val words=repo.words
 val savedWallpapers=repo.savedWallpapers
 val lastWallpaper=repo.lastWallpaper
 val settings=repo.settings
 val cycle=ScreenCycleController.state
 private val draft=MutableStateFlow<Typography?>(null)
 val typographyDraft=draft.asStateFlow()
 fun editTypography(t: Typography) {if(!_busy.value) draft.value=t}
 private val _preview=MutableStateFlow<Bitmap?>(null)
 val preview=_preview.asStateFlow()
 private val _previewError=MutableStateFlow("")
 val previewError=_previewError.asStateFlow()
 private val _busy=MutableStateFlow(false)
 val busy=_busy.asStateFlow()
 val messages=MutableSharedFlow<String>(extraBufferCapacity=8)
 private val previewRefresh=MutableStateFlow(0)
 fun refreshPreview() {previewRefresh.update {it+1}}
 init {
  ScreenCycleController.initialize(app)
  viewModelScope.launch {
   combine(settings.map {WallSettings(background=it.background,photo=it.photo,cropX=it.cropX,cropY=it.cropY)}.distinctUntilChanged(),previewRefresh) {settings,_->settings}.collectLatest {s ->
    delay(180)
    try {_previewError.value="";_preview.value=repo.preview(s)}
    catch(e: CancellationException) {throw e}
    catch(e: Exception) {_preview.value=null;_previewError.value=e.message ?: "Preview failed";messages.emit(_previewError.value)}
   }
  }
  RotationSchedule.set(app,settings.value.hours)
 }
 private fun operation(success: String?=null,block: suspend ()->Unit) {
  if(_busy.value) return
  _busy.value=true
  viewModelScope.launch {
   try {block();if(success!=null) messages.emit(success)}
   catch(e: CancellationException) {throw e}
   catch(e: Exception) {messages.emit(e.message ?: "Something went wrong. Please try again.")}
   finally {_busy.value=false}
  }
 }
 fun edit(change: (WallSettings)->WallSettings) {
  if(_busy.value) return
  viewModelScope.launch {
   try {repo.edit(change)}
   catch(e: CancellationException) {throw e}
   catch(e: Exception) {messages.emit(e.message ?: "Could not save settings")}
  }
 }
 private val _catalog=MutableStateFlow(CatalogState())
 val catalog=_catalog.asStateFlow()
 fun browseWallpapers(page: Int=1) {
  if(_catalog.value.loading) return
  _catalog.value=_catalog.value.copy(loading=true,error="")
  viewModelScope.launch {
   try {
    val items=WallpaperCatalog.list(page)
    if(items.isEmpty()) _catalog.value=_catalog.value.copy(loading=false,error="No more photos on this page.")
    else _catalog.value=CatalogState(page=page,items=items)
   } catch(e: CancellationException) {throw e}
   catch(e: Exception) {_catalog.value=_catalog.value.copy(loading=false,error=e.message ?: "Could not load photos. Try again.")}
  }
 }
 fun saveOnlineWallpaper(item: OnlineWallpaper,onSelected: ()->Unit)=operation("Wallpaper saved. Preview it before applying.") {
  val path=repo.saveOnlineWallpaper(item);repo.chooseWallpaper(path);onSelected()
 }
 fun chooseWallpaper(path: String,onSelected: ()->Unit)=operation {repo.chooseWallpaper(path);onSelected()}
 fun useLastWallpaper(onSelected: ()->Unit)=operation("Last-used clean background restored") {repo.useLastWallpaper();onSelected()}
 fun importCurrentWallpaper(onSelected: ()->Unit)=operation("Current wallpaper imported for preview") {repo.importCurrentWallpaper();onSelected()}
 fun removeWallpaper(path: String)=operation("Wallpaper removed from collection") {repo.removeWallpaper(path)}
 fun pick(uri: Uri)=operation("Background saved on this device") {repo.importPhoto(uri)}
 fun palette(name: String)=operation {repo.usePalette(name)}
 fun next()=operation {repo.nextWord()}
 fun selectWord(id: String)=operation {repo.selectWord(id)}
 fun downloadedAt(level: Int)=repo.downloadedAt(level)
 private val _download=MutableStateFlow(DownloadState())
 val download=_download.asStateFlow()
 fun downloadLevels() {
  if(_download.value.running) return
  val levels=settings.value.levels.sortedDescending()
  if(levels.isEmpty()) {messages.tryEmit("Select at least one JLPT level to download.");return}
  _download.value=DownloadState(true,"Starting download…")
  viewModelScope.launch {
   val errors=mutableListOf<String>();var total=0
   try {
    for(level in levels) {
     _download.value=DownloadState(true,"Downloading N$level…")
     try {total+=repo.downloadLevel(level)}
     catch(e: CancellationException) {throw e}
     catch(e: Exception) {errors+="N$level: ${e.message ?: "Download failed"}"}
    }
    _download.value=DownloadState(false,if(errors.isEmpty()) "$total words saved for offline use." else "$total words saved. Existing cache kept for failed levels.",errors.joinToString("\n"))
   } finally {if(_download.value.running) _download.value=_download.value.copy(running=false)}
  }
 }
 fun saveTypography(t: Typography)=operation("Line layout saved") {repo.edit {it.copy(typography=t)};draft.value=null}
 fun apply(t: Typography?=null)=operation("Lock-screen wallpaper updated") {
  if(t!=null) {repo.edit {it.copy(typography=t)};draft.value=null}
  repo.apply()
 }
 fun export(uri: Uri)=operation("Wallpaper PNG saved") {repo.export(uri)}
 fun startCycle()=operation("Screen-off updates started. Turn your screen off, wait a moment, then wake it.") {
  repo.edit {it.copy(hours=0,lastError="")}
  RotationSchedule.set(getApplication(),0);ScreenCycleController.start(getApplication())
 }
 fun stopCycle() {ScreenCycleController.stop(getApplication())}
 fun schedule(hours: Int)=operation(if(hours==0) "Automatic updates paused" else "Automatic updates enabled") {
  ScreenCycleController.stop(getApplication());repo.edit {it.copy(hours=hours,lastError="")};RotationSchedule.set(getApplication(),hours)
 }
}
data class DownloadState(val running: Boolean=false,val message: String="",val error: String="")
data class CatalogState(val page: Int=1,val items: List<OnlineWallpaper> = emptyList(),val loading: Boolean=false,val error: String="")
