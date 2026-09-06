package com.kotobawall.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.collect

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {super.onCreate(savedInstanceState);enableEdgeToEdge();setContent {KotobaTheme {KotobaApp()}}}
}
@Composable
fun KotobaTheme(content: @Composable ()->Unit) {
 val context=LocalContext.current;val dark=isSystemInDarkTheme()
 val scheme=when {
  Build.VERSION.SDK_INT>=31 -> if(dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  dark -> darkColorScheme(primary=Color(0xFFA9C7FF),secondary=Color(0xFFB9C7DF))
  else -> lightColorScheme(primary=Color(0xFF235BB5),onPrimary=Color.White,primaryContainer=Color(0xFFDCE7FF),onPrimaryContainer=Color(0xFF092C60),background=Color(0xFFF9FAFE),surface=Color(0xFFF9FAFE))
 }
 MaterialTheme(colorScheme=scheme,content=content)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotobaApp(vm: WallViewModel=viewModel()) {
 val s by vm.settings.collectAsStateWithLifecycle()
 val preview by vm.preview.collectAsStateWithLifecycle()
 val previewError by vm.previewError.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val cycle by vm.cycle.collectAsStateWithLifecycle()
 val context=LocalContext.current
 var confirmCycle by rememberSaveable {mutableStateOf(false)}
 val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted ->
  if(granted) vm.startCycle() else vm.messages.tryEmit("Notification permission is needed for this opt-in mode. You can allow it in Android settings.")
 }
 var tab by rememberSaveable {mutableIntStateOf(0)}
 var pendingHours by rememberSaveable {mutableIntStateOf(0)}
 var showAbout by rememberSaveable {mutableStateOf(false)}
 val snackbar=remember {SnackbarHostState()}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {uri ->if(uri!=null) vm.pick(uri)}
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {uri ->if(uri!=null) vm.export(uri)}
 LaunchedEffect(vm) {vm.messages.collect {snackbar.showSnackbar(it)}}
 Scaffold(
  topBar={TopAppBar(title={Column {
   Text("Kotoba Wall",fontWeight=FontWeight.SemiBold)
   Text("A little Japanese, every day",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }},actions={IconButton(onClick={showAbout=true}) {Icon(Icons.Outlined.Info,"About this app")}})},
  snackbarHost={SnackbarHost(snackbar)},
  bottomBar={NavigationBar {
   listOf("Studio" to Icons.Outlined.Wallpaper,"Words" to Icons.Outlined.MenuBook,"Wallpapers" to Icons.Outlined.PhotoLibrary,"Schedule" to Icons.Outlined.Schedule).forEachIndexed {index,item ->
    NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(item.second,null)},label={Text(item.first)})
   }
  }}
 ) {padding ->
  when(tab) {
   0 -> StudioScreen(vm,s,preview,previewError,busy,Modifier.padding(padding),export={exporter.launch("kotoba-wallpaper.png")})
   1 -> WordLibrary(vm,s,busy,Modifier.padding(padding)) {tab=0}
   2 -> WallpapersScreen(vm,s,busy,Modifier.padding(padding),pick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},onSelected={tab=0})
   3 -> LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
    item {
     Icon(Icons.Outlined.AutoAwesome,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(36.dp));Spacer(Modifier.height(16.dp))
     Text(if(s.rotateWallpaper) "A new background.\nA new word." else "Same background.\nA new word.",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold)
     Spacer(Modifier.height(12.dp))
     Text("Automatically render the next vocabulary card and update your lock screen. Rendering happens on your device using your saved vocabulary.",style=MaterialTheme.typography.bodyLarge)
    }
    item {Card {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Icon(Icons.Outlined.PhonelinkLock,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp))
      Text("New word on screen-off",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
     }
     Text("Prepare the next word on screen-off. Keep your chosen background, or rotate your saved collection using the setting in Wallpapers.")
     Text(when {cycle.running -> "Active · ongoing notification shown";cycle.enabled -> "Stopped · tap Resume to restart";else -> "Off · enable to start"},color=MaterialTheme.colorScheme.primary)
     if(cycle.error.isNotEmpty()) Text(cycle.error,color=MaterialTheme.colorScheme.error)
     Button(onClick={if(cycle.running) vm.stopCycle() else confirmCycle=true},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
      Text(if(cycle.running) "Stop screen-off updates" else if(cycle.enabled) "Resume screen-off updates" else "Enable screen-off updates")
     }
     if(cycle.enabled && !cycle.running) TextButton(onClick={vm.stopCycle()}) {Text("Turn this mode off")}
     Text("An ongoing notification is required. Fast toggles may be combined. Battery restrictions or force-stop can stop updates; reopen the app and resume. The timer below is an alternative, not an additional trigger.",style=MaterialTheme.typography.bodySmall)
    }}}
    item {OutlinedCard {Column(Modifier.padding(16.dp)) {
     Text("Update frequency",style=MaterialTheme.typography.titleMedium)
     listOf(0 to "Off · manual only",6 to "Every 6 hours",12 to "Every 12 hours",24 to "Daily").forEach {(hours,label) ->
      Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(enabled=!busy) {if(hours==0) vm.schedule(0) else if(hours!=s.hours || cycle.enabled) pendingHours=hours},verticalAlignment=Alignment.CenterVertically) {
       RadioButton(selected=s.hours==hours && !cycle.enabled,onClick=null,enabled=!busy);Spacer(Modifier.width(12.dp));Text(label)
      }
     }
    }}}
    item {
     Text("Updates are approximate",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(8.dp))
     Text("Android may delay work during battery saving, low battery or device restrictions. The first update is scheduled after the selected interval. Force-stopping the app pauses background work until you open it again.",style=MaterialTheme.typography.bodyMedium)
    }
    item {Card {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text("Wallpaper status",style=MaterialTheme.typography.titleMedium)
     Text(if(s.lastApplied==0L) "No wallpaper applied yet" else "Last applied: "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(s.lastApplied)))
     if(s.lastError.isNotEmpty()) Text(s.lastError,color=MaterialTheme.colorScheme.error)
     Text("Automatic updates will replace any wallpaper you set elsewhere. Turn them off here to stop.",style=MaterialTheme.typography.bodySmall)
    }}}
   }
  }
 }
 if(confirmCycle) AlertDialog(onDismissRequest={confirmCycle=false},title={Text("Enable screen-off updates?")},
  text={Text("Kotoba Wall will keep a service active with an ongoing notification and a Stop control. It updates your lock-screen image after screen-off events. Android can delay or stop it; it is not guaranteed on every wake. Enabling this turns off timed rotation.")},
  confirmButton={TextButton(onClick={confirmCycle=false
   if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
   else vm.startCycle()
  }) {Text("Enable")}},dismissButton={TextButton(onClick={confirmCycle=false}) {Text("Cancel")}})
 if(pendingHours>0) AlertDialog(onDismissRequest={pendingHours=0},title={Text("Enable automatic wallpaper changes?")},
  text={Text("Your lock-screen wallpaper will be replaced with the next word approximately every $pendingHours hours. The original background is preserved. You can turn this off at any time.")},
  confirmButton={TextButton(onClick={vm.schedule(pendingHours);pendingHours=0}) {Text("Enable")}},dismissButton={TextButton(onClick={pendingHours=0}) {Text("Cancel")}})
 if(showAbout) {
  val licenses by produceState("") {value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
   listOf("gothic_OFL.txt","mincho_OFL.txt").joinToString("\n\n") {name ->context.assets.open("fonts/$name").bufferedReader().use {it.readText()}}
  }}
  AlertDialog(onDismissRequest={showAbout=false},title={Text("Kotoba Wall 1.5")},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("50 offline starter entries, plus optional JLPT N5–N1 downloads. No account or analytics. Your photos and settings are not uploaded. The vocabulary provider receives your IP address and requested level when you download.")
   Text("Vocabulary: wkei / JLPT Vocabulary API, based on Jonathan Waller’s Tanos study lists. Levels are estimates, not an official JLPT syllabus. Readings and meanings may contain errors.")
   Text("Japanese fonts: Zen Kaku Gothic New and Zen Old Mincho, bundled under SIL Open Font License 1.1. Material Icons: Google, Apache 2.0.")
   Text("Online photos: Pexels (default) and Unsplash via Lorem Picsum. Pexels receives your search terms and API key. Browsing and saving contact the provider and CDN. Keys are entered on-device and encrypted with Android Keystore, not bundled in the APK. Saved backgrounds and Last used stay in private app storage. Coil image loader: Apache 2.0.")
   Text(licenses,style=MaterialTheme.typography.bodySmall)
  }},confirmButton={TextButton(onClick={showAbout=false}) {Text("Close")}})
 }
}
