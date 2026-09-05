package com.kotobawall.app

data class TextRow(
 val template: String = "{word}", val font: String = "Sans", val size: Float = 40f,
 val bold: Boolean = false, val color: String = "#FFFFFF", val alignment: String = "Default"
)
data class Typography(
 val lineCount: Int = 3, val alignment: String = "Center", val spacing: Float = 8f,
 val hideRepeatedReading: Boolean = true,
 val rows: List<TextRow> = listOf(TextRow(bold=true),TextRow("{reading}",size=21f),TextRow("{meaning}",size=18f))
) {
 fun withRow(index: Int,row: TextRow): Typography = copy(rows=rows.mapIndexed { i,old -> if(i==index) row else old })
 fun text(row: TextRow,word: Word): String {
  if(hideRepeatedReading && row.template.trim()=="{reading}" && word.reading==word.written) return ""
  // Replace tokens in one pass; custom text or dictionary values are never evaluated as code.
  return Regex("\\{(word|reading|meaning)\\}").replace(row.template) { match ->
   when(match.groupValues[1]) { "word"->word.written; "reading"->word.reading; else->word.meaning }
  }.replace('\n',' ').replace('\r',' ').trim()
 }
 companion object {
  val fonts = listOf("Sans","Serif","Monospace","Rounded")
  val alignments = listOf("Left","Center","Right")
  val presets = linkedMapOf(
   "Japanese word" to "{word}", "Kana reading" to "{reading}", "English meaning" to "{meaning}",
   "Word · reading" to "{word} · {reading}", "Word · meaning" to "{word} · {meaning}",
   "Reading · meaning" to "{reading} · {meaning}", "All three" to "{word} · {reading} · {meaning}", "Empty line" to ""
  )
 }
}
