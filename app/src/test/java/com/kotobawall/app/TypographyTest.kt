package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test

class TypographyTest {
 private val word=Word("eat","食べる","たべる","To eat","Verbs")
 @Test fun resolvesAllTokens() {
  assertEquals("食べる · たべる · taberu — To eat",Typography().text(TextRow("{word} · {reading} · {romaji} — {meaning}"),word))
 }
 @Test fun keepsCustomText() {assertEquals("Learn today",Typography().text(TextRow("Learn today"),word))}
 @Test fun canPlaceMeaningFirst() {
  val t=Typography().withRow(0,TextRow("{meaning}"))
  assertEquals("To eat",t.text(t.rows[0],word))
 }
 @Test fun defaultLayoutShowsWordKanaRomajiAndMeaning() {
  val t=Typography()
  assertEquals(4,t.lineCount);assertEquals(4,t.rows.size)
  assertEquals(listOf("{word}","{reading}","{romaji}","{meaning}"),t.rows.map {it.template})
 }
 @Test fun shorterLayoutsPreserveHiddenRows() {
  val t=Typography().copy(lineCount=2)
  assertEquals(2,t.rows.take(t.lineCount).size);assertEquals(4,t.rows.size)
 }
 @Test fun romajiUsesCuratedSpellingWhenPresent() {
  val curated=Word("school","学校","がっこう","School","Everyday",0,"gakkō")
  assertEquals("gakkō",Typography().text(TextRow("{romaji}"),curated))
 }
 @Test fun romajiStaysForKatakanaEvenWhenKanaLineIsHidden() {
  val w=Word("coffee","コーヒー","コーヒー","Coffee","Katakana")
  assertEquals("",Typography().text(TextRow("{reading}"),w))
  assertEquals("kōhī",Typography().text(TextRow("{romaji}"),w))
 }
 @Test fun repeatedReadingOptional() {
  val w=Word("coffee","コーヒー","コーヒー","Coffee","Katakana")
  assertEquals("",Typography().text(TextRow("{reading}"),w))
  assertEquals("コーヒー",Typography(hideRepeatedReading=false).text(TextRow("{reading}"),w))
 }
 @Test fun legacyThreeLineLayoutKeepsItsAppearance() {
  val legacy="{\"lineCount\":3,\"alignment\":\"Center\",\"spacing\":8,\"hideRepeatedReading\":true,\"rows\":[{\"template\":\"{word}\"},{\"template\":\"{reading}\"},{\"template\":\"{meaning}\"}]}"
  val t=TypographyCodec.decode(legacy)
  assertEquals(3,t.lineCount);assertEquals(4,t.rows.size)
  assertEquals(listOf("{word}","{reading}","{meaning}"),t.rows.take(t.lineCount).map {it.template})
 }
 @Test fun freshInstallDecodesToFourLines() {assertEquals(4,TypographyCodec.decode(null).lineCount)}
 @Test fun encodeDecodeKeepsFourLines() {
  val t=Typography().copy(lineCount=4,rows=Typography().rows.mapIndexed {i,r ->if(i==2) r.copy(size=17f) else r})
  val decoded=TypographyCodec.decode(TypographyCodec.encode(t))
  assertEquals("{romaji}",decoded.rows[2].template);assertEquals(17f,decoded.rows[2].size,0.001f)
 }
 @Test fun middleIsNewDefault() {assertEquals(0.5f,WallSettings().position,0.001f)}
}
