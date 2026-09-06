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
 private val starter: List<Word> = context.assets.open("words.json").bufferedReader().use { reader ->
  val a = JSONArray(reader.readText())
  List(a.length()) { i -> a.getJSONObject(i).let {
   Word(it.getString("id"),it.getString("written"),it.getString("reading"),it.getString("meaning"),it.getString("category"))
  } }
 }
 private val cache=VocabularyCache(context)
 private val mutableWords=MutableStateFlow(starter+(5 downTo 1).flatMap {cache.load(it)})
 val words=mutableWords.asStateFlow()
 private val mutable = MutableStateFlow(load())
 val settings = mutable.asStateFlow()
 private fun load() = WallSettings(
  wordIndex=words.value.indexOfFirst {it.id==prefs.getString("wordId",null)}.takeIf {it>=0} ?: prefs.getInt("word",0).coerceIn(0,words.value.lastIndex),
  background=prefs.getString("background","Ocean") ?: "Ocean", photo=prefs.getString("photo","") ?: "",
  showReading=prefs.getBoolean("reading",true), showMeaning=prefs.getBoolean("meaning",true),
  scale=prefs.getFloat("scale",1f), position=prefs.getFloat("position",0.5f), panel=if(!prefs.getBoolean("panelV13",false) && prefs.getFloat("panel",0.4f)==0.4f) 0f else prefs.getFloat("panel",0f),
  cropX=prefs.getFloat("cropX",0.5f),cropY=prefs.getFloat("cropY",0.5f),hours=prefs.getInt("hours",0),
  lastApplied=prefs.getLong("lastApplied",0),lastError=prefs.getString("error","") ?: "",
  typography=TypographyCodec.decode(prefs.getString("typography",null),prefs.getBoolean("reading",true),prefs.getBoolean("meaning",true)),
  levels=prefs.getStringSet("levels",setOf("5"))!!.mapNotNull {it.toIntOrNull()?.takeIf {v->v in 1..5}}.toSet(),
  includeStarter=prefs.getBoolean("includeStarter",true),favorites=prefs.getStringSet("favorites",emptySet())!!.toSet(),
  favoritesOnly=prefs.getBoolean("favoritesOnly",false),shuffle=prefs.getBoolean("shuffle",false)
 )
 private fun save(requested: WallSettings) {
  val index=WordPolicy.choose(words.value,requested,false) ?: requested.wordIndex.coerceIn(0,words.value.lastIndex)
  val s=requested.copy(wordIndex=index)
  check(prefs.edit().putInt("word",s.wordIndex).putString("wordId",words.value[s.wordIndex].id).putBoolean("panelV13",true).putString("background",s.background).putString("photo",s.photo)
   .putBoolean("reading",s.showReading).putBoolean("meaning",s.showMeaning).putFloat("scale",s.scale)
   .putFloat("position",s.position).putFloat("panel",s.panel).putFloat("cropX",s.cropX).putFloat("cropY",s.cropY)
   .putStringSet("levels",s.levels.map {it.toString()}.toSet()).putBoolean("includeStarter",s.includeStarter)
   .putStringSet("favorites",s.favorites).putBoolean("favoritesOnly",s.favoritesOnly).putBoolean("shuffle",s.shuffle)
   .putString("typography",TypographyCodec.encode(s.typography)).putInt("hours",s.hours).putLong("lastApplied",s.lastApplied).putString("error",s.lastError).commit()) {
   "Could not save settings. Check available storage."
  }
  mutable.value = s
 }
 suspend fun edit(change: (WallSettings)->WallSettings) = withContext(Dispatchers.IO) {
  mutex.withLock { save(change(mutable.value)) }
 }
 fun downloadedAt(level: Int)=cache.updatedAt(level)
 suspend fun downloadLevel(level: Int): Int {
  val fetched=JlptClient.fetch(level)
  return withContext(Dispatchers.IO) {
   mutex.withLock {
    cache.save(level,fetched)
    val previous=words.value
    val replacements=fetched.associateBy {it.id}
    val oldIds=previous.map {it.id}.toSet()
    mutableWords.value=previous.map {replacements[it.id] ?: it}+fetched.filter {it.id !in oldIds}
    save(mutable.value.copy(lastError=""))
    fetched.size
   }
  }
 }
 suspend fun nextWord()=withContext(Dispatchers.IO) {
  mutex.withLock {
   val s=mutable.value
   val index=WordPolicy.choose(words.value,s,true) ?: error("No eligible words. Adjust filters in Words.")
   save(s.copy(wordIndex=index))
  }
 }
 suspend fun selectWord(id: String)=withContext(Dispatchers.IO) {
  mutex.withLock {
   val index=words.value.indexOfFirst {it.id==id}
   check(index>=0 && WordPolicy.eligible(words.value[index],mutable.value)) {"This word is not in your current filters."}
   save(mutable.value.copy(wordIndex=index))
  }
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
   renderer.render(s,starter.first(),360,(360f*h/w).roundToInt(),includeText=false)
  }
 }
 suspend fun apply(advance: Boolean=false, scheduled: Boolean=false): Boolean = withContext(Dispatchers.IO) {
  mutex.withLock {
   val old=mutable.value
   if(scheduled && old.hours==0) return@withLock false
   val index=WordPolicy.choose(words.value,old,advance) ?: error("No eligible words. Download a selected JLPT level or adjust filters in Words.")
   val s=old.copy(wordIndex=index)
   val manager=WallpaperManager.getInstance(context)
   check(manager.isWallpaperSupported && manager.isSetWallpaperAllowed) { "Wallpaper changes are blocked on this device." }
   val (w,h)=outputSize(); val bitmap=renderer.render(s,words.value[s.wordIndex],w,h)
   try {
    if(manager.setBitmap(bitmap,null,false,WallpaperManager.FLAG_LOCK)==0) throw IOException("Android could not apply the wallpaper.")
    save(s.copy(lastApplied=System.currentTimeMillis(),lastError=""))
   } finally { bitmap.recycle() }
   true
  }
 }
 suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
  mutex.withLock {
   val current=mutable.value
   val index=WordPolicy.choose(words.value,current,false) ?: error("No eligible words. Adjust filters in Words.")
   val s=current.copy(wordIndex=index); val (w,h)=outputSize(); val bitmap=renderer.render(s,words.value[s.wordIndex],w,h)
   try {
    context.contentResolver.openOutputStream(uri)?.use {
     check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) { "Could not save image." }
    } ?: error("Cannot write to this location.")
   } finally { bitmap.recycle() }
  }
 }
}
