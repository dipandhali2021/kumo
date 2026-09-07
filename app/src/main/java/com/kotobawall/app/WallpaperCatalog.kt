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

data class OnlineWallpaper(val id: String,val author: String,val width: Int,val height: Int,val sourceUrl: String,
 val provider: WallpaperProvider=WallpaperProvider.PICSUM,val previewUrl: String="",val downloadUrl: String="",
 val description: String="",val photographerUrl: String="") {
 val thumbnail: String get()=if(provider==WallpaperProvider.PEXELS) previewUrl else "https://picsum.photos/id/$id/360/540.jpg"
 val imageUrl: String get() {
  if(provider==WallpaperProvider.PEXELS) return downloadUrl
  val ratio=minOf(1f,2400f/maxOf(width,height))
  return "https://picsum.photos/id/"+id+"/"+(width*ratio).roundToInt().coerceAtLeast(1)+"/"+(height*ratio).roundToInt().coerceAtLeast(1)+".jpg"
 }
}
object WallpaperCatalog {
 // Green grassfield by Paul Jarvis: unsplash.com/photos/Cm7oKel-X2Q. Picsum republishes it as id 11,
 // so the app can fetch it over the existing no-key path instead of shipping a photo inside the APK.
 val DEFAULT=OnlineWallpaper("11","Paul Jarvis",2500,1667,"https://unsplash.com/photos/Cm7oKel-X2Q")
 // Picsum's ids 0-9 are the old Unsplash sample set: desks, laptops and studio shots that make poor
 // lock screens, and they crowd out the first page of results.
 private val excludedAuthors=setOf("alejandro escamilla")
 // Requested with room to spare, because filtering can drop most of a page near the start of the feed.
 private const val PAGE=24
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
 /** Drops photographers excluded from browsing. Kept separate from parse so it can be tested directly. */
 fun eligible(items: List<OnlineWallpaper>): List<OnlineWallpaper> =
  items.filterNot {it.author.trim().lowercase() in excludedAuthors}
 suspend fun list(page: Int): List<OnlineWallpaper> = withContext(Dispatchers.IO) {
  require(page in 1..1000)
  eligible(parse(String(read("https://picsum.photos/v2/list?page=$page&limit=$PAGE",256*1024),Charsets.UTF_8)))
 }
 suspend fun image(item: OnlineWallpaper): ByteArray = withContext(Dispatchers.IO) {read(item.imageUrl,16*1024*1024)}
 private suspend fun read(address: String,limit: Int): ByteArray {
  var url=URL(address)
  repeat(4) {
   check(url.protocol=="https" && url.host in setOf("picsum.photos","fastly.picsum.photos","images.pexels.com") && url.userInfo==null && url.port in listOf(-1,443)) {"Unexpected image host."}
   val c=url.openConnection() as HttpURLConnection
   try {
    c.connectTimeout=15_000;c.readTimeout=20_000;c.instanceFollowRedirects=false
    c.setRequestProperty("User-Agent","KotobaWall/1.5")
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
