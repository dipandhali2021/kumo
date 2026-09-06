package com.kotobawall.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class OnlineWallpaper(val id: String,val author: String,val width: Int,val height: Int,val sourceUrl: String) {
 val thumbnail: String get()="https://picsum.photos/id/$id/360/540.jpg"
 val imageUrl: String get() {
  val ratio=minOf(1f,2400f/maxOf(width,height))
  return "https://picsum.photos/id/"+id+"/"+(width*ratio).roundToInt().coerceAtLeast(1)+"/"+(height*ratio).roundToInt().coerceAtLeast(1)+".jpg"
 }
}
object WallpaperCatalog {
 fun parse(json: String): List<OnlineWallpaper> {
  val data=JSONArray(json);check(data.length()<=100) {"Unexpected gallery response."}
  return List(data.length()) {i ->
   val j=data.getJSONObject(i);val id=j.getString("id")
   check(id.matches(Regex("[0-9]{1,10}"))) {"Invalid image identifier."}
   val w=j.getInt("width");val h=j.getInt("height")
   check(w in 1..50000 && h in 1..50000) {"Invalid image dimensions."}
   val source=j.optString("url").takeIf {it.startsWith("https://unsplash.com/")} ?: "https://picsum.photos/images"
   OnlineWallpaper(id,j.optString("author","Unknown photographer").take(120),w,h,source)
  }.distinctBy {it.id}
 }
 suspend fun list(page: Int): List<OnlineWallpaper> = withContext(Dispatchers.IO) {
  require(page in 1..1000)
  parse(String(read("https://picsum.photos/v2/list?page=$page&limit=12",256*1024),Charsets.UTF_8))
 }
 suspend fun image(item: OnlineWallpaper): ByteArray = withContext(Dispatchers.IO) {read(item.imageUrl,16*1024*1024)}
 private suspend fun read(address: String,limit: Int): ByteArray {
  var url=URL(address)
  repeat(4) {
   check(url.protocol=="https" && url.host in setOf("picsum.photos","fastly.picsum.photos")) {"Unexpected image host."}
   val c=url.openConnection() as HttpURLConnection
   try {
    c.connectTimeout=15_000;c.readTimeout=20_000;c.instanceFollowRedirects=false
    c.setRequestProperty("User-Agent","KotobaWall/1.4")
    val code=c.responseCode
    if(code in listOf(301,302,303,307,308)) {
     url=URL(url,c.getHeaderField("Location") ?: throw IOException("Missing image redirect."))
    } else {
     if(code!=200) throw IOException("Photo service returned HTTP $code. Saved wallpapers still work offline.")
     val data=ByteArrayOutputStream()
     c.inputStream.use {input ->
      val buffer=ByteArray(8192)
      while(true) {
       currentCoroutineContext().ensureActive()
       val n=input.read(buffer);if(n<0) break
       check(data.size()+n<=limit) {"Photo response is too large."}
       data.write(buffer,0,n)
      }
     }
     return data.toByteArray()
    }
   } finally {c.disconnect()}
  }
  throw IOException("Too many image redirects.")
 }
}
