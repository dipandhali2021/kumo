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
 private val _preview=MutableStateFlow<Bitmap?>(null)
 val preview=_preview.asStateFlow()
 private val _previewError=MutableStateFlow("")
 val previewError=_previewError.asStateFlow()
 private val _busy=MutableStateFlow(false)
 val busy=_busy.asStateFlow()
 val messages=MutableSharedFlow<String>(extraBufferCapacity=8)
 init {
  viewModelScope.launch {
   // Status-only changes do not need a new bitmap.
   settings.map { it.copy(lastApplied=0,lastError="",hours=0) }.distinctUntilChanged().collectLatest { s ->
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
 fun apply()=operation("Lock-screen wallpaper updated") { repo.apply() }
 fun export(uri: Uri)=operation("Wallpaper PNG saved") { repo.export(uri) }
 fun schedule(hours: Int)=operation(if(hours==0) "Automatic updates paused" else "Automatic updates enabled") {
  repo.edit { it.copy(hours=hours,lastError="") }
  RotationSchedule.set(getApplication(),hours)
 }
}
