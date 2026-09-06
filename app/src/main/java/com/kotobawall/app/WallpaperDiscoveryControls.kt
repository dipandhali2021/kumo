package com.kotobawall.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WallpaperDiscoveryControls(browser: WallpaperBrowserViewModel,s: WallpaperBrowseState) {
 val key by browser.keyStatus.collectAsStateWithLifecycle()
 val uri=LocalUriHandler.current
 var search by rememberSaveable(s.provider,s.query) {mutableStateOf(s.query)}
 var showKey by remember {mutableStateOf(false)}
 // Do not put credentials in rememberSaveable / Android saved-instance state.
 var enteredKey by remember {mutableStateOf("")}
 val canSearch=key.ready && !key.saving && !s.loading && (s.provider!=WallpaperProvider.PEXELS || key.present)
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  HorizontalDivider();Text("Find your learning background",style=MaterialTheme.typography.titleLarge)
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   WallpaperProvider.entries.forEach {source -> FilterChip(selected=s.provider==source,onClick={browser.selectProvider(source)},label={Text(source.label)})}
  }
  if(s.provider==WallpaperProvider.PEXELS) {
   TextButton(onClick={uri.openUri("https://www.pexels.com")}) {Text("Photos provided by Pexels")}
   Text("Choose a background for your Japanese vocabulary. Search terms, your API key and network metadata go to Pexels; your own photos are never uploaded.",style=MaterialTheme.typography.bodySmall)
   OutlinedButton(onClick={enteredKey="";showKey=true},enabled=key.ready && !key.saving,modifier=Modifier.fillMaxWidth()) {
    Text(if(!key.ready) "Opening secure key storage…" else if(key.present) "Manage Pexels API key" else "Add Pexels API key")
   }
   if(key.error.isNotBlank()) Text(key.error,color=MaterialTheme.colorScheme.error)
   OutlinedTextField(value=search,onValueChange={search=it.take(100)},label={Text("Search backgrounds")},
    placeholder={Text("Try a style, subject or mood")},singleLine=true,modifier=Modifier.fillMaxWidth(),
    leadingIcon={Icon(AppIcons.Search,null)},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
    keyboardActions=KeyboardActions(onSearch={if(canSearch) browser.search(search)}))
   Button(onClick={browser.search(search)},enabled=canSearch,modifier=Modifier.fillMaxWidth()) {Text(if(search.isBlank()) "Browse Featured" else "Search")}
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    linkedMapOf("Featured" to "","Minimal" to "minimal abstract","Night city" to "city lights night","Space" to "stars galaxy","Architecture" to "architecture","Ocean" to "ocean coast","Textures" to "abstract texture").forEach {(label,query) ->
     FilterChip(selected=s.query==query,onClick={search=query;browser.search(query)},enabled=canSearch,label={Text(label)})
    }
   }
   Text("Shape",style=MaterialTheme.typography.labelLarge)
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    linkedMapOf("Portrait" to "portrait","Landscape" to "landscape","Square" to "square","Any" to "").forEach {(label,value) ->
     FilterChip(selected=s.orientation==value,onClick={browser.search(s.query,value)},enabled=canSearch && s.query.isNotBlank(),label={Text(label)})
    }
   }
   Text(if(s.query.isBlank()) "Featured is Pexels’ curated feed. Shape filters apply to searches; every selected photo is fitted to your display without stretching." else "Results for “${s.query}” · ${s.orientation.ifEmpty {"any shape"}}",style=MaterialTheme.typography.bodySmall)
  } else {
   TextButton(onClick={uri.openUri("https://picsum.photos")}) {Text("Photos from Unsplash via Lorem Picsum")}
   Text("The original no-key source is still here. Picsum offers a fixed photo catalogue, not keyword or category search. Use Pexels for topics and shape filters.",style=MaterialTheme.typography.bodySmall)
   Button(onClick={browser.load()},enabled=canSearch,modifier=Modifier.fillMaxWidth()) {Text(if(s.loaded) "Load this page" else "Browse photos")}
  }
  Text("Browse results are cached in memory for up to 24 hours. Saving downloads a local copy; screen-off rotation uses only your saved collection, with no automatic API downloads.",style=MaterialTheme.typography.bodySmall)
  if(s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  if(s.error.isNotBlank()) {
   Text(s.error,color=MaterialTheme.colorScheme.error)
   if(canSearch) TextButton(onClick={browser.load()}) {Text("Retry")}
  }
 }
 if(showKey) AlertDialog(onDismissRequest={if(!key.saving) {enteredKey="";showKey=false}},title={Text("Pexels API key")},
  text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Enter your own key once. It is encrypted with Android Keystore, excluded from backups, and sent only to api.pexels.com. It is not built into the APK.")
   OutlinedTextField(value=enteredKey,onValueChange={enteredKey=it.take(256)},singleLine=true,label={Text(if(key.present) "Replacement API key" else "API key")},
    visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false),enabled=!key.saving)
   TextButton(onClick={uri.openUri("https://www.pexels.com/api/")}) {Text("Get or manage a key at Pexels")}
   if(key.error.isNotBlank()) Text(key.error,color=MaterialTheme.colorScheme.error)
   if(key.present) TextButton(onClick={enteredKey="";browser.clearKey()},enabled=!key.saving) {Text("Remove saved key")}
  }},
  confirmButton={TextButton(onClick={browser.saveKey(enteredKey) {enteredKey="";showKey=false}},enabled=!key.saving && PexelsClient.validKey(enteredKey.trim())) {Text(if(key.saving) "Saving…" else "Save securely")}},
  dismissButton={TextButton(onClick={enteredKey="";showKey=false},enabled=!key.saving) {Text("Cancel")}})
}
