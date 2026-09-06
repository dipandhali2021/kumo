package com.kotobawall.app

import android.os.ParcelFileDescriptor
import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
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
import kotlin.math.roundToInt

class KotobaApplication : Application() {
 val repository by lazy {WallRepository(this)}
}
class WallRepository(private val context: Context) {
 private val prefs=context.getSharedPreferences("wall_v1",Context.MODE_PRIVATE)
 private val mutex=Mutex()
 private val renderer=WallpaperRenderer(context)
 private val wallpapers=WallpaperStore(context)
 val savedWallpapers=wallpapers.saved
 val lastWallpaper=wallpapers.last
 private val starter: List<Word> = context.assets.open("words.json").bufferedReader().use {reader ->
  val a=JSONArray(reader.readText())
  List(a.length()) {i ->a.getJSONObject(i).let {
   Word(it.getString("id"),it.getString("written"),it.getString("reading"),it.getString("meaning"),it.getString("category"))
  }}
 }
 private val cache=VocabularyCache(context)
 private val mutableWords=MutableStateFlow(starter+(5 downTo 1).flatMap {cache.load(it)})
 val words=mutableWords.asStateFlow()
 private val mutable=MutableStateFlow(load())
 val settings=mutable.asStateFlow()
 private fun load()=WallSettings(
  wordIndex=words.value.indexOfFirst {it.id==prefs.getString("wordId",null)}.takeIf {it>=0} ?: prefs.getInt("word",0).coerceIn(0,words.value.lastIndex),
  background=prefs.getString("background","Ocean") ?: "Ocean",photo=prefs.getString("photo","") ?: "",
  showReading=prefs.getBoolean("reading",true),showMeaning=prefs.getBoolean("meaning",true),
  scale=prefs.getFloat("scale",1f),position=prefs.getFloat("position",0.5f),panel=if(!prefs.getBoolean("panelV13",false) && prefs.getFloat("panel",0.4f)==0.4f) 0f else prefs.getFloat("panel",0f),
  cropX=prefs.getFloat("cropX",0.5f),cropY=prefs.getFloat("cropY",0.5f),hours=prefs.getInt("hours",0),
  lastApplied=prefs.getLong("lastApplied",0),lastError=prefs.getString("error","") ?: "",
  typography=TypographyCodec.decode(prefs.getString("typography",null),prefs.getBoolean("reading",true),prefs.getBoolean("meaning",true)),
  levels=prefs.getStringSet("levels",setOf("5"))!!.mapNotNull {it.toIntOrNull()?.takeIf {v->v in 1..5}}.toSet(),
  includeStarter=prefs.getBoolean("includeStarter",true),favorites=prefs.getStringSet("favorites",emptySet())!!.toSet(),
  favoritesOnly=prefs.getBoolean("favoritesOnly",false),shuffle=prefs.getBoolean("shuffle",false),rotateWallpaper=prefs.getBoolean("rotateWallpaper",false)
 )
 private fun save(requested: WallSettings) {
  val index=WordPolicy.choose(words.value,requested,false) ?: requested.wordIndex.coerceIn(0,words.value.lastIndex)
  val s=requested.copy(wordIndex=index)
  check(prefs.edit().putInt("word",s.wordIndex).putString("wordId",words.value[s.wordIndex].id).putBoolean("panelV13",true).putString("background",s.background).putString("photo",s.photo)
   .putBoolean("reading",s.showReading).putBoolean("meaning",s.showMeaning).putFloat("scale",s.scale)
   .putFloat("position",s.position).putFloat("panel",s.panel).putFloat("cropX",s.cropX).putFloat("cropY",s.cropY)
   .putStringSet("levels",s.levels.map {it.toString()}.toSet()).putBoolean("includeStarter",s.includeStarter)
   .putStringSet("favorites",s.favorites).putBoolean("favoritesOnly",s.favoritesOnly).putBoolean("shuffle",s.shuffle).putBoolean("rotateWallpaper",s.rotateWallpaper)
   .putString("typography",TypographyCodec.encode(s.typography)).putInt("hours",s.hours).putLong("lastApplied",s.lastApplied).putString("error",s.lastError).commit()) {
   "Could not save settings. Check available storage."
  }
  mutable.value=s
 }
 suspend fun edit(change: (WallSettings)->WallSettings)=withContext(Dispatchers.IO) {mutex.withLock {save(change(mutable.value))}}
 fun downloadedAt(level: Int)=cache.updatedAt(level)
 suspend fun downloadLevel(level: Int): Int {
  val fetched=JlptClient.fetch(level)
  return withContext(Dispatchers.IO) {mutex.withLock {
   cache.save(level,fetched)
   val previous=words.value;val replacements=fetched.associateBy {it.id};val oldIds=previous.map {it.id}.toSet()
   mutableWords.value=previous.map {replacements[it.id] ?: it}+fetched.filter {it.id !in oldIds}
   save(mutable.value.copy(lastError=""));fetched.size
  }}
 }
 suspend fun nextWord()=withContext(Dispatchers.IO) {mutex.withLock {
  val s=mutable.value
  val index=WordPolicy.choose(words.value,s,true) ?: error("No eligible words. Adjust filters in Words.")
  save(s.copy(wordIndex=index))
 }}
 suspend fun selectWord(id: String)=withContext(Dispatchers.IO) {mutex.withLock {
  val index=words.value.indexOfFirst {it.id==id}
  check(index>=0 && WordPolicy.eligible(words.value[index],mutable.value)) {"This word is not in your current filters."}
  save(mutable.value.copy(wordIndex=index))
 }}
 private fun deleteOldPhoto(path: String) {
  if(path.isNotEmpty() && !wallpapers.owns(path)) File(context.filesDir,path).delete()
 }
 suspend fun importPhoto(uri: Uri)=withContext(Dispatchers.IO) {mutex.withLock {
  val entry=context.contentResolver.openInputStream(uri)?.use {wallpapers.add(it,"My photo")} ?: error("Cannot open selected photo.")
  val old=mutable.value.photo
  save(mutable.value.copy(photo=entry.file,cropX=0.5f,cropY=0.5f));deleteOldPhoto(old)
 }}
 suspend fun saveOnlineWallpaper(item: OnlineWallpaper): String {
  savedWallpapers.value.firstOrNull {it.title=="Photo ${item.id}" && it.sourceUrl==item.sourceUrl && wallpapers.file(it.file).isFile}?.let {return it.file}
  val bytes=WallpaperCatalog.image(item)
  return withContext(Dispatchers.IO) {mutex.withLock {
   bytes.inputStream().use {wallpapers.add(it,"Photo ${item.id}",item.author,item.sourceUrl).file}
  }}
 }
 suspend fun chooseWallpaper(path: String)=withContext(Dispatchers.IO) {mutex.withLock {
  check(savedWallpapers.value.any {it.file==path} && wallpapers.file(path).isFile) {"Saved wallpaper is missing."}
  val old=mutable.value.photo
  save(mutable.value.copy(photo=path,cropX=0.5f,cropY=0.5f));if(old!=path) deleteOldPhoto(old)
 }}
 suspend fun removeWallpaper(path: String)=withContext(Dispatchers.IO) {mutex.withLock {wallpapers.remove(path,mutable.value.photo)}}
 private fun restoreLast() {
  val last=lastWallpaper.value ?: error("Apply a wallpaper once to save its clean background here.")
  val old=mutable.value.photo;val original=last.sourceKey.substringBefore("|")
  val path=when {
   last.photo.isEmpty() -> ""
   savedWallpapers.value.any {it.file==original} && wallpapers.file(original).isFile -> original
   else -> wallpapers.file(last.photo).inputStream().use {wallpapers.add(it,"Last-used background").file}
  }
  save(mutable.value.copy(photo=path,background=last.background,cropX=last.cropX,cropY=last.cropY))
  if(old!=path) deleteOldPhoto(old)
 }
 suspend fun useLastWallpaper()=withContext(Dispatchers.IO) {mutex.withLock {restoreLast()}}
 suspend fun importCurrentWallpaper()=withContext(Dispatchers.IO) {mutex.withLock {
  val manager=WallpaperManager.getInstance(context);val last=lastWallpaper.value
  if(last!=null && last.wallpaperId>0 && manager.getWallpaperId(WallpaperManager.FLAG_LOCK)==last.wallpaperId) {
   restoreLast();return@withLock
  }
  try {
   val descriptor=LegacyWallpaperReader.open(context)
    ?: error("No readable static wallpaper. Choose its original image instead.")
   val entry=ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {wallpapers.add(it,"Imported lock-screen wallpaper")}
   val old=mutable.value.photo
   save(mutable.value.copy(photo=entry.file,cropX=0.5f,cropY=0.5f));deleteOldPhoto(old)
  } catch(e: SecurityException) {throw IllegalStateException("Android blocked wallpaper access. Choose its original image instead.",e)}
 }}
 suspend fun usePalette(name: String)=withContext(Dispatchers.IO) {mutex.withLock {
  val old=mutable.value.photo;save(mutable.value.copy(photo="",background=name));deleteOldPhoto(old)
 }}
 fun outputSize(): Pair<Int,Int> {
  val display=context.getSystemService(DisplayManager::class.java).getDisplay(android.view.Display.DEFAULT_DISPLAY)
  val mode=display?.mode
  val w=mode?.physicalWidth ?: 1080;val h=mode?.physicalHeight ?: 1920
  val scale=minOf(1f,2160f/w,3840f/h,kotlin.math.sqrt(4_000_000f/(w.toFloat()*h)))
  return (w*scale).roundToInt().coerceAtLeast(1) to (h*scale).roundToInt().coerceAtLeast(1)
 }
 suspend fun preview(s: WallSettings): Bitmap=withContext(Dispatchers.Default) {mutex.withLock {
  val (w,h)=outputSize();renderer.render(s,starter.first(),360,(360f*h/w).roundToInt(),includeText=false)
 }}
 suspend fun apply(advance: Boolean=false,scheduled: Boolean=false,screenCycle: Boolean=false): Boolean=withContext(Dispatchers.IO) {
  mutex.withLock {
   val old=mutable.value
   if(scheduled && old.hours==0) return@withLock false
   val index=WordPolicy.choose(words.value,old,advance) ?: error("No eligible words. Download a selected JLPT level or adjust filters in Words.")
   var s=old.copy(wordIndex=index)
   if(screenCycle && s.rotateWallpaper) {
    val next=WallpaperPolicy.next(savedWallpapers.value.filter {wallpapers.file(it.file).isFile}.map {it.file},s.photo)
     ?: error("No saved wallpapers. Add backgrounds in Wallpapers or switch to static mode.")
    s=s.copy(photo=next,cropX=0.5f,cropY=0.5f)
   }
   val manager=WallpaperManager.getInstance(context)
   check(manager.isWallpaperSupported && manager.isSetWallpaperAllowed) {"Wallpaper changes are blocked on this device."}
   val (w,h)=outputSize();val bitmap=renderer.render(s,words.value[s.wordIndex],w,h)
   try {
    val id=manager.setBitmap(bitmap,null,false,WallpaperManager.FLAG_LOCK)
    if(id==0) throw IOException("Android could not apply the wallpaper.")
    try {wallpapers.recordLast(s,id)} catch(e: Exception) {throw IOException("Wallpaper applied, but saving its clean background failed: ${e.message}",e)}
    save(s.copy(lastApplied=System.currentTimeMillis(),lastError=""))
   } finally {bitmap.recycle()}
   true
  }
 }
 suspend fun export(uri: Uri)=withContext(Dispatchers.IO) {mutex.withLock {
  val current=mutable.value
  val index=WordPolicy.choose(words.value,current,false) ?: error("No eligible words. Adjust filters in Words.")
  val s=current.copy(wordIndex=index);val (w,h)=outputSize();val bitmap=renderer.render(s,words.value[s.wordIndex],w,h)
  try {
   context.contentResolver.openOutputStream(uri)?.use {
    check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) {"Could not save image."}
   } ?: error("Cannot write to this location.")
  } finally {bitmap.recycle()}
 }}
}
