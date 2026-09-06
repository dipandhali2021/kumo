package com.kotobawall.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@Composable
fun WordLibrary(vm: WallViewModel,s: WallSettings,busy: Boolean,modifier: Modifier,onSelected: ()->Unit) {
 val library by vm.words.collectAsStateWithLifecycle()
 val download by vm.download.collectAsStateWithLifecycle()
 var query by rememberSaveable {mutableStateOf("")}
 val uri=LocalUriHandler.current
 val pool=remember(library,s.levels,s.includeStarter,s.favorites,s.favoritesOnly) {library.filter {WordPolicy.eligible(it,s)}}
 val matches=remember(pool,query) {pool.filter {w ->listOf(w.written,w.reading,w.meaning).any {it.contains(query.trim(),true)}}}
 val counts=remember(library) {library.groupingBy {it.level}.eachCount()}
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item {
   Text("Your vocabulary",style=MaterialTheme.typography.headlineSmall)
   Spacer(Modifier.height(8.dp))
   Text("Choose the levels used in this list and automatic wallpaper rotation. N5 is beginner; N1 is advanced.")
  }
  item {OutlinedCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("JLPT levels",style=MaterialTheme.typography.titleMedium)
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    (5 downTo 1).forEach {level -> FilterChip(selected=level in s.levels,enabled=!busy && !download.running,
     onClick={vm.edit {it.copy(levels=if(level in it.levels) it.levels-level else it.levels+level)}},
     label={Text("N$level · ${counts[level] ?: 0}")})}
   }
   LibrarySwitch("Include 50 offline starter words",s.includeStarter,!busy) {v->vm.edit {it.copy(includeStarter=v)}}
   LibrarySwitch("Favorites only",s.favoritesOnly,!busy) {v->vm.edit {it.copy(favoritesOnly=v)}}
   LibrarySwitch("Shuffle wallpaper rotation",s.shuffle,!busy) {v->vm.edit {it.copy(shuffle=v)}}
   Text("${pool.size} eligible words · searches only filter this list, not wallpaper rotation.",style=MaterialTheme.typography.bodySmall)
   Button(onClick={vm.downloadLevels()},enabled=!download.running && s.levels.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
    Icon(Icons.Outlined.Download,null);Spacer(Modifier.width(8.dp));Text(if(download.running) "Downloading…" else "Download / refresh selected levels")
   }
   if(download.running) LinearProgressIndicator(Modifier.fillMaxWidth())
   if(download.message.isNotBlank()) Text(download.message,style=MaterialTheme.typography.bodyMedium)
   if(download.error.isNotBlank()) Text(download.error,color=MaterialTheme.colorScheme.error)
   s.levels.sortedDescending().forEach {level ->
    val stamp=vm.downloadedAt(level)
    Text(if(stamp==0L) "N$level: not downloaded" else "N$level: saved "+DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(stamp)),style=MaterialTheme.typography.bodySmall)
   }
   Text("Downloads need internet. Saved words work offline; screen-off updates never call the API. Public service availability and word accuracy can vary.",style=MaterialTheme.typography.bodySmall)
  }}}
  item {
   OutlinedTextField(value=query,onValueChange={query=it.take(120)},singleLine=true,label={Text("Search Japanese, kana or meaning")},
    leadingIcon={Icon(Icons.Outlined.Search,null)},modifier=Modifier.fillMaxWidth(),
    trailingIcon={if(query.isNotEmpty()) IconButton(onClick={query=""}) {Icon(Icons.Outlined.Close,"Clear search")}})
   Spacer(Modifier.height(8.dp));Text("${matches.size} results · tap a word to preview",style=MaterialTheme.typography.bodySmall)
  }
  if(matches.isEmpty()) item {
   Text(if(pool.isEmpty()) "No eligible words. Download selected levels, include the starter pack, or turn off Favorites only." else "No matches. Try a shorter search.",Modifier.padding(vertical=16.dp))
  }
  items(matches,key={it.id}) {w ->
   OutlinedCard(onClick={vm.selectWord(w.id);onSelected()},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
      Text(if(w.level==0) "Starter · ungraded" else "JLPT N${w.level}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
      Text(w.written,style=MaterialTheme.typography.titleLarge)
      if(w.reading!=w.written) Text(w.reading,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Text(w.meaning,style=MaterialTheme.typography.bodyMedium)
     }
     Column(horizontalAlignment=Alignment.CenterHorizontally) {
      IconButton(onClick={vm.edit {it.copy(favorites=if(w.id in it.favorites) it.favorites-w.id else it.favorites+w.id)}},enabled=!busy) {
       Icon(if(w.id in s.favorites) Icons.Outlined.Star else Icons.Outlined.StarBorder,
        if(w.id in s.favorites) "Remove favorite" else "Add favorite",tint=MaterialTheme.colorScheme.primary)
      }
      if(library.getOrNull(s.wordIndex)?.id==w.id) Icon(Icons.Outlined.CheckCircle,"Selected",tint=MaterialTheme.colorScheme.primary)
     }
    }
   }
  }
  item {
   Text("Source: JLPT Vocabulary API by wkei; underlying study lists from Jonathan Waller / Tanos. These are third-party study levels, not an official JLPT vocabulary syllabus.",style=MaterialTheme.typography.bodySmall)
   TextButton(onClick={uri.openUri(JlptClient.HOME)}) {Text("Vocabulary source & documentation")}
  }
 }
}
@Composable
private fun LibrarySwitch(label: String,checked: Boolean,enabled: Boolean,onChange: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
  Text(label,Modifier.weight(1f).padding(end=8.dp));Switch(checked=checked,onCheckedChange=onChange,enabled=enabled)
 }
}
