package com.kotobawall.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  val bars=WindowCompat.getInsetsController(window,window.decorView)
  setContent {
   val theme=rememberThemeController()
   val dark=theme.isDark()
   // Status and navigation icons follow the chosen appearance, not only the Android setting.
   SideEffect {bars.isAppearanceLightStatusBars=!dark;bars.isAppearanceLightNavigationBars=!dark}
   KotobaTheme(dark) {KotobaApp(theme)}
  }
 }
}
@Composable
fun KotobaTheme(dark: Boolean=isSystemInDarkTheme(),content: @Composable ()->Unit) {
 val context=LocalContext.current
 val scheme=when {
  Build.VERSION.SDK_INT>=31 -> if(dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  dark -> darkColorScheme(primary=Color(0xFFA9C7FF),secondary=Color(0xFFB9C7DF))
  else -> lightColorScheme(primary=Color(0xFF235BB5),onPrimary=Color.White,primaryContainer=Color(0xFFDCE7FF),onPrimaryContainer=Color(0xFF092C60),background=Color(0xFFF9FAFE),surface=Color(0xFFF9FAFE))
 }
 MaterialTheme(colorScheme=scheme,content=content)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotobaApp(theme: ThemeController,vm: WallViewModel=viewModel()) {
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
   Text("Kumo",fontWeight=FontWeight.SemiBold)
   Text("A little Japanese, every day",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }},actions={
   // Appearance switcher sits just before the info button and cycles system, light, dark.
   IconButton(onClick={vm.messages.tryEmit(theme.advance().label)}) {
    Icon(when(theme.mode) {ThemeMode.System -> AppIcons.ThemeAuto;ThemeMode.Light -> AppIcons.LightMode;ThemeMode.Dark -> AppIcons.DarkMode},theme.mode.label+", tap to change")
   }
   IconButton(onClick={showAbout=true}) {Icon(AppIcons.Info,"About this app")}
  })},
  snackbarHost={SnackbarHost(snackbar)},
  bottomBar={NavigationBar {
   listOf("Studio" to AppIcons.Wallpaper,"Words" to AppIcons.MenuBook,"Wallpapers" to AppIcons.PhotoLibrary,"Schedule" to AppIcons.Schedule).forEachIndexed {index,item ->
    NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(item.second,null)},label={Text(item.first)})
   }
  }}
 ) {padding ->
  when(tab) {
   0 -> StudioScreen(vm,s,preview,previewError,busy,Modifier.padding(padding),export={exporter.launch("kumo-wallpaper.png")})
   1 -> WordLibrary(vm,s,busy,Modifier.padding(padding)) {tab=0}
   2 -> WallpapersScreen(vm,s,busy,Modifier.padding(padding),pick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},onSelected={tab=0})
   // Every caveat that used to sit in body text now lives behind the info button on its own card.
   3 -> LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    item {Card {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Icon(AppIcons.PhonelinkLock,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp))
      Text("New word on screen-off",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
      InfoButton("Screen-off updates","Kumo prepares the next word after your screen turns off, keeping a service active with an ongoing notification and a Stop control.\n\nAndroid can delay or stop it. Battery saving, device restrictions and force-stopping the app all pause updates until you open Kumo again, and quick screen toggles may be combined into one update.\n\nThe timer below is an alternative trigger, not an extra one. Backgrounds come from your saved collection, following the rotate setting in Wallpapers.")
     }
     Text(when {cycle.running -> "Active";cycle.enabled -> "Stopped";else -> "Off"},
      style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)
     if(cycle.error.isNotEmpty()) Text(cycle.error,color=MaterialTheme.colorScheme.error)
     Button(onClick={if(cycle.running) vm.stopCycle() else confirmCycle=true},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
      Text(if(cycle.running) "Stop" else if(cycle.enabled) "Resume" else "Enable")
     }
     if(cycle.enabled && !cycle.running) TextButton(onClick={vm.stopCycle()}) {Text("Turn off")}
    }}}
    item {OutlinedCard {Column(Modifier.padding(16.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Text("Update frequency",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
      InfoButton("Update frequency","Timed updates are approximate. Android may delay them during battery saving, low battery or device restrictions, and the first update arrives after the interval you pick, not immediately.\n\nForce-stopping the app pauses background work until you open it again. Enabling screen-off updates above switches the timer off.")
     }
     listOf(0 to "Off",6 to "Every 6 hours",12 to "Every 12 hours",24 to "Daily").forEach {(hours,label) ->
      Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(enabled=!busy) {if(hours==0) vm.schedule(0) else if(hours!=s.hours || cycle.enabled) pendingHours=hours},verticalAlignment=Alignment.CenterVertically) {
       RadioButton(selected=s.hours==hours && !cycle.enabled,onClick=null,enabled=!busy);Spacer(Modifier.width(12.dp));Text(label)
      }
     }
    }}}
    item {Card {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Text("Wallpaper status",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
      InfoButton("Wallpaper status","Automatic updates replace any lock-screen wallpaper you set elsewhere. Turn them off here to stop.\n\nEach card is rendered on your device from your saved vocabulary. Nothing is uploaded.")
     }
     Text(if(s.lastApplied==0L) "Not applied yet" else "Last applied "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(s.lastApplied)),
      style=MaterialTheme.typography.bodyMedium)
     if(s.lastError.isNotEmpty()) Text(s.lastError,color=MaterialTheme.colorScheme.error)
    }}}
   }
  }
 }
 if(confirmCycle) AlertDialog(onDismissRequest={confirmCycle=false},title={Text("Enable screen-off updates?")},
  text={Text("Kumo keeps a service active with an ongoing notification and a Stop control, and updates your lock screen after screen-off events. Android can delay or stop it. This turns off timed rotation.")},
  confirmButton={TextButton(onClick={confirmCycle=false
   if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
   else vm.startCycle()
  }) {Text("Enable")}},dismissButton={TextButton(onClick={confirmCycle=false}) {Text("Cancel")}})
 if(pendingHours>0) AlertDialog(onDismissRequest={pendingHours=0},title={Text("Change wallpaper automatically?")},
  text={Text("Your lock screen will show the next word about every $pendingHours hours. Your background is kept, and you can turn this off at any time.")},
  confirmButton={TextButton(onClick={vm.schedule(pendingHours);pendingHours=0}) {Text("Enable")}},dismissButton={TextButton(onClick={pendingHours=0}) {Text("Cancel")}})
 if(showAbout) {
  val licenses by produceState("") {value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
   listOf("gothic_OFL.txt","mincho_OFL.txt").joinToString("\n\n") {name ->context.assets.open("fonts/$name").bufferedReader().use {it.readText()}}
  }}
  AlertDialog(onDismissRequest={showAbout=false},title={Text("Kumo 1.9 \u00b7 \u96f2")},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("50 offline starter entries, plus optional JLPT N5\u2013N1 downloads. No account or analytics. Your photos and settings are not uploaded. The vocabulary provider receives your IP address and requested level when you download.")
   Text("Romaji: taken from the vocabulary service when it supplies one, and otherwise written on your device from the kana reading using modified Hepburn, with macrons for long vowels.")
   Text("Vocabulary: wkei / JLPT Vocabulary API, based on Jonathan Waller\u2019s Tanos study lists. Levels are estimates, not an official JLPT syllabus. Readings and meanings may contain errors.")
   Text("Japanese fonts: Zen Kaku Gothic New and Zen Old Mincho, bundled under SIL Open Font License 1.1. The \u96f2 app icon is drawn from Noto Sans JP Bold outlines, also SIL Open Font License 1.1. Interface icons: original compact Kumo vector set.")
   Text("Online photos: Unsplash via Lorem Picsum by default, with Pexels as an optional source. Pexels receives your search terms and API key. Browsing and saving contact the provider and CDN. Keys are entered on-device and encrypted with Android Keystore, not bundled in the APK. Saved backgrounds and Last used stay in private app storage. Coil image loader: Apache 2.0.")
   Text(licenses,style=MaterialTheme.typography.bodySmall)
  }},confirmButton={TextButton(onClick={showAbout=false}) {Text("Close")}})
 }
}
/** Title-row info affordance: keeps long explanations off the screen until they are asked for. */
@Composable
private fun InfoButton(title: String,body: String) {
 var open by remember {mutableStateOf(false)}
 IconButton(onClick={open=true},modifier=Modifier.size(36.dp)) {
  Icon(AppIcons.Info,"About $title",tint=MaterialTheme.colorScheme.onSurfaceVariant)
 }
 if(open) AlertDialog(onDismissRequest={open=false},title={Text(title)},
  text={Text(body,style=MaterialTheme.typography.bodyMedium)},
  confirmButton={TextButton(onClick={open=false}) {Text("Got it")}})
}
