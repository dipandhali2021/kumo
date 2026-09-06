package com.kotobawall.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

enum class WallpaperProvider(val label: String) { PEXELS("Pexels"), PICSUM("Unsplash / Picsum") }
data class PhotoPage(val items: List<OnlineWallpaper>,val hasNext: Boolean)

object PexelsClient {
 fun validKey(key: String): Boolean = key.matches(Regex("[A-Za-z0-9_-]{20,256}"))
 fun requestUrl(query: String,page: Int,orientation: String): String {
  require(page in 1..1000 && query.length<=100)
  require(orientation in setOf("","portrait","landscape","square"))
  val base="https://api.pexels.com/v1/"
  return if(query.isBlank()) "${base}curated?page=$page&per_page=12"
   else "${base}search?query=${URLEncoder.encode(query.trim(),"UTF-8")}&page=$page&per_page=12"+
    if(orientation.isEmpty()) "" else "&orientation=$orientation"
 }
 private fun trusted(address: String,host: String): URL {
  val url=URL(address)
  check(url.protocol=="https" && url.host==host && url.userInfo==null && url.port in listOf(-1,443)) {"Unexpected Pexels image or attribution address."}
  return url
 }
 fun parse(json: String): PhotoPage {
  val root=JSONObject(json);val photos=root.getJSONArray("photos")
  check(photos.length()<=80) {"Unexpected Pexels response size."}
  val items=List(photos.length()) {i ->
   val photo=photos.getJSONObject(i);val id=photo.getLong("id")
   check(id>0) {"Invalid Pexels image identifier."}
   val width=photo.getInt("width");val height=photo.getInt("height")
   check(width in 1..50000 && height in 1..50000) {"Invalid Pexels image dimensions."}
   val src=photo.getJSONObject("src")
   val original=trusted(src.getString("original"),"images.pexels.com")
   check(original.path.startsWith("/photos/")) {"Unexpected Pexels image path."}
   val ratio=minOf(1f,2400f/maxOf(width,height))
   val w=(width*ratio).roundToInt().coerceAtLeast(1);val h=(height*ratio).roundToInt().coerceAtLeast(1)
   val image=original.toExternalForm().substringBefore('?').substringBefore('#')+"?auto=compress&cs=tinysrgb&fit=max&w=$w&h=$h"
   val thumbnail=trusted(src.getString("medium"),"images.pexels.com").toExternalForm()
   val source=trusted(photo.getString("url"),"www.pexels.com").toExternalForm()
   val photographer=photo.optString("photographer_url").takeIf {it.isNotBlank()}?.let {trusted(it,"www.pexels.com").toExternalForm()} ?: source
   OnlineWallpaper("pexels:$id",photo.optString("photographer","Unknown photographer").take(120),width,height,source,
    provider=WallpaperProvider.PEXELS,previewUrl=thumbnail,downloadUrl=image,
    description=photo.optString("alt").take(300),photographerUrl=photographer)
  }.distinctBy {it.id}
  // Only consume pagination as a boolean. Never follow a server-provided URL
  // with the Authorization header attached.
  return PhotoPage(items,root.optString("next_page").startsWith("https://api.pexels.com/"))
 }
 suspend fun list(key: String,query: String,page: Int,orientation: String): PhotoPage=withContext(Dispatchers.IO) {
  require(validKey(key)) {"Add a valid Pexels API key in Wallpapers first."}
  currentCoroutineContext().ensureActive()
  val c=URL(requestUrl(query,page,orientation)).openConnection() as HttpURLConnection
  try {
   c.connectTimeout=15_000;c.readTimeout=20_000;c.instanceFollowRedirects=false
   c.setRequestProperty("Authorization",key)
   c.setRequestProperty("User-Agent","KotobaWall/1.5")
   c.setRequestProperty("Accept","application/json")
   when(val code=c.responseCode) {
    200 -> Unit
    401,403 -> throw IOException("Pexels rejected the API key. Check or replace it in API key settings.")
    429 -> throw IOException("Pexels request limit reached. Try later or switch to Unsplash / Picsum. Saved backgrounds still work.")
    else -> throw IOException("Pexels returned HTTP $code. Try again later or switch sources.")
   }
   val bytes=ByteArrayOutputStream()
   c.inputStream.use {input ->
    val buffer=ByteArray(8192)
    while(true) {
     currentCoroutineContext().ensureActive()
     val n=input.read(buffer);if(n<0) break
     check(bytes.size()+n<=1024*1024) {"Pexels response is too large."}
     bytes.write(buffer,0,n)
    }
   }
   parse(String(bytes.toByteArray(),Charsets.UTF_8))
  } finally {c.disconnect()}
 }
}
