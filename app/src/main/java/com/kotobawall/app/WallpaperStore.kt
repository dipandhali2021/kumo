package com.kotobawall.app

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

data class SavedWallpaper(val file: String,val title: String,val author: String="",val sourceUrl: String="")
data class LastWallpaper(val photo: String,val background: String,val cropX: Float,val cropY: Float,val wallpaperId: Int,val sourceKey: String)
object WallpaperPolicy {
 fun next(files: List<String>,current: String): String? =
  if(files.isEmpty()) null else files[(files.indexOf(current)+1)%files.size]
}
// Writes are called under the repository mutex on an IO dispatcher.
class WallpaperStore(private val context: Context) {
 private val directory=File(context.filesDir,"wallpapers").apply {mkdirs()}
 private val index=AtomicFile(File(directory,"library.json"))
 private val lastIndex=AtomicFile(File(directory,"last.json"))
 private fun safe(path: String)=path.matches(Regex("wallpapers/[a-zA-Z0-9._-]+"))
 private val mutable=MutableStateFlow(load())
 val saved=mutable.asStateFlow()
 private val mutableLast=MutableStateFlow(loadLast())
 val last=mutableLast.asStateFlow()
 fun file(path: String): File {require(safe(path));return File(context.filesDir,path)}
 fun owns(path: String)=safe(path)
 private fun load(): List<SavedWallpaper> = try {
  val a=JSONArray(String(index.readFully(),Charsets.UTF_8))
  List(minOf(a.length(),12)) {i ->val j=a.getJSONObject(i);SavedWallpaper(j.getString("file"),j.getString("title"),j.optString("author"),j.optString("sourceUrl"))}
   .filter {safe(it.file) && File(context.filesDir,it.file).isFile}.distinctBy {it.file}
 } catch(_: Exception) {emptyList()}
 private fun loadLast(): LastWallpaper? = try {
  val j=JSONObject(String(lastIndex.readFully(),Charsets.UTF_8));val path=j.getString("photo")
  if(path.isNotEmpty() && (!safe(path)||!File(context.filesDir,path).isFile)) null else
   LastWallpaper(path,j.getString("background"),j.getDouble("cropX").toFloat(),j.getDouble("cropY").toFloat(),j.getInt("wallpaperId"),j.getString("sourceKey"))
 } catch(_: Exception) {null}
 private fun write(target: AtomicFile,text: String) {
  val stream=target.startWrite()
  try {stream.write(text.toByteArray(Charsets.UTF_8));target.finishWrite(stream)}
  catch(e: Exception) {target.failWrite(stream);throw e}
 }
 private fun save(entries: List<SavedWallpaper>) {
  val data=JSONArray().apply {entries.forEach {e ->put(JSONObject().apply {
   put("file",e.file);put("title",e.title);put("author",e.author);put("sourceUrl",e.sourceUrl)
  })}}
  write(index,data.toString());mutable.value=entries
 }
 fun add(input: InputStream,title: String,author: String="",sourceUrl: String=""): SavedWallpaper {
  check(saved.value.size<12) {"Your collection holds 12 wallpapers. Remove one before adding another."}
  val used=directory.listFiles()?.sumOf {it.length()} ?: 0L
  val relative="wallpapers/photo-${UUID.randomUUID()}.img";val target=file(relative)
  try {
   target.outputStream().use {out ->
    val buffer=ByteArray(8192);var total=0L
    while(true) {
     val n=input.read(buffer);if(n<0) break
     total+=n
     check(total<=40L*1024*1024 && used+total<=160L*1024*1024) {"Wallpaper storage limit reached. Choose a smaller photo or remove saved images."}
     out.write(buffer,0,n)
    }
   }
   val b=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeFile(target.path,b)
   check(b.outWidth>0 && b.outHeight>0) {"The source did not return a readable image."}
   val entry=SavedWallpaper(relative,title.take(100),author.take(120),sourceUrl)
   save(saved.value+entry);return entry
  } catch(e: Exception) {target.delete();throw e}
 }
 fun remove(path: String,current: String) {
  check(path!=current) {"Choose another background before removing this wallpaper."}
  check(saved.value.any {it.file==path}) {"Wallpaper not found."}
  save(saved.value.filterNot {it.file==path});file(path).delete()
 }
 fun recordLast(s: WallSettings,id: Int) {
  val key="${s.photo}|${s.background}|${s.cropX}|${s.cropY}"
  val old=last.value
  var path=old?.photo ?: ""
  val changed=old?.sourceKey!=key || (path.isNotEmpty() && !file(path).isFile)
  if(changed) {
   path=if(s.photo.isBlank()) "" else "wallpapers/last-${UUID.randomUUID()}.img"
   if(path.isNotEmpty()) File(context.filesDir,s.photo).copyTo(file(path),overwrite=true)
  }
  val value=LastWallpaper(path,s.background,s.cropX,s.cropY,id,key)
  try {
   write(lastIndex,JSONObject().apply {
    put("photo",path);put("background",s.background);put("cropX",s.cropX);put("cropY",s.cropY);put("wallpaperId",id);put("sourceKey",key)
   }.toString())
   mutableLast.value=value
   if(changed && old!=null && old.photo.isNotEmpty() && old.photo!=path) file(old.photo).delete()
  } catch(e: Exception) {if(changed && path.isNotEmpty()) file(path).delete();throw e}
 }
}
