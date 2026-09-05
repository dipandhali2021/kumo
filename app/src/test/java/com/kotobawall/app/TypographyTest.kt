package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test

class TypographyTest {
 private val word=Word("eat","食べる","たべる","To eat","Verbs")
 @Test fun resolvesAllTokens() {
  assertEquals("食べる · たべる — To eat",Typography().text(TextRow("{word} · {reading} — {meaning}"),word))
 }
 @Test fun keepsCustomText() {assertEquals("Learn today",Typography().text(TextRow("Learn today"),word))}
 @Test fun canPlaceMeaningFirst() {
  val t=Typography().withRow(0,TextRow("{meaning}"))
  assertEquals("To eat",t.text(t.rows[0],word))
 }
 @Test fun twoLineModePreservesThirdRow() {
  val t=Typography().copy(lineCount=2)
  assertEquals(2,t.rows.take(t.lineCount).size);assertEquals(3,t.rows.size)
 }
 @Test fun repeatedReadingOptional() {
  val w=Word("coffee","コーヒー","コーヒー","Coffee","Katakana")
  assertEquals("",Typography().text(TextRow("{reading}"),w))
  assertEquals("コーヒー",Typography(hideRepeatedReading=false).text(TextRow("{reading}"),w))
 }
 @Test fun middleIsNewDefault() {assertEquals(0.5f,WallSettings().position,0.001f)}
}
