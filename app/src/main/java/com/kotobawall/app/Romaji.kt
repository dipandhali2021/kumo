package com.kotobawall.app

/**
 * Offline kana to romaji transliteration behind the {romaji} line token.
 *
 * Pure Kotlin with no Android dependencies, so it is unit-testable and cheap enough to call
 * while rendering a wallpaper or filtering the word list.
 *
 * Behaviour notes:
 * - Katakana is folded to hiragana first, so a single table covers both scripts.
 * - The katakana long mark becomes a macron: コーヒー -> kōhī.
 * - Kana vowel pairs stay literal: がっこう -> gakkou. Collapsing them to gakkō needs word
 *   boundaries this app does not store, and 思う would wrongly become omō.
 * - Curated romaji from words.json always wins, so こんにちは stays konnichiwa.
 * - Kanji, latin letters and punctuation pass through unchanged.
 */
object Romaji {
 private const val KATAKANA_FIRST=0x30A1
 private const val KATAKANA_LAST=0x30F6
 private const val HIRAGANA_SHIFT=0x60
 private val longMarks=setOf('ー','ｰ','―','—','〜','～')
 private val iterationMarks=setOf('ヽ','ヾ','ゝ','ゞ')
 private val macrons=mapOf('a' to 'ā','i' to 'ī','u' to 'ū','e' to 'ē','o' to 'ō')
 private val vowels=setOf('a','i','u','e','o')
 private val digraphs=mapOf(
  "きゃ" to "kya","きゅ" to "kyu","きょ" to "kyo","きぇ" to "kye",
  "ぎゃ" to "gya","ぎゅ" to "gyu","ぎょ" to "gyo",
  "しゃ" to "sha","しゅ" to "shu","しょ" to "sho","しぇ" to "she",
  "じゃ" to "ja","じゅ" to "ju","じょ" to "jo","じぇ" to "je",
  "ちゃ" to "cha","ちゅ" to "chu","ちょ" to "cho","ちぇ" to "che",
  "ぢゃ" to "ja","ぢゅ" to "ju","ぢょ" to "jo",
  "にゃ" to "nya","にゅ" to "nyu","にょ" to "nyo",
  "ひゃ" to "hya","ひゅ" to "hyu","ひょ" to "hyo",
  "びゃ" to "bya","びゅ" to "byu","びょ" to "byo",
  "ぴゃ" to "pya","ぴゅ" to "pyu","ぴょ" to "pyo",
  "みゃ" to "mya","みゅ" to "myu","みょ" to "myo",
  "りゃ" to "rya","りゅ" to "ryu","りょ" to "ryo",
  "ふぁ" to "fa","ふぃ" to "fi","ふぇ" to "fe","ふぉ" to "fo","ふゅ" to "fyu",
  "てぃ" to "ti","てゅ" to "tyu","でぃ" to "di","でゅ" to "dyu",
  "とぅ" to "tu","どぅ" to "du",
  "うぃ" to "wi","うぇ" to "we","うぉ" to "wo",
  "ゔぁ" to "va","ゔぃ" to "vi","ゔぇ" to "ve","ゔぉ" to "vo",
  "つぁ" to "tsa","つぃ" to "tsi","つぇ" to "tse","つぉ" to "tso",
  "くゎ" to "kwa","ぐゎ" to "gwa"
 )
 private val monographs=mapOf(
  "あ" to "a","い" to "i","う" to "u","え" to "e","お" to "o",
  "か" to "ka","き" to "ki","く" to "ku","け" to "ke","こ" to "ko",
  "が" to "ga","ぎ" to "gi","ぐ" to "gu","げ" to "ge","ご" to "go",
  "さ" to "sa","し" to "shi","す" to "su","せ" to "se","そ" to "so",
  "ざ" to "za","じ" to "ji","ず" to "zu","ぜ" to "ze","ぞ" to "zo",
  "た" to "ta","ち" to "chi","つ" to "tsu","て" to "te","と" to "to",
  "だ" to "da","ぢ" to "ji","づ" to "zu","で" to "de","ど" to "do",
  "な" to "na","に" to "ni","ぬ" to "nu","ね" to "ne","の" to "no",
  "は" to "ha","ひ" to "hi","ふ" to "fu","へ" to "he","ほ" to "ho",
  "ば" to "ba","び" to "bi","ぶ" to "bu","べ" to "be","ぼ" to "bo",
  "ぱ" to "pa","ぴ" to "pi","ぷ" to "pu","ぺ" to "pe","ぽ" to "po",
  "ま" to "ma","み" to "mi","む" to "mu","め" to "me","も" to "mo",
  "や" to "ya","ゆ" to "yu","よ" to "yo",
  "ら" to "ra","り" to "ri","る" to "ru","れ" to "re","ろ" to "ro",
  "わ" to "wa","ゐ" to "i","ゑ" to "e","を" to "o",
  "ゔ" to "vu","ゕ" to "ka","ゖ" to "ke",
  "ぁ" to "a","ぃ" to "i","ぅ" to "u","ぇ" to "e","ぉ" to "o",
  "ゃ" to "ya","ゅ" to "yu","ょ" to "yo","ゎ" to "wa"
 )
 /** Curated romaji when the entry provides one, otherwise a transliteration of its kana. */
 fun display(word: Word): String = word.romaji.trim().ifBlank {of(word.reading.ifBlank {word.written})}
 fun of(kana: String): String {
  if(kana.isBlank()) return ""
  val source=normalize(kana.trim())
  val out=StringBuilder(source.length*2)
  var index=0;var sokuon=false;var nasal=false
  while(index<source.length) {
   val character=source[index]
   if(character in longMarks) {lengthen(out);index++;sokuon=false;nasal=false;continue}
   if(character=='っ') {sokuon=true;index++;continue}
   val pair=if(index+1<source.length) source.substring(index,index+2) else ""
   val digraph=digraphs[pair]
   var consumed=1
   val syllable=when {
    digraph!=null -> {consumed=2;digraph}
    character=='ん' -> "n"
    else -> monographs[character.toString()]
   }
   if(syllable==null) {out.append(character);index++;sokuon=false;nasal=false;continue}
   index+=consumed
   if(nasal && (syllable.first() in vowels || syllable.first()=='y')) out.append('\'')
   out.append(when {
    !sokuon -> syllable
    syllable.startsWith("ch") -> "t$syllable"
    syllable.first() in vowels -> syllable
    else -> "${syllable.first()}$syllable"
   })
   sokuon=false
   nasal=digraph==null && character=='ん'
  }
  return out.toString()
 }
 private fun lengthen(out: StringBuilder) {
  val last=out.lastOrNull() ?: return
  val macron=macrons[last]
  if(macron!=null) out.setCharAt(out.length-1,macron) else out.append('-')
 }
 private fun normalize(text: String): String = buildString(text.length) {
  text.forEach {character ->
   when {
    character in iterationMarks -> Unit
    character in longMarks -> append(character)
    character.code in KATAKANA_FIRST..KATAKANA_LAST -> append((character.code-HIRAGANA_SHIFT).toChar())
    else -> append(character)
   }
  }
 }
}
