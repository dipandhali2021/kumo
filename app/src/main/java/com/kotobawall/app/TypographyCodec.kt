package com.kotobawall.app

import org.json.JSONArray
import org.json.JSONObject

object TypographyCodec {
 fun encode(t: Typography): String = JSONObject().apply {
  put("lineCount",t.lineCount);put("alignment",t.alignment);put("spacing",t.spacing)
  put("hideRepeatedReading",t.hideRepeatedReading)
  put("rows",JSONArray().apply { t.rows.forEach { r -> put(JSONObject().apply {
   put("template",r.template);put("font",r.font);put("size",r.size);put("bold",r.bold);put("color",r.color);put("alignment",r.alignment)
  }) } })
 }.toString()
 // Layouts saved before romaji existed have three rows and lineCount 3. Their templates are
 // restored slot for slot, so the wallpaper keeps its current appearance until the user adds line 4.
 fun decode(raw: String?,reading: Boolean=true,meaning: Boolean=true): Typography {
  val initial=Typography().let { t -> t.copy(rows=t.rows.mapIndexed { i,r ->
   if((i==1 && !reading)||(i==3 && !meaning)) r.copy(template="") else r
  }) }
  if(raw.isNullOrBlank()) return initial
  return try {
   val j=JSONObject(raw);val rows=j.optJSONArray("rows")
   initial.copy(
    lineCount=j.optInt("lineCount",3).coerceIn(2,Typography.MAX_LINES),
    alignment=j.optString("alignment","Center").takeIf {it in Typography.alignments} ?: "Center",
    spacing=j.optDouble("spacing",8.0).toFloat().takeIf {it.isFinite()}?.coerceIn(0f,24f) ?: 8f,
    hideRepeatedReading=j.optBoolean("hideRepeatedReading",true),
    rows=List(Typography.MAX_LINES) { i ->
     val r=rows?.optJSONObject(i);val d=initial.rows[i]
     if(r==null) d else d.copy(
      template=r.optString("template",d.template).take(160),
      font=Typography.migrateFont(r.optString("font",d.font)),
      size=r.optDouble("size",d.size.toDouble()).toFloat().takeIf {it.isFinite()}?.coerceIn(12f,60f) ?: d.size,
      bold=r.optBoolean("bold",d.bold),
      color=r.optString("color",d.color).takeIf {it.matches(Regex("#[0-9a-fA-F]{6}"))} ?: "#FFFFFF",
      alignment=r.optString("alignment","Default").takeIf {it in Typography.alignments || it=="Default"} ?: "Default"
     )
    }
   )
  } catch(_: Exception) {initial}
 }
}
