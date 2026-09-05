package com.kotobawall.app

import kotlin.math.max

data class Word(val id: String, val written: String, val reading: String, val meaning: String, val category: String)
data class WallSettings(
 val wordIndex: Int = 0, val background: String = "Ocean", val photo: String = "",
 val showReading: Boolean = true, val showMeaning: Boolean = true,
 val scale: Float = 1f, val position: Float = 0.65f, val panel: Float = 0.4f,
 val cropX: Float = 0.5f, val cropY: Float = 0.5f,
 val hours: Int = 0, val lastApplied: Long = 0L, val lastError: String = ""
)
object WallMath {
 fun nextIndex(current: Int, count: Int): Int {
  require(count > 0)
  return (current.coerceIn(0, count - 1) + 1) % count
 }
 fun crop(sw: Int, sh: Int, ow: Int, oh: Int, x: Float, y: Float): FloatArray {
  require(sw > 0 && sh > 0 && ow > 0 && oh > 0)
  val scale = max(ow.toFloat() / sw, oh.toFloat() / sh)
  val w = ow / scale; val h = oh / scale
  return floatArrayOf((sw - w) * x.coerceIn(0f,1f), (sh - h) * y.coerceIn(0f,1f), w, h)
 }
 fun top(height: Int, block: Float, position: Float, margin: Float): Float =
  margin + (height - block - margin * 2).coerceAtLeast(0f) * position.coerceIn(0f,1f)
}
