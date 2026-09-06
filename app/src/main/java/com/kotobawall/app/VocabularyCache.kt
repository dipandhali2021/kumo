package com.kotobawall.app
import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class VocabularyCache(context: Context) {
 private val directory=File(context.filesDir,"vocabulary").apply {mkdirs()}
 private fun file(level: Int)=AtomicFile(File(directory,"n$level.json"))
 fun load(level: Int): List<Word> = try {
  val data=file(level).readFully()
  if(data.size>6*1024*1024) emptyList() else JlptClient.parse(String(data,Charsets.UTF_8),level)
 } catch(_: Exception) {emptyList()}
 fun save(level: Int,words: List<Word>) {
  val json=JSONArray().apply {words.forEach {w ->put(JSONObject().apply {
   put("word",w.written);put("furigana",w.reading);put("meaning",w.meaning);put("level",w.level)
  })} }.toString().toByteArray(Charsets.UTF_8)
  check(json.size<=6*1024*1024) {"Vocabulary cache too large."}
  val destination=file(level);val output=destination.startWrite()
  try {output.write(json);destination.finishWrite(output)}
  catch(e: Exception) {destination.failWrite(output);throw e}
 }
 fun updatedAt(level: Int): Long=File(directory,"n$level.json").lastModified()
}
