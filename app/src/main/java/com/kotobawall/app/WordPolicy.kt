package com.kotobawall.app

object WordPolicy {
 fun eligible(word: Word,s: WallSettings): Boolean =
  (if(word.level==0) s.includeStarter else word.level in s.levels) && (!s.favoritesOnly || word.id in s.favorites)
 fun choose(words: List<Word>,s: WallSettings,advance: Boolean): Int? {
  val indices=words.indices.filter {eligible(words[it],s)}
  if(indices.isEmpty()) return null
  if(!advance && s.wordIndex in indices) return s.wordIndex
  if(!advance) return indices.first()
  if(s.shuffle) return indices.filter {it!=s.wordIndex}.ifEmpty {indices}.random()
  val current=indices.indexOf(s.wordIndex)
  return indices[(current+1)%indices.size]
 }
}
