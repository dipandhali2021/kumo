package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test

class RomajiTest {
 @Test fun readsHiragana() {
  assertEquals("taberu",Romaji.of("たべる"))
  assertEquals("mizu",Romaji.of("みず"))
 }
 @Test fun katakanaLongMarksBecomeMacrons() {
  assertEquals("kōhī",Romaji.of("コーヒー"))
  assertEquals("aisukurīmu",Romaji.of("アイスクリーム"))
  assertEquals("pasokon",Romaji.of("パソコン"))
 }
 @Test fun smallTsuDoublesTheNextConsonant() {
  assertEquals("gakkou",Romaji.of("がっこう"))
  assertEquals("matchi",Romaji.of("マッチ"))
  assertEquals("issho",Romaji.of("いっしょ"))
 }
 @Test fun handlesDigraphs() {
  assertEquals("densha",Romaji.of("でんしゃ"))
  assertEquals("ryokou",Romaji.of("りょこう"))
  assertEquals("ocha",Romaji.of("おちゃ"))
 }
 @Test fun separatesSyllabicN() {
  assertEquals("kin'en",Romaji.of("きんえん"))
  assertEquals("shinbun",Romaji.of("しんぶん"))
 }
 @Test fun unknownCharactersPassThrough() {
  assertEquals("Wi-Fi",Romaji.of("Wi-Fi"))
  assertEquals("食beru",Romaji.of("食べる"))
 }
 @Test fun blankStaysBlank() {assertEquals("",Romaji.of("   "))}
 @Test fun curatedRomajiWins() {
  assertEquals("konnichiwa",Romaji.display(Word("hello","こんにちは","こんにちは","Hello","Everyday",0,"konnichiwa")))
 }
 @Test fun derivesWhenCuratedRomajiMissing() {
  assertEquals("neko",Romaji.display(Word("cat","猫","ねこ","Cat","Nature")))
 }
 @Test fun fallsBackToWrittenFormWhenKanaMissing() {
  assertEquals("basu",Romaji.display(Word("bus","バス","","Bus","Katakana")))
 }
}
