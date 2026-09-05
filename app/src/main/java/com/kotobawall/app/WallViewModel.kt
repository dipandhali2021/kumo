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
 init {
  ScreenCycleController.initialize(app)
  viewModelScope.launch {
   // Only background/crop changes need a decoded bitmap. Text is drawn live by Compose.
   settings.map { it.copy(lastApplied=0,lastError="",hours=0,wordIndex=0,scale=1f,position=0.65f,panel=0.4f,showReading=true,showMeaning=true,typography=Typography()) }.distinctUntilChanged().collectLatest { s ->
    delay(180)
    try { _previewError.value=""; _preview.value=repo.preview(s) }
    catch(e: CancellationException) { throw e }
    catch(e: Exception) { _preview.value=null; _previewError.value=e.message ?: "Preview failed"; messages.emit(_previewError.value) }
   }
  }
  // Reconcile the persisted opt-in with WorkManager when the app opens.
  RotationSchedule.set(app,settings.value.hours)
 }
 private fun operation(success: String?=null, block: suspend ()->Unit) {
  if(_busy.value) return
  _busy.value=true
  viewModelScope.launch {
   try { block(); if(success!=null) messages.emit(success) }
   catch(e: CancellationException) { throw e }
   catch(e: Exception) { messages.emit(e.message ?: "Something went wrong. Please try again.") }
   finally { _busy.value=false }
  }
 }
 fun edit(change: (WallSettings)->WallSettings) {
  if(_busy.value) return
  viewModelScope.launch {
   try { repo.edit(change) }
   catch(e: CancellationException) { throw e }
   catch(e: Exception) { messages.emit(e.message ?: "Could not save settings") }
  }
 }
 fun pick(uri: Uri)=operation("Background saved on this device") { repo.importPhoto(uri) }
 fun palette(name: String)=operation { repo.usePalette(name) }
 fun next()=edit { it.copy(wordIndex=WallMath.nextIndex(it.wordIndex,words.size)) }
 fun saveTypography(t: Typography)=operation("Line layout saved") { repo.edit { it.copy(typography=t) }; draft.value=null }
 fun apply(t: Typography?=null)=operation("Lock-screen wallpaper updated") {
  if(t!=null) { repo.edit { it.copy(typography=t) }; draft.value=null }
  repo.apply()
 }
 fun export(uri: Uri)=operation("Wallpaper PNG saved") { repo.export(uri) }
 fun startCycle()=operation("Screen-off updates started. Turn your screen off, wait a moment, then wake it.") {
  repo.edit { it.copy(hours=0,lastError="") }
  RotationSchedule.set(getApplication(),0)
  ScreenCycleController.start(getApplication())
 }
 fun stopCycle() { ScreenCycleController.stop(getApplication()) }
 fun schedule(hours: Int)=operation(if(hours==0) "Automatic updates paused" else "Automatic updates enabled") {
  ScreenCycleController.stop(getApplication())
  repo.edit { it.copy(hours=hours,lastError="") }
  RotationSchedule.set(getApplication(),hours)
 }
}
