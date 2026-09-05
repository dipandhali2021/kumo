package com.kotobawall.app
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RendererTest {
 @Test fun rendersEveryStarterEntry() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val repo=(context.applicationContext as KotobaApplication).repository
  val renderer=WallpaperRenderer(context)
  repo.words.forEach { word ->
   listOf(0f,0.65f,1f).forEach { position ->
    val bitmap=renderer.render(WallSettings(scale=1.4f,position=position),word,360,800)
    assertEquals(360,bitmap.width); assertEquals(800,bitmap.height)
    assertFalse(bitmap.isRecycled); bitmap.recycle()
   }
  }
 }
}
