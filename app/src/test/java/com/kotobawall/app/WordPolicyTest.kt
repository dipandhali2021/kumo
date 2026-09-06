package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test
class WordPolicyTest {
 private val words=listOf(Word("s","猫","ねこ","Cat","Starter"),Word("5","毎朝","まいあさ","Every morning","N5",5),Word("1","語彙","ごい","Vocabulary","N1",1))
 @Test fun levelSelectionFiltersRotation() {
  val s=WallSettings(levels=setOf(1),includeStarter=false)
  assertEquals(2,WordPolicy.choose(words,s,false));assertEquals(2,WordPolicy.choose(words,s,true))
 }
 @Test fun mixedLevelsRotateOnlyEligibleWords() {
  val s=WallSettings(levels=setOf(1,5),includeStarter=false,wordIndex=1)
  assertEquals(2,WordPolicy.choose(words,s,true));assertEquals(1,WordPolicy.choose(words,s.copy(wordIndex=2),true))
 }
 @Test fun emptyFavoritesNeverFallBackToOtherLevels() {
  assertNull(WordPolicy.choose(words,WallSettings(favoritesOnly=true),true))
 }
 @Test fun favoritesAreScopedToSelectedLevels() {
  val s=WallSettings(levels=setOf(5),includeStarter=false,favorites=setOf("1"),favoritesOnly=true)
  assertNull(WordPolicy.choose(words,s,true))
 }
 @Test fun shuffleAvoidsImmediateRepeat() {
  val s=WallSettings(levels=setOf(1,5),includeStarter=false,shuffle=true,wordIndex=1)
  repeat(20) {assertEquals(2,WordPolicy.choose(words,s,true))}
 }
 @Test fun panelAndFontsHaveNewDefaults() {
  assertEquals(0f,WallSettings().panel,0f)
  assertEquals("Gothic JP",TextRow().font)
  assertEquals("Mincho JP",Typography.migrateFont("Serif"))
 }
}
