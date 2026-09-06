package com.kotobawall.app

import org.junit.Assert.*
import org.junit.Test

class PexelsClientTest {
 private val photo="""{"id":42,"width":6000,"height":4000,"photographer":"Test photographer","photographer_url":"https://www.pexels.com/@example/","url":"https://www.pexels.com/photo/example-42/","alt":"A test scene","src":{"original":"https://images.pexels.com/photos/42/example.jpeg","medium":"https://images.pexels.com/photos/42/example.jpeg?w=350"}}"""
 private fun response(value: String=photo)="""{"photos":[$value],"next_page":"https://api.pexels.com/v1/curated?page=2"}"""
 @Test fun defaultsToPexels() {assertEquals(WallpaperProvider.PEXELS,WallpaperBrowseState().provider)}
 @Test fun featuredDoesNotSendUnsupportedOrientation() {
  assertEquals("https://api.pexels.com/v1/curated?page=1&per_page=12",PexelsClient.requestUrl("",1,"portrait"))
 }
 @Test fun searchEncodesTextAndAddsShape() {
  val url=PexelsClient.requestUrl("night city & stars",2,"landscape")
  assertTrue(url.contains("query=night+city+%26+stars"));assertTrue(url.endsWith("&orientation=landscape"))
 }
 @Test fun anyShapeOmitsOrientation() {assertFalse(PexelsClient.requestUrl("ocean",1,"").contains("orientation="))}
 @Test(expected=IllegalArgumentException::class) fun rejectsUnknownShape() {PexelsClient.requestUrl("ocean",1,"invalid")}
 @Test(expected=IllegalArgumentException::class) fun rejectsInvalidPage() {PexelsClient.requestUrl("",0,"")}
 @Test fun parsesPhotoAndBoundsDownloadSize() {
  val page=PexelsClient.parse(response());val item=page.items.single()
  assertEquals("pexels:42",item.id);assertEquals(WallpaperProvider.PEXELS,item.provider)
  assertTrue(item.imageUrl.endsWith("&w=2400&h=1600"));assertTrue(page.hasNext)
  assertEquals("Test photographer",item.author);assertEquals("A test scene",item.description)
  assertEquals("https://www.pexels.com/@example/",item.photographerUrl)
 }
 @Test(expected=IllegalStateException::class) fun rejectsUntrustedImageHost() {
  PexelsClient.parse(response(photo.replace("images.pexels.com","images.pexels.com.attacker.invalid")))
 }
 @Test(expected=IllegalStateException::class) fun rejectsInsecureImage() {
  PexelsClient.parse(response(photo.replace("https://images.pexels.com","http://images.pexels.com")))
 }
 @Test(expected=IllegalStateException::class) fun rejectsUntrustedAttribution() {
  PexelsClient.parse(response(photo.replace("www.pexels.com","attacker.invalid")))
 }
 @Test(expected=IllegalStateException::class) fun rejectsInvalidDimensions() {
  PexelsClient.parse(response(photo.replace("6000","0")))
 }
 @Test fun emptyResultsAndNoNextPage() {
  val page=PexelsClient.parse("""{"photos":[]}""");assertTrue(page.items.isEmpty());assertFalse(page.hasNext)
 }
 @Test fun rejectsHeaderInjectionAndAllowsTestKeyShape() {
  assertTrue(PexelsClient.validKey("a".repeat(56)))
  assertFalse(PexelsClient.validKey("a".repeat(56)+"\r\nX-Test: injected"))
  assertFalse(PexelsClient.validKey(""));assertFalse(PexelsClient.validKey("a".repeat(257)))
 }
}
