package com.kotobawall.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.collect
import kotlin.math.roundToInt

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  setContent { KotobaTheme { KotobaApp() } }
 }
}
@Composable
fun KotobaTheme(content: @Composable ()->Unit) {
 val context=LocalContext.current
 val dark=isSystemInDarkTheme()
 val scheme=when {
  Build.VERSION.SDK_INT>=31 -> if(dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  dark -> darkColorScheme(primary=Color(0xFFA9C7FF),secondary=Color(0xFFB9C7DF))
  else -> lightColorScheme(primary=Color(0xFF235BB5),onPrimary=Color.White,
   primaryContainer=Color(0xFFDCE7FF),onPrimaryContainer=Color(0xFF092C60),
   background=Color(0xFFF9FAFE),surface=Color(0xFFF9FAFE))
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
 var tab by rememberSaveable { mutableIntStateOf(0) }
 var pendingHours by rememberSaveable { mutableIntStateOf(0) }
 var showAbout by rememberSaveable { mutableStateOf(false) }
 val snackbar=remember { SnackbarHostState() }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) vm.pick(uri) }
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri -> if(uri!=null) vm.export(uri) }
 LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
 Scaffold(
  topBar={ TopAppBar(title={ Column {
   Text("Kotoba Wall",fontWeight=FontWeight.SemiBold)
   Text("A little Japanese, every day",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  } },actions={ IconButton(onClick={showAbout=true}) { Icon(Icons.Outlined.Info,contentDescription="About this app") } }) },
  snackbarHost={ SnackbarHost(snackbar) },
  bottomBar={ NavigationBar {
   listOf("Studio" to Icons.Outlined.Wallpaper,"Words" to Icons.Outlined.MenuBook,"Schedule" to Icons.Outlined.Schedule).forEachIndexed { index,item ->
    NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(item.second,contentDescription=null)},label={Text(item.first)})
   }
  } }
 ) { padding ->
  when(tab) {
   0 -> LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
    item {
     Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Text("Your next moment of learning",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
      Box(Modifier.width(184.dp).aspectRatio(if(preview!=null) preview!!.width.toFloat()/preview!!.height else 0.48f)
       .clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
       preview?.let { Image(it.asImageBitmap(),contentDescription="Generated lock-screen wallpaper preview",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds) }
        ?: if(previewError.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.Center))
        else Text("Preview unavailable. Choose another photo or a gradient.",modifier=Modifier.align(Alignment.Center).padding(16.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
       Column(Modifier.align(Alignment.TopCenter).padding(top=28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text("9:41",style=MaterialTheme.typography.headlineLarge,color=Color.White)
        Text("Clock preview",style=MaterialTheme.typography.labelSmall,color=Color.White.copy(alpha=0.8f))
       }
      }
      Text("Clock is a guide only; it is not saved in your image.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
    item {
     Card(Modifier.fillMaxWidth()) {
      Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
       Column(Modifier.weight(1f)) {
        Text(vm.words[s.wordIndex].written,style=MaterialTheme.typography.headlineSmall)
        Text(vm.words[s.wordIndex].meaning,style=MaterialTheme.typography.bodyMedium)
       }
       TextButton(onClick={vm.next()},enabled=!busy) { Text("Next"); Icon(Icons.Outlined.NavigateNext,null) }
      }
     }
    }
    item {
     Button(onClick={vm.apply()},enabled=!busy && preview!=null,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
      Icon(Icons.Outlined.Lock,null); Spacer(Modifier.width(8.dp)); Text(if(busy) "Working…" else "Set lock-screen wallpaper")
     }
     Spacer(Modifier.height(8.dp))
     Text("Changes in the editor are saved, but apply only when you tap above or a scheduled update runs.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    item {
     SectionTitle("Background",Icons.Outlined.Photo)
     OutlinedButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
      Icon(Icons.Outlined.AddPhotoAlternate,null); Spacer(Modifier.width(8.dp)); Text(if(s.photo.isEmpty()) "Choose a photo" else "Replace photo")
     }
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      WallpaperRenderer.palettes.keys.forEach { name ->
       FilterChip(selected=s.photo.isEmpty() && s.background==name,onClick={vm.palette(name)},enabled=!busy,label={Text(name)})
      }
     }
     if(s.photo.isNotEmpty()) {
      SettingSlider("Crop · left / right",s.cropX,0f..1f,!busy) { v -> vm.edit { it.copy(cropX=v) } }
      SettingSlider("Crop · top / bottom",s.cropY,0f..1f,!busy) { v -> vm.edit { it.copy(cropY=v) } }
     }
    }
    item {
     SectionTitle("Vocabulary layout",Icons.Outlined.Tune)
     ToggleRow("Kana reading","Hidden when it matches the main word",s.showReading,!busy) { v -> vm.edit { it.copy(showReading=v) } }
     ToggleRow("English meaning","A short definition below the word",s.showMeaning,!busy) { v -> vm.edit { it.copy(showMeaning=v) } }
     SettingSlider("Text size",s.scale,0.75f..1.4f,!busy) { v -> vm.edit { it.copy(scale=v) } }
     SettingSlider("Vertical position",s.position,0f..1f,!busy) { v -> vm.edit { it.copy(position=v) } }
     SettingSlider("Dark panel opacity",s.panel,0f..0.8f,!busy) { v -> vm.edit { it.copy(panel=v) } }
     Text("Leave room for your clock, notifications and fingerprint sensor. Actual placement can differ by phone.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    item {
     OutlinedButton(onClick={exporter.launch("kotoba-${vm.words[s.wordIndex].id}.png")},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
      Icon(Icons.Outlined.Download,null); Spacer(Modifier.width(8.dp)); Text("Save wallpaper as PNG")
     }
    }
   }
   1 -> WordLibrary(vm,s,busy,Modifier.padding(padding)) { tab=0 }
   2 -> LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
    item {
     Icon(Icons.Outlined.AutoAwesome,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(36.dp))
     Spacer(Modifier.height(16.dp))
     Text("Same background.\nA new word.",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold)
     Spacer(Modifier.height(12.dp))
     Text("Automatically render the next vocabulary card and update your lock screen. Everything stays on your device.",style=MaterialTheme.typography.bodyLarge)
    }
    item { OutlinedCard {
     Column(Modifier.padding(16.dp)) {
      Text("Update frequency",style=MaterialTheme.typography.titleMedium)
      listOf(0 to "Off · manual only",6 to "Every 6 hours",12 to "Every 12 hours",24 to "Daily").forEach { (hours,label) ->
       Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(enabled=!busy) {
        if(hours==0) vm.schedule(0) else if(hours!=s.hours) pendingHours=hours
       },verticalAlignment=Alignment.CenterVertically) {
        RadioButton(selected=s.hours==hours,onClick=null,enabled=!busy)
        Spacer(Modifier.width(12.dp)); Text(label)
       }
      }
     }
    } }
    item {
     Text("Updates are approximate",style=MaterialTheme.typography.titleMedium)
     Spacer(Modifier.height(8.dp))
     Text("Android may delay work during battery saving, low battery or device restrictions. The first update is scheduled after the selected interval. Force-stopping the app pauses background work until you open it again.",style=MaterialTheme.typography.bodyMedium)
    }
    item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text("Wallpaper status",style=MaterialTheme.typography.titleMedium)
     Text(if(s.lastApplied==0L) "No wallpaper applied yet" else "Last applied: "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(s.lastApplied)))
     if(s.lastError.isNotEmpty()) Text(s.lastError,color=MaterialTheme.colorScheme.error)
     Text("Automatic updates will replace any wallpaper you set elsewhere. Turn them off here to stop.",style=MaterialTheme.typography.bodySmall)
    } } }
   }
  }
 }
 if(pendingHours>0) AlertDialog(onDismissRequest={pendingHours=0},title={Text("Enable automatic wallpaper changes?")},
  text={Text("Your lock-screen wallpaper will be replaced with the next word approximately every $pendingHours hours. The original background is preserved. You can turn this off at any time.")},
  confirmButton={TextButton(onClick={vm.schedule(pendingHours);pendingHours=0}) {Text("Enable")}},
  dismissButton={TextButton(onClick={pendingHours=0}) {Text("Cancel")}})
 if(showAbout) AlertDialog(onDismissRequest={showAbout=false},title={Text("Kotoba Wall")},
  text={Text("An offline Japanese vocabulary wallpaper app.\n\n50 starter entries. Readings and meanings are intentionally concise, not a complete dictionary.\n\nKotlin · Jetpack Compose · Material 3\nMaterial Icons by Google (Apache 2.0).\n\nNo account, analytics, network permission, accessibility service or floating overlay.")},
  confirmButton={TextButton(onClick={showAbout=false}) {Text("Close")}})
}

@Composable
private fun SectionTitle(title: String,icon: ImageVector) {
 Row(Modifier.padding(bottom=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
  Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
 }
}
@Composable
private fun SettingSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,enabled: Boolean,onCommit: (Float)->Unit) {
 var local by remember(value) { mutableFloatStateOf(value) }
 Column(Modifier.padding(vertical=8.dp)) {
  Row { Text(label,Modifier.weight(1f)); Text("${(local*100).roundToInt()}%",color=MaterialTheme.colorScheme.onSurfaceVariant) }
  Slider(value=local,onValueChange={local=it},valueRange=range,enabled=enabled,onValueChangeFinished={onCommit(local)})
 }
}
@Composable
private fun ToggleRow(title: String,description: String,checked: Boolean,enabled: Boolean,onChange: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
  Column(Modifier.weight(1f).padding(end=16.dp)) {
   Text(title,style=MaterialTheme.typography.bodyLarge)
   Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Switch(checked=checked,onCheckedChange=onChange,enabled=enabled)
 }
}
@Composable
private fun WordLibrary(vm: WallViewModel,s: WallSettings,busy: Boolean,modifier: Modifier,onSelected: ()->Unit) {
 var query by rememberSaveable { mutableStateOf("") }
 var category by rememberSaveable { mutableStateOf("All") }
 val words=vm.words.withIndex().filter { (_,w) ->
  (category=="All" || w.category==category) && listOf(w.written,w.reading,w.meaning).any { it.contains(query,ignoreCase=true) }
 }
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item {
   Text("Make every glance count",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
   Spacer(Modifier.height(12.dp))
   OutlinedTextField(value=query,onValueChange={query=it},label={Text("Search Japanese or English")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    (listOf("All")+vm.words.map {it.category}.distinct()).forEach { name ->
     FilterChip(selected=category==name,onClick={category=name},label={Text(name)})
    }
   }
   Text("${words.size} words · tap to use in the studio",style=MaterialTheme.typography.bodySmall)
  }
  if(words.isEmpty()) item { Text("No words found. Try a different search or category.",Modifier.padding(vertical=24.dp)) }
  itemsIndexed(words,key={_,item->item.value.id}) { _,indexed ->
   val w=indexed.value
   OutlinedCard(onClick={if(!busy) {vm.edit {it.copy(wordIndex=indexed.index)};onSelected()}},modifier=Modifier.fillMaxWidth()) {
    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
      Text(w.written,style=MaterialTheme.typography.titleLarge)
      if(w.reading!=w.written) Text(w.reading,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Text(w.meaning,style=MaterialTheme.typography.bodyMedium)
     }
     if(s.wordIndex==indexed.index) Icon(Icons.Outlined.CheckCircle,"Selected",tint=MaterialTheme.colorScheme.primary)
    }
   }
  }
 }
}
