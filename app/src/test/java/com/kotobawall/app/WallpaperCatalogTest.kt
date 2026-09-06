package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test
class WallpaperCatalogTest {
 @Test fun photoMetadataBuildsBoundedUrl() {
  val item=WallpaperCatalog.parse("""[{"id":"1","author":"A","width":6000,"height":4000,"url":"https://unsplash.com/photos/test"}]""").single()
  assertEquals("https://picsum.photos/id/1/2400/1600.jpg",item.imageUrl)
 }
 @Test(expected=IllegalStateException::class) fun rejectsPathInjection() {
  WallpaperCatalog.parse("""[{"id":"../private","author":"A","width":6000,"height":4000}]""")
 }
 @Test fun attributionIsLimitedToKnownSource() {
  val item=WallpaperCatalog.parse("""[{"id":"1","width":600,"height":400,"url":"javascript:bad"}]""").single()
  assertEquals("https://picsum.photos/images",item.sourceUrl)
 }
 @Test fun rotatesAndWrapsSavedCollection() {
  assertEquals("b",WallpaperPolicy.next(listOf("a","b"),"a"))
  assertEquals("a",WallpaperPolicy.next(listOf("a","b"),"b"))
  assertEquals("a",WallpaperPolicy.next(listOf("a","b"),"unknown"))
 }
 @Test fun emptyCollectionHasNoFallback() {assertNull(WallpaperPolicy.next(emptyList(),"a"))}
 @Test fun staticIsDefault() {assertFalse(WallSettings().rotateWallpaper)}
}
