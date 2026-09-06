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
import java.security.MessageDigest

object JlptClient {
 const val HOME="https://jlpt-vocab-api.vercel.app/"
 fun parse(raw: String,level: Int): List<Word> {
  require(level in 1..5)
  val data=JSONArray(raw)
  check(data.length() in 1..12000) {"The vocabulary service returned an empty or oversized list."}
  val result=List(data.length()) {i ->
   val j=data.getJSONObject(i)
   val written=j.optString("word").trim()
   val reading=j.optString("furigana").ifBlank {j.optString("hiragana")}.trim()
   val meaning=j.optString("meaning").trim()
   check(j.optInt("level",-1)==level && written.isNotBlank() && meaning.isNotBlank()) {"Unexpected vocabulary format. Cached words were kept."}
   check(written.length<=200 && reading.length<=300 && meaning.length<=4000) {"Vocabulary entry too large."}
   val kana=reading.ifBlank {written}
   val identity="$level\u0000$written\u0000$kana"
   val id=MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it.toInt() and 255)}
   Word("jlpt:$id",written,kana,meaning,"JLPT N$level",level)
  }
  return result.distinctBy {it.id}
 }
 suspend fun fetch(level: Int): List<Word> = withContext(Dispatchers.IO) {
  require(level in 1..5)
  val connection=URL("${HOME}api/words/all?level=$level").openConnection() as HttpURLConnection
  try {
   connection.connectTimeout=15_000;connection.readTimeout=20_000
   connection.instanceFollowRedirects=false
   connection.setRequestProperty("Accept","application/json")
   connection.setRequestProperty("User-Agent","KotobaWall/1.3")
   val status=connection.responseCode
   if(status!=200) throw IOException(if(status==429) "Vocabulary service is busy. Retry later; cached words still work." else "Vocabulary service returned HTTP $status. Retry later.")
   val bytes=ByteArrayOutputStream()
   connection.inputStream.use {input ->
    val buffer=ByteArray(8192)
    while(true) {
     currentCoroutineContext().ensureActive()
     val n=input.read(buffer);if(n<0) break
     check(bytes.size()+n<=6*1024*1024) {"Vocabulary download exceeded the size limit."}
     bytes.write(buffer,0,n)
    }
   }
   parse(bytes.toString("UTF-8"),level)
  } finally {connection.disconnect()}
 }
}
