package com.kotobawall.app

import android.content.Context
import android.graphics.*
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextDirectionHeuristics
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.Locale
import kotlin.math.max

/**
 * Where the text block actually lands inside a rendered wallpaper, in canvas pixels.
 * `left`/`width` describe the text column, which is narrower than the dark panel.
 */
data class TextBounds(val left: Float,val top: Float,val width: Float,val height: Float)

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
 /** One measured text block: the laid-out lines plus every number drawText needs to place them. */
 private class Block(val lines: List<StaticLayout>,val unit: Float,val margin: Float,val padding: Float,
  val gap: Float,val fit: Float,val total: Float,val top: Float,val textWidth: Int)
 /**
  * Measures the block once. drawText and textBounds both go through here, so a preview can never
  * disagree with what is actually drawn.
  */
 private fun layout(s: WallSettings,word: Word,width: Int,height: Int): Block? {
  val unit=width/360f;val margin=22f*unit;val padding=20f*unit
  val textWidth=(width-2*(margin+padding)).toInt().coerceAtLeast(1)
  val t=s.typography
  val resolved=t.rows.take(t.lineCount).map { it to t.text(it,word) }.filter { it.second.isNotBlank() }
  if(resolved.isEmpty()) return null
  val lines=resolved.map { (row,text) ->
   val p=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize=row.size.coerceIn(12f,60f)*unit*s.scale.coerceIn(0.75f,1.4f)
    color=try {Color.parseColor(row.color)} catch(_: IllegalArgumentException) {Color.WHITE}
    typeface=JapaneseFonts.get(context,row.font,row.bold)
    textLocale=Locale.JAPANESE
    setShadowLayer(2f*unit,0f,unit,Color.BLACK)
   }
   val measured=p.measureText(text)
   if(measured>textWidth) p.textSize=(p.textSize*textWidth/measured).coerceAtLeast(12f*unit)
   val alignment=when(if(row.alignment=="Default") t.alignment else row.alignment) {
    "Left"->Layout.Alignment.ALIGN_NORMAL;"Right"->Layout.Alignment.ALIGN_OPPOSITE;else->Layout.Alignment.ALIGN_CENTER
   }
   StaticLayout.Builder.obtain(text,0,text.length,p,textWidth)
    .setAlignment(alignment).setTextDirection(TextDirectionHeuristics.LTR).setIncludePad(true)
    .setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).setEllipsizedWidth(textWidth)
    .setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE).build()
  }
  val gap=t.spacing.coerceIn(0f,24f)*unit
  val natural=lines.sumOf {it.height}.toFloat()+gap*(lines.size-1)+padding*2
  val fit=minOf(1f,(height-2*margin).coerceAtLeast(1f)/natural)
  val total=natural*fit
  return Block(lines,unit,margin,padding,gap,fit,total,WallMath.top(height,total,s.position,margin),textWidth)
 }
 fun drawText(canvas: Canvas,s: WallSettings,word: Word,width: Int,height: Int) {
  val b=layout(s,word,width,height) ?: return
  val panel=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.argb((s.panel.coerceIn(0f,0.8f)*255).toInt(),0,0,0)}
  canvas.drawRoundRect(RectF(b.margin,b.top,width-b.margin,b.top+b.total),18f*b.unit,18f*b.unit,panel)
  canvas.save()
  canvas.translate((width-width*b.fit)/2,b.top)
  canvas.scale(b.fit,b.fit)
  var y=b.padding
  b.lines.forEach {layout ->
   canvas.save();canvas.translate(b.margin+b.padding,y);layout.draw(canvas);canvas.restore()
   y+=layout.height+b.gap
  }
  canvas.restore()
 }
 /**
  * Exact geometry of the block drawText would draw at this canvas size, or null when every line is
  * empty. Previews use this to frame the text instead of re-deriving the renderer's maths.
  */
 fun textBounds(s: WallSettings,word: Word,width: Int,height: Int): TextBounds? {
  val b=layout(s,word,width,height) ?: return null
  return TextBounds((width-width*b.fit)/2+(b.margin+b.padding)*b.fit,b.top,b.textWidth*b.fit,b.total)
 }
}
