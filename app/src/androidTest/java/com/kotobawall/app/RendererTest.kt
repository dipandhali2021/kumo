package com.kotobawall.app
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RendererTest {
 @Test fun bundledJapaneseFontsRenderDifferently() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val renderer=WallpaperRenderer(context)
  val word=Word("eat","食べる","たべる","To eat","Verbs")
  val a=renderer.render(WallSettings(typography=Typography().copy(rows=listOf(TextRow(font="Gothic JP")))),word,360,800)
  val b=renderer.render(WallSettings(typography=Typography().copy(rows=listOf(TextRow(font="Mincho JP")))),word,360,800)
  try {assertFalse("Bundled Japanese families must produce different glyphs",a.sameAs(b))}
  finally {a.recycle();b.recycle()}
 }
 @Test fun previewOverlayMatchesExport() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val renderer=WallpaperRenderer(context)
  val word=Word("eat","食べる","たべる","To eat","Verbs")
  val settings=WallSettings(position=0.3f,scale=1.2f,panel=0.5f)
  val preview=renderer.render(settings,word,360,800,includeText=false)
  val exported=renderer.render(settings,word,360,800)
  try {
   renderer.drawText(Canvas(preview),settings,word,360,800)
   assertTrue("Live overlay must match flattened export",preview.sameAs(exported))
  } finally {preview.recycle();exported.recycle()}
 }
 @Test fun rendersEveryStarterEntry() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val repo=(context.applicationContext as KotobaApplication).repository
  val renderer=WallpaperRenderer(context)
  repo.words.value.forEach { word ->
   listOf(0f,0.65f,1f).forEach { position ->
    val bitmap=renderer.render(WallSettings(scale=1.4f,position=position),word,360,800)
    assertEquals(360,bitmap.width); assertEquals(800,bitmap.height)
    assertFalse(bitmap.isRecycled); bitmap.recycle()
   }
  }
 }
}
