package com.kotobawall.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

/**
 * Sources and their settings, in a bottom sheet behind the Discover filter button. Picking a source
 * or running a search closes the sheet so the results are visible straight away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperFilterSheet(browser: WallpaperBrowserViewModel,s: WallpaperBrowseState,onDismiss: ()->Unit) {
 val key by browser.keyStatus.collectAsStateWithLifecycle()
 val uri=LocalUriHandler.current
 var search by rememberSaveable(s.provider,s.query) {mutableStateOf(s.query)}
 var showKey by remember {mutableStateOf(false)}
 // Do not put credentials in rememberSaveable / Android saved-instance state.
 var enteredKey by remember {mutableStateOf("")}
 val canSearch=key.ready && !key.saving && !s.loading && (s.provider!=WallpaperProvider.PEXELS || key.present)
 ModalBottomSheet(onDismissRequest=onDismiss) {
  Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState())
   .padding(start=20.dp,end=20.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Sources & filters",style=MaterialTheme.typography.titleLarge)
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    WallpaperProvider.entries.forEach {source ->
     FilterChip(selected=s.provider==source,onClick={browser.selectProvider(source)},label={Text(source.label)})
    }
   }
   if(s.provider==WallpaperProvider.PEXELS) {
    OutlinedButton(onClick={enteredKey="";showKey=true},enabled=key.ready && !key.saving,modifier=Modifier.fillMaxWidth()) {
     Text(if(!key.ready) "Opening secure key storage\u2026" else if(key.present) "Manage Pexels API key" else "Add Pexels API key")
    }
    if(key.error.isNotBlank()) Text(key.error,color=MaterialTheme.colorScheme.error)
    OutlinedTextField(value=search,onValueChange={search=it.take(100)},placeholder={Text("Search backgrounds")},
     singleLine=true,shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth(),
     leadingIcon={Icon(AppIcons.Search,null)},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
     keyboardActions=KeyboardActions(onSearch={if(canSearch) {browser.search(search);onDismiss()}}))
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     linkedMapOf("Featured" to "","Minimal" to "minimal abstract","Night city" to "city lights night","Space" to "stars galaxy","Architecture" to "architecture","Ocean" to "ocean coast","Textures" to "abstract texture").forEach {(label,query) ->
      FilterChip(selected=s.query==query,onClick={search=query;browser.search(query)},enabled=canSearch,label={Text(label)})
     }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     linkedMapOf("Portrait" to "portrait","Landscape" to "landscape","Square" to "square","Any" to "").forEach {(label,value) ->
      FilterChip(selected=s.orientation==value,onClick={browser.search(s.query,value)},enabled=canSearch && s.query.isNotBlank(),label={Text(label)})
     }
    }
    Button(onClick={browser.search(search);onDismiss()},enabled=canSearch,shape=RoundedCornerShape(16.dp),
     modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(if(search.isBlank()) "Browse featured" else "Search")}
    Text("Search terms and your key go to Pexels. Your own photos are never uploaded.",
     style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick={uri.openUri("https://www.pexels.com")}) {Text("Photos provided by Pexels")}
   } else {
    Button(onClick={browser.load();onDismiss()},enabled=canSearch,shape=RoundedCornerShape(16.dp),
     modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text("Browse photos")}
    Text("No key needed. A fixed catalogue, so keyword search and shape filters need Pexels.",
     style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick={uri.openUri("https://picsum.photos")}) {Text("Photos from Unsplash via Lorem Picsum")}
   }
   if(s.error.isNotBlank()) Text(s.error,color=MaterialTheme.colorScheme.error)
  }
 }
 if(showKey) AlertDialog(onDismissRequest={if(!key.saving) {enteredKey="";showKey=false}},title={Text("Pexels API key")},
  text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Enter your own key once. It is encrypted with Android Keystore, excluded from backups, and sent only to api.pexels.com.")
   OutlinedTextField(value=enteredKey,onValueChange={enteredKey=it.take(256)},singleLine=true,label={Text(if(key.present) "Replacement API key" else "API key")},
    visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false),enabled=!key.saving)
   TextButton(onClick={uri.openUri("https://www.pexels.com/api/")}) {Text("Get or manage a key at Pexels")}
   if(key.error.isNotBlank()) Text(key.error,color=MaterialTheme.colorScheme.error)
   if(key.present) TextButton(onClick={enteredKey="";browser.clearKey()},enabled=!key.saving) {Text("Remove saved key")}
  }},
  confirmButton={TextButton(onClick={browser.saveKey(enteredKey) {enteredKey="";showKey=false}},enabled=!key.saving && PexelsClient.validKey(enteredKey.trim())) {Text(if(key.saving) "Saving\u2026" else "Save securely")}},
  dismissButton={TextButton(onClick={enteredKey="";showKey=false},enabled=!key.saving) {Text("Cancel")}})
}
