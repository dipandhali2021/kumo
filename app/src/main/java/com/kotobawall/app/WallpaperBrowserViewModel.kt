package com.kotobawall.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WallpaperBrowseState(
 val provider: WallpaperProvider=WallpaperProvider.PEXELS,val query: String="",val orientation: String="portrait",
 val page: Int=1,val items: List<OnlineWallpaper> = emptyList(),val loading: Boolean=false,
 val loaded: Boolean=false,val hasNext: Boolean=false,val error: String=""
)
data class PexelsKeyStatus(val ready: Boolean=false,val present: Boolean=false,val saving: Boolean=false,val error: String="")
private data class PhotoRequest(val provider: WallpaperProvider,val query: String,val orientation: String,val page: Int)

class WallpaperBrowserViewModel(app: Application): AndroidViewModel(app) {
 private val vault=PexelsKeyStore(app)
 private var apiKey=""
 private var opened=false
 private var requestJob: Job?=null
 private var generation=0
 private val pages=linkedMapOf<PhotoRequest,Pair<Long,PhotoPage>>()
 private val _state=MutableStateFlow(WallpaperBrowseState())
 val state=_state.asStateFlow()
 private val _key=MutableStateFlow(PexelsKeyStatus())
 val keyStatus=_key.asStateFlow()
 init {viewModelScope.launch {
  try {apiKey=withContext(Dispatchers.IO) {vault.read()};_key.value=PexelsKeyStatus(ready=true,present=apiKey.isNotEmpty())}
  catch(e: CancellationException) {throw e}
  catch(e: Exception) {_key.value=PexelsKeyStatus(ready=true,error="Could not read the saved key. Enter it again or remove it.")}
  if(opened) load()
 }}
 fun open() {opened=true;if(_key.value.ready && !_state.value.loaded && !_state.value.loading) load()}
 fun selectProvider(provider: WallpaperProvider) {
  if(provider==_state.value.provider) return
  _state.value=WallpaperBrowseState(provider=provider);load()
 }
 fun search(query: String,orientation: String=_state.value.orientation) {
  _state.value=_state.value.copy(query=query.trim().take(100),orientation=orientation,page=1,items=emptyList(),loaded=false,hasNext=false)
  load()
 }
 fun load(page: Int=_state.value.page) {
  if(!_key.value.ready || _key.value.saving || page !in 1..1000) return
  requestJob?.cancel();val ticket=++generation
  val s=_state.value
  if(s.provider==WallpaperProvider.PEXELS && apiKey.isEmpty()) {
   _state.value=s.copy(page=1,items=emptyList(),loading=false,loaded=false,hasNext=false,error="Add your Pexels API key, or choose Unsplash / Picsum without a key.");return
  }
  val request=PhotoRequest(s.provider,if(s.provider==WallpaperProvider.PEXELS) s.query else "",if(s.query.isBlank()) "" else s.orientation,page)
  _state.value=s.copy(page=page,items=if(page==s.page) s.items else emptyList(),loading=true,error="",hasNext=false)
  val keyForRequest=apiKey
  requestJob=viewModelScope.launch {
   try {
    val cached=pages[request]?.takeIf {System.currentTimeMillis()-it.first in 0..86_400_000L}?.second
    val result=cached ?: if(request.provider==WallpaperProvider.PEXELS) PexelsClient.list(keyForRequest,request.query,request.page,request.orientation)
     else WallpaperCatalog.list(page).let {PhotoPage(it,it.size==12 && page<1000)}
    if(ticket!=generation) return@launch
    if(cached==null) {if(pages.size>=24) pages.remove(pages.keys.first());pages[request]=System.currentTimeMillis() to result}
    _state.value=_state.value.copy(items=result.items,loading=false,loaded=true,hasNext=result.hasNext && page<1000,
     error=if(result.items.isEmpty()) "No photos found. Try another topic or shape." else "")
   } catch(e: CancellationException) {throw e}
   catch(e: Exception) {if(ticket==generation) _state.value=_state.value.copy(loading=false,loaded=true,error=e.message ?: "Could not load photos. Try again.")}
  }
 }
 fun saveKey(value: String,onSaved: ()->Unit) {
  if(!_key.value.ready || _key.value.saving) return
  val raw=value.trim()
  if(!PexelsClient.validKey(raw)) {_key.value=_key.value.copy(error="Enter a valid API key without spaces.");return}
  _key.value=_key.value.copy(saving=true,error="")
  viewModelScope.launch {
   try {
    withContext(Dispatchers.IO) {vault.write(raw)}
    apiKey=raw;pages.clear();_key.value=PexelsKeyStatus(ready=true,present=true);onSaved()
    if(_state.value.provider==WallpaperProvider.PEXELS) load(1)
   } catch(e: CancellationException) {throw e}
   catch(e: Exception) {_key.value=_key.value.copy(saving=false,error="Could not save the key securely. Please try again.")}
  }
 }
 fun clearKey() {
  if(!_key.value.ready || _key.value.saving) return
  _key.value=_key.value.copy(saving=true,error="")
  viewModelScope.launch {
   try {
    withContext(Dispatchers.IO) {vault.clear()}
    apiKey="";pages.clear();_key.value=PexelsKeyStatus(ready=true)
    if(_state.value.provider==WallpaperProvider.PEXELS) load(1)
   } catch(e: CancellationException) {throw e}
   catch(e: Exception) {_key.value=_key.value.copy(saving=false,error="Could not remove the saved key. Try again.")}
  }
 }
}
