package com.kotobawall.app

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.roundToInt

class KotobaApplication : Application() {
 val repository by lazy { WallRepository(this) }
}
class WallRepository(private val context: Context) {
 private val prefs = context.getSharedPreferences("wall_v1",Context.MODE_PRIVATE)
 private val mutex = Mutex()
 private val renderer = WallpaperRenderer(context)
 val words: List<Word> = context.assets.open("words.json").bufferedReader().use { reader ->
  val a = JSONArray(reader.readText())
  List(a.length()) { i -> a.getJSONObject(i).let {
   Word(it.getString("id"),it.getString("written"),it.getString("reading"),it.getString("meaning"),it.getString("category"))
  } }
 }
 private val mutable = MutableStateFlow(load())
 val settings = mutable.asStateFlow()
 private fun load() = WallSettings(
  wordIndex=prefs.getInt("word",0).coerceIn(0,words.lastIndex),
  background=prefs.getString("background","Ocean") ?: "Ocean", photo=prefs.getString("photo","") ?: "",
  showReading=prefs.getBoolean("reading",true), showMeaning=prefs.getBoolean("meaning",true),
  scale=prefs.getFloat("scale",1f), position=prefs.getFloat("position",0.5f), panel=prefs.getFloat("panel",0.4f),
  cropX=prefs.getFloat("cropX",0.5f),cropY=prefs.getFloat("cropY",0.5f),hours=prefs.getInt("hours",0),
  lastApplied=prefs.getLong("lastApplied",0),lastError=prefs.getString("error","") ?: "",
  typography=TypographyCodec.decode(prefs.getString("typography",null),prefs.getBoolean("reading",true),prefs.getBoolean("meaning",true))
 )
 private fun save(s: WallSettings) {
  check(prefs.edit().putInt("word",s.wordIndex).putString("background",s.background).putString("photo",s.photo)
   .putBoolean("reading",s.showReading).putBoolean("meaning",s.showMeaning).putFloat("scale",s.scale)
   .putFloat("position",s.position).putFloat("panel",s.panel).putFloat("cropX",s.cropX).putFloat("cropY",s.cropY)
   .putString("typography",TypographyCodec.encode(s.typography)).putInt("hours",s.hours).putLong("lastApplied",s.lastApplied).putString("error",s.lastError).commit()) {
   "Could not save settings. Check available storage."
  }
  mutable.value = s
 }
 suspend fun edit(change: (WallSettings)->WallSettings) = withContext(Dispatchers.IO) {
  mutex.withLock { save(change(mutable.value)) }
 }
 suspend fun importPhoto(uri: Uri) = withContext(Dispatchers.IO) {
  mutex.withLock {
   val dest = File(context.filesDir,"photo-${UUID.randomUUID()}")
   try {
    context.contentResolver.openInputStream(uri)?.use { input ->
     dest.outputStream().use { output ->
      val buffer = ByteArray(8192); var total = 0L
      while(true) {
       val n = input.read(buffer); if(n == -1) break
       total += n; check(total <= 40L*1024*1024) { "Choose an image smaller than 40 MB." }
       output.write(buffer,0,n)
      }
     }
    } ?: error("Cannot open selected photo.")
    val b = BitmapFactory.Options().apply { inJustDecodeBounds=true }
    BitmapFactory.decodeFile(dest.path,b)
    check(b.outWidth>0 && b.outHeight>0) { "Unsupported image. Try JPEG or PNG." }
    val old = mutable.value.photo
    save(mutable.value.copy(photo=dest.name,cropX=0.5f,cropY=0.5f))
    if(old.isNotEmpty()) File(context.filesDir,old).delete()
   } catch(e: Exception) { dest.delete(); throw e }
  }
 }
 suspend fun usePalette(name: String) = withContext(Dispatchers.IO) {
  mutex.withLock {
   val old=mutable.value.photo
   save(mutable.value.copy(photo="",background=name))
   if(old.isNotEmpty()) File(context.filesDir,old).delete()
  }
 }
 // Physical display mode avoids sizing the wallpaper to a multi-window app viewport.
 fun outputSize(): Pair<Int,Int> {
  val display=context.getSystemService(DisplayManager::class.java).getDisplay(android.view.Display.DEFAULT_DISPLAY)
  val mode=display?.mode
  val a=mode?.physicalWidth ?: 1080; val b=mode?.physicalHeight ?: 1920
  val w=minOf(a,b); val h=maxOf(a,b)
  val scale=minOf(1f,1440f/w,3200f/h)
  return (w*scale).roundToInt().coerceAtLeast(1) to (h*scale).roundToInt().coerceAtLeast(1)
 }
 suspend fun preview(s: WallSettings): Bitmap = withContext(Dispatchers.Default) {
  mutex.withLock {
   val (w,h)=outputSize()
   renderer.render(s,words[s.wordIndex],360,(360f*h/w).roundToInt(),includeText=false)
  }
 }
 suspend fun apply(advance: Boolean=false, scheduled: Boolean=false): Boolean = withContext(Dispatchers.IO) {
  mutex.withLock {
   val old=mutable.value
   if(scheduled && old.hours==0) return@withLock false
   val s=if(advance) old.copy(wordIndex=WallMath.nextIndex(old.wordIndex,words.size)) else old
   val manager=WallpaperManager.getInstance(context)
   check(manager.isWallpaperSupported && manager.isSetWallpaperAllowed) { "Wallpaper changes are blocked on this device." }
   val (w,h)=outputSize(); val bitmap=renderer.render(s,words[s.wordIndex],w,h)
   try {
    if(manager.setBitmap(bitmap,null,false,WallpaperManager.FLAG_LOCK)==0) throw IOException("Android could not apply the wallpaper.")
    save(s.copy(lastApplied=System.currentTimeMillis(),lastError=""))
   } finally { bitmap.recycle() }
   true
  }
 }
 suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
  mutex.withLock {
   val s=mutable.value; val (w,h)=outputSize(); val bitmap=renderer.render(s,words[s.wordIndex],w,h)
   try {
    context.contentResolver.openOutputStream(uri)?.use {
     check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) { "Could not save image." }
    } ?: error("Cannot write to this location.")
   } finally { bitmap.recycle() }
  }
 }
}
