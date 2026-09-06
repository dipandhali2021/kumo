package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test
class JlptClientTest {
 @Test fun parsesFuriganaAndStableIds() {
  val a=JlptClient.parse("""[{"word":"毎朝","furigana":"まいあさ","meaning":"every morning","level":5}]""",5).single()
  val b=JlptClient.parse("""[{"word":"毎朝","hiragana":"まいあさ","meaning":"Each morning","level":5}]""",5).single()
  assertEquals("まいあさ",a.reading);assertEquals(a.id,b.id);assertEquals(5,a.level)
 }
 @Test fun blankKanaRemainsStableAfterCacheRoundTrip() {
  val a=JlptClient.parse("""[{"word":"はい","meaning":"yes","level":5}]""",5).single()
  val b=JlptClient.parse("""[{"word":"はい","furigana":"はい","meaning":"yes","level":5}]""",5).single()
  assertEquals(a.id,b.id)
 }
 @Test(expected=IllegalStateException::class) fun rejectsWrongLevel() {
  JlptClient.parse("""[{"word":"毎朝","meaning":"every morning","level":1}]""",5)
 }
 @Test(expected=IllegalStateException::class) fun rejectsEmptyResponse() {JlptClient.parse("[]",5)}
}
