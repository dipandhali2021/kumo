package com.kotobawall.app

import android.content.Context
import android.graphics.*
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.Locale
import kotlin.math.max

class WallpaperRenderer(private val context: Context) {
 companion object {
  val palettes = linkedMapOf(
   "Ocean" to intArrayOf(Color.rgb(25,58,89), Color.rgb(9,21,39)),
   "Moss" to intArrayOf(Color.rgb(55,77,66), Color.rgb(18,35,31)),
   "Clay" to intArrayOf(Color.rgb(110,65,55), Color.rgb(48,29,31)),
   "Ink" to intArrayOf(Color.rgb(51,56,72), Color.rgb(15,17,24))
  )
 }
 fun render(s: WallSettings, word: Word, width: Int, height: Int, includeText: Boolean = true): Bitmap {
  require(width in 1..2160 && height in 1..4800)
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  try {
   val canvas = Canvas(bitmap)
   val paint = Paint().apply {
    shader = LinearGradient(0f,0f,width.toFloat(),height.toFloat(),
     palettes[s.background] ?: palettes.getValue("Ocean"),null,Shader.TileMode.CLAMP)
   }
   canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
   if (s.photo.isNotEmpty()) {
    val photo = decode(File(context.filesDir,s.photo),max(width,height))
    try {
     val r = WallMath.crop(photo.width,photo.height,width,height,s.cropX,s.cropY)
     val matrix = Matrix().apply {
      setRectToRect(RectF(r[0],r[1],r[0]+r[2],r[1]+r[3]),
       RectF(0f,0f,width.toFloat(),height.toFloat()),Matrix.ScaleToFit.FILL)
     }
     canvas.drawBitmap(photo,matrix,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    } finally { photo.recycle() }
   }
   if (includeText) drawText(canvas,s,word,width,height)
   return bitmap
  } catch(e: Exception) { bitmap.recycle(); throw e }
 }
 private fun decode(file: File, target: Int): Bitmap {
  check(file.isFile) { "Saved photo is missing. Select it again." }
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeFile(file.path,bounds)
  check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image. Try a JPEG or PNG." }
  var sample = 1
  while (max(bounds.outWidth,bounds.outHeight)/sample > target*2 ||
   (bounds.outWidth.toLong()/sample)*(bounds.outHeight/sample) > 4_000_000) sample *= 2
  val raw = BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply {
   inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888
  }) ?: error("Could not decode the image.")
  val orientation = try { ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION,1) } catch(_: Exception) { 1 }
  val m = Matrix()
  when(orientation) {
   2 -> m.setScale(-1f,1f)
   3 -> m.setRotate(180f)
   4 -> m.setScale(1f,-1f)
   5 -> { m.setRotate(90f); m.postScale(-1f,1f) }
   6 -> m.setRotate(90f)
   7 -> { m.setRotate(-90f); m.postScale(-1f,1f) }
   8 -> m.setRotate(-90f)
  }
  if (m.isIdentity) return raw
  return try {
   Bitmap.createBitmap(raw,0,0,raw.width,raw.height,m,true).also { if(it !== raw) raw.recycle() }
  } catch(e: Exception) { raw.recycle(); throw e }
 }
 fun drawText(canvas: Canvas,s: WallSettings,word: Word,width: Int,height: Int) {
  val unit = width/360f
  val margin = 22f*unit; val padding = 20f*unit
  val textWidth = (width-2*(margin+padding)).toInt().coerceAtLeast(1)
  fun line(text: String,size: Float,color: Int,bold: Boolean=false): StaticLayout {
   val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size*unit*s.scale.coerceIn(0.75f,1.4f)
    this.color = color
    typeface = Typeface.create("sans-serif",if(bold) Typeface.BOLD else Typeface.NORMAL)
    textLocale = Locale.JAPANESE
    setShadowLayer(2f*unit,0f,unit,Color.BLACK)
   }
   return StaticLayout.Builder.obtain(text,0,text.length,p,textWidth)
    .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(true)
    .setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE).build()
  }
  val lines = mutableListOf(line(word.written,40f,Color.WHITE,true))
  if(s.showReading && word.reading != word.written) lines += line(word.reading,21f,Color.rgb(223,234,248))
  if(s.showMeaning) lines += line(word.meaning,18f,Color.WHITE)
  val gap = 8f*unit
  val total = lines.sumOf { it.height }.toFloat()+gap*(lines.size-1)+padding*2
  val top = WallMath.top(height,total,s.position,margin)
  val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((s.panel.coerceIn(0f,0.8f)*255).toInt(),0,0,0) }
  canvas.drawRoundRect(RectF(margin,top,width-margin,top+total),18f*unit,18f*unit,panel)
  var y = top+padding
  lines.forEach {
   canvas.save(); canvas.translate(margin+padding,y); it.draw(canvas); canvas.restore()
   y += it.height+gap
  }
 }
}
