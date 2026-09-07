package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test
class JlptClientTest {
 @Test fun parsesFuriganaAndStableIds() {
  val a=JlptClient.parse("""[{"word":"毎朝","furigana":"まいあさ","meaning":"every morning","level":5}]""",5).single()
  val b=JlptClient.parse("""[{"word":"毎朝","hiragana":"まいあさ","meaning":"Each morning","level":5}]""",5).single()
  assertEquals("まいあさ",a.reading);assertEquals(a.id,b.id);assertEquals(5,a.level)
 }
 @Test fun keepsServiceRomajiInsteadOfTransliterating() {
  val word=JlptClient.parse("""[{"word":"学校","furigana":"がっこう","romaji":"gakkō","meaning":"school","level":5}]""",5).single()
  assertEquals("gakkō",word.romaji)
  assertEquals("gakkō",Romaji.display(word))
  assertEquals("gakkou",Romaji.of(word.reading))
 }
 @Test fun transliteratesOnlyWhenRomajiIsMissing() {
  val word=JlptClient.parse("""[{"word":"毎朝","furigana":"まいあさ","meaning":"every morning","level":5}]""",5).single()
  assertEquals("",word.romaji);assertEquals("maiasa",Romaji.display(word))
 }
 @Test fun acceptsPaginatedEnvelope() {
  val word=JlptClient.parse("""{"total":8385,"offset":0,"limit":10,"words":[{"word":"毎朝","meaning":"every morning","furigana":"まいあさ","romaji":"maiasa","level":5}]}""",5).single()
  assertEquals("maiasa",word.romaji);assertEquals("まいあさ",word.reading)
 }
 @Test fun cachedRomajiSurvivesARoundTrip() {
  val original=JlptClient.parse("""[{"word":"学校","furigana":"がっこう","romaji":"gakkō","meaning":"school","level":5}]""",5).single()
  val cached=JlptClient.parse("""[{"word":"${original.written}","furigana":"${original.reading}","romaji":"${original.romaji}","meaning":"${original.meaning}","level":${original.level}}]""",5).single()
  assertEquals(original,cached)
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
