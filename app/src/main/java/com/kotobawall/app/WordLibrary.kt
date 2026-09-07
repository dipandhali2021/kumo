package com.kotobawall.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLibrary(vm: WallViewModel,s: WallSettings,busy: Boolean,modifier: Modifier,onSelected: ()->Unit) {
 val library by vm.words.collectAsStateWithLifecycle()
 val download by vm.download.collectAsStateWithLifecycle()
 var query by rememberSaveable {mutableStateOf("")}
 var showFilters by rememberSaveable {mutableStateOf(false)}
 val uri=LocalUriHandler.current
 // distinctBy is not cosmetic: the vocabulary service can repeat a word inside one level, and
 // duplicate keys in a lazy list throw, which used to take the entire list down.
 val pool=remember(library,s.levels,s.includeStarter,s.favorites,s.favoritesOnly) {
  library.filter {WordPolicy.eligible(it,s)}.distinctBy {it.id}
 }
 // Transliterate once per pool: searching would otherwise convert thousands of words on every keystroke.
 val romajiById=remember(pool) {pool.associate {it.id to Romaji.display(it)}}
 val matches=remember(pool,romajiById,query) {
  val term=query.trim()
  if(term.isEmpty()) pool
  else pool.filter {w ->listOf(w.written,w.reading,w.meaning,romajiById[w.id] ?: "").any {it.contains(term,true)}}
 }
 val counts=remember(library) {library.groupingBy {it.level}.eachCount()}
 // The panel is hidden now, so the button itself has to show that a filter is narrowing the list.
 val filtered=s.favoritesOnly || !s.includeStarter || s.levels!=setOf(5)
 Column(modifier.fillMaxSize()) {
  Row(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=12.dp,bottom=6.dp),
   horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
   OutlinedTextField(value=query,onValueChange={query=it.take(120)},singleLine=true,
    shape=RoundedCornerShape(28.dp),modifier=Modifier.weight(1f),
    placeholder={Text("Search kanji, kana, romaji or meaning",maxLines=1,overflow=TextOverflow.Ellipsis)},
    leadingIcon={Icon(AppIcons.Search,null)},
    trailingIcon={if(query.isNotEmpty()) IconButton(onClick={query=""}) {Icon(AppIcons.Close,"Clear search")}})
   Surface(shape=RoundedCornerShape(18.dp),
    color=if(filtered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
    IconButton(onClick={showFilters=true},modifier=Modifier.size(56.dp)) {
     Icon(AppIcons.Filter,"Levels and filters",
      tint=if(filtered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  // Counts are spelled out so an empty list from filters can be told apart from an empty library.
  Text("${matches.size} shown\u2009\u00b7\u2009${pool.size} eligible\u2009\u00b7\u2009${library.size} in library",
   style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
   modifier=Modifier.padding(start=20.dp,end=20.dp,bottom=6.dp))
  LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.fillMaxWidth().weight(1f),
   contentPadding=PaddingValues(start=20.dp,end=20.dp,top=6.dp,bottom=24.dp),
   horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   if(matches.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {
    Column(Modifier.padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     if(library.isEmpty()) Text("The word list could not be read. Reinstalling the app restores the 50 starter words.",color=MaterialTheme.colorScheme.error)
     else if(pool.isEmpty()) {
      Text("Every word is filtered out right now.",style=MaterialTheme.typography.titleMedium)
      if(!s.includeStarter) Button(onClick={vm.edit {it.copy(includeStarter=true)}},enabled=!busy,shape=RoundedCornerShape(20.dp)) {Text("Turn the 50 starter words back on")}
      if(s.favoritesOnly) Button(onClick={vm.edit {it.copy(favoritesOnly=false)}},enabled=!busy,shape=RoundedCornerShape(20.dp)) {Text("Stop showing favorites only")}
      if(s.levels.isEmpty()) Button(onClick={vm.edit {it.copy(levels=setOf(5))}},enabled=!busy,shape=RoundedCornerShape(20.dp)) {Text("Select level N5")}
      TextButton(onClick={showFilters=true}) {Text("Open levels & filters")}
     } else Text("Nothing matches \u201c${query.trim()}\u201d. Try a shorter search.")
    }
   }
   items(matches,key={it.id}) {w ->
    val favorite=w.id in s.favorites
    OutlinedCard(onClick={vm.selectWord(w.id);onSelected()},enabled=!busy,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
     Column(Modifier.padding(start=14.dp,end=6.dp,top=14.dp,bottom=6.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
      Text(if(w.level==0) "Starter" else "N${w.level}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
      Text(w.written,style=MaterialTheme.typography.headlineSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
      if(w.reading!=w.written) Text(w.reading,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
      val romaji=romajiById[w.id] ?: ""
      if(romaji.isNotBlank()) Text(romaji,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
      Text(w.meaning,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
       if(library.getOrNull(s.wordIndex)?.id==w.id) Icon(AppIcons.CheckCircle,"Selected",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(18.dp))
       Spacer(Modifier.weight(1f))
       IconButton(onClick={vm.edit {it.copy(favorites=if(favorite) it.favorites-w.id else it.favorites+w.id)}},enabled=!busy) {
        Icon(if(favorite) AppIcons.Star else AppIcons.StarBorder,if(favorite) "Remove favorite" else "Add favorite",tint=MaterialTheme.colorScheme.primary)
       }
      }
     }
    }
   }
   item(span={GridItemSpan(maxLineSpan)}) {
    Column(Modifier.padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text("Romaji comes from the vocabulary service when it provides one, and is otherwise written on your device from the kana reading.",style=MaterialTheme.typography.bodySmall)
     Text("Source: JLPT Vocabulary API by wkei; underlying study lists from Jonathan Waller / Tanos. These are third-party study levels, not an official JLPT vocabulary syllabus.",style=MaterialTheme.typography.bodySmall)
     TextButton(onClick={uri.openUri(JlptClient.HOME)}) {Text("Vocabulary source & documentation")}
    }
   }
  }
 }
 if(showFilters) ModalBottomSheet(onDismissRequest={showFilters=false}) {
  Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState())
   .padding(start=20.dp,end=20.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Levels & filters",style=MaterialTheme.typography.titleLarge)
   Text("These choices drive this list and automatic wallpaper updates. N5 is beginner; N1 is advanced.",style=MaterialTheme.typography.bodyMedium)
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    (5 downTo 1).forEach {level -> FilterChip(selected=level in s.levels,enabled=!busy && !download.running,
     onClick={vm.edit {it.copy(levels=if(level in it.levels) it.levels-level else it.levels+level)}},
     label={Text("N$level\u2009\u00b7\u2009${counts[level] ?: 0}")})}
   }
   LibrarySwitch("Include 50 offline starter words",s.includeStarter,!busy) {v->vm.edit {it.copy(includeStarter=v)}}
   LibrarySwitch("Favorites only",s.favoritesOnly,!busy) {v->vm.edit {it.copy(favoritesOnly=v)}}
   Text("${pool.size} eligible words",style=MaterialTheme.typography.bodySmall)
   Button(onClick={vm.downloadLevels()},enabled=!download.running && s.levels.isNotEmpty(),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
    Icon(AppIcons.Download,null);Spacer(Modifier.width(8.dp));Text(if(download.running) "Downloading\u2026" else "Download / refresh selected levels")
   }
   if(download.running) LinearProgressIndicator(Modifier.fillMaxWidth())
   if(download.message.isNotBlank()) Text(download.message,style=MaterialTheme.typography.bodyMedium)
   if(download.error.isNotBlank()) Text(download.error,color=MaterialTheme.colorScheme.error)
   s.levels.sortedDescending().forEach {level ->
    val stamp=vm.downloadedAt(level)
    Text(if(stamp==0L) "N$level: not downloaded" else "N$level: saved "+DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(stamp)),style=MaterialTheme.typography.bodySmall)
   }
   Text("Downloads need internet. Saved words work offline; screen-off updates never call the API. Public service availability and word accuracy can vary.",style=MaterialTheme.typography.bodySmall)
  }
 }
}
@Composable
private fun LibrarySwitch(label: String,checked: Boolean,enabled: Boolean,onChange: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
  Text(label,Modifier.weight(1f).padding(end=8.dp));Switch(checked=checked,onCheckedChange=onChange,enabled=enabled)
 }
}
