package com.kotobawall.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Sources only. Search and shape filters moved onto the Discover grid, because they belong next to
 * the results they change; this sheet keeps the source choice and the Pexels key setting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperFilterSheet(browser: WallpaperBrowserViewModel,s: WallpaperBrowseState,onDismiss: ()->Unit) {
 val key by browser.keyStatus.collectAsStateWithLifecycle()
 val uri=LocalUriHandler.current
 var showKey by remember {mutableStateOf(false)}
 // Do not put credentials in rememberSaveable / Android saved-instance state.
 var enteredKey by remember {mutableStateOf("")}
 ModalBottomSheet(onDismissRequest=onDismiss) {
  Column(Modifier.fillMaxWidth().heightIn(max=520.dp).verticalScroll(rememberScrollState())
   .padding(start=20.dp,end=20.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Photo source",style=MaterialTheme.typography.titleLarge)
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    WallpaperProvider.entries.forEach {source ->
     FilterChip(selected=s.provider==source,onClick={browser.selectProvider(source);onDismiss()},label={Text(source.label)})
    }
   }
   if(s.provider==WallpaperProvider.PEXELS) {
    OutlinedButton(onClick={enteredKey="";showKey=true},enabled=key.ready && !key.saving,modifier=Modifier.fillMaxWidth()) {
     Text(if(!key.ready) "Opening secure key storage\u2026" else if(key.present) "Manage Pexels API key" else "Add Pexels API key")
    }
    if(key.error.isNotBlank()) Text(key.error,color=MaterialTheme.colorScheme.error)
    Text("Search terms and your key go to Pexels. Your own photos are never uploaded.",
     style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick={uri.openUri("https://www.pexels.com")}) {Text("Photos provided by Pexels")}
   } else {
    Text("No key needed. A fixed catalogue, so keyword search needs Pexels.",
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
