package com.kotobawall.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun TypographyEditor(value: Typography,enabled: Boolean,dirty: Boolean,onChange: (Typography)->Unit,onSave: ()->Unit) {
 var selected by rememberSaveable {mutableIntStateOf(0)}
 val index=selected.coerceAtMost(value.lineCount-1)
 val row=value.rows[index]
 fun update(r: TextRow) {onChange(value.withRow(index,r))}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Line designer",style=MaterialTheme.typography.titleMedium)
  Text("Choose what each line says and how it looks. Save the layout to use it for automatic updates.",style=MaterialTheme.typography.bodyMedium)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   listOf(2,3).forEach {count -> FilterChip(selected=value.lineCount==count,onClick={onChange(value.copy(lineCount=count))},enabled=enabled,label={Text("$count lines")})}
  }
  ChoiceMenu("Block alignment",value.alignment,Typography.alignments,enabled) {onChange(value.copy(alignment=it))}
  ChoiceMenu("Font for all lines",value.rows.map {it.font}.distinct().singleOrNull() ?: "Mixed",Typography.fonts,enabled) {font -> onChange(value.copy(rows=value.rows.map {it.copy(font=font)}))}
  EditorSlider("Space between lines",value.spacing,0f..24f,enabled) {onChange(value.copy(spacing=it))}
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Text("Hide repeated kana reading",Modifier.weight(1f).padding(end=8.dp))
   Switch(checked=value.hideRepeatedReading,onCheckedChange={onChange(value.copy(hideRepeatedReading=it))},enabled=enabled)
  }
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   (0 until value.lineCount).forEach {i -> FilterChip(selected=index==i,onClick={selected=i},label={Text("Line ${i+1}")})}
  }
  OutlinedCard {
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Text("Line ${index+1}",style=MaterialTheme.typography.titleMedium)
    ChoiceMenu("Content preset",Typography.presets.entries.firstOrNull {it.value==row.template}?.key ?: "Custom text",Typography.presets.keys.toList(),enabled) {
     update(row.copy(template=Typography.presets.getValue(it)))
    }
    OutlinedTextField(value=row.template,onValueChange={update(row.copy(template=it.take(160)))},enabled=enabled,
     modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Line content / custom text")},
     supportingText={Text("Use {word}, {reading}, {meaning}, or your own text. Tokens change with each vocabulary word.")})
    ChoiceMenu("Font",row.font,Typography.fonts,enabled) {update(row.copy(font=it))}
    ChoiceMenu("Line alignment",row.alignment,listOf("Default")+Typography.alignments,enabled) {update(row.copy(alignment=it))}
    EditorSlider("Line font size",row.size,12f..60f,enabled) {update(row.copy(size=it))}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     Text("Bold",Modifier.weight(1f));Switch(checked=row.bold,onCheckedChange={update(row.copy(bold=it))},enabled=enabled)
    }
    var hex by remember(index,row.color) {mutableStateOf(row.color)}
    val valid=hex.matches(Regex("#[0-9a-fA-F]{6}"))
    OutlinedTextField(value=hex,onValueChange={next -> hex=next.take(7);if(hex.matches(Regex("#[0-9a-fA-F]{6}"))) update(row.copy(color=hex))},
     enabled=enabled,label={Text("Text color · #RRGGBB")},singleLine=true,isError=!valid,modifier=Modifier.fillMaxWidth(),
     supportingText={Text(if(valid) "Choose a color that contrasts with the background." else "Enter # and six hex digits. The last valid color remains in use.")})
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     linkedMapOf("White" to "#FFFFFF","Cream" to "#FFF1CE","Blue" to "#BADDFF","Black" to "#161616").forEach {(name,color) ->
      FilterChip(selected=row.color.equals(color,true),onClick={hex=color;update(row.copy(color=color))},enabled=enabled,label={Text(name)})
     }
    }
   }
  }
  Text("Each slot is one visual line. Long content shrinks to fit; very long text is shortened with an ellipsis. Empty slots are hidden. Gothic JP and Mincho JP are bundled Japanese fonts, with separate regular and bold files.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Button(onClick=onSave,enabled=enabled && dirty,modifier=Modifier.fillMaxWidth()) {Text(if(dirty) "Save line layout" else "Line layout saved")}
  TextButton(onClick={onChange(Typography())},enabled=enabled) {Text("Reset line layout")}
 }
}

@Composable
private fun ChoiceMenu(label: String,selected: String,options: List<String>,enabled: Boolean,onSelect: (String)->Unit) {
 var open by remember {mutableStateOf(false)}
 Column {
  Text(label,style=MaterialTheme.typography.labelLarge)
  Box {
   OutlinedButton(onClick={open=true},enabled=enabled,modifier=Modifier.fillMaxWidth()) {
    Text(selected,Modifier.weight(1f));Icon(Icons.Outlined.ArrowDropDown,null)
   }
   DropdownMenu(expanded=open,onDismissRequest={open=false}) {
    options.forEach {option -> DropdownMenuItem(text={Text(option)},onClick={onSelect(option);open=false})}
   }
  }
 }
}
@Composable
private fun EditorSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,enabled: Boolean,onChange: (Float)->Unit) {
 Column {
  Row {Text(label,Modifier.weight(1f));Text(value.roundToInt().toString())}
  Slider(value=value,onValueChange=onChange,valueRange=range,enabled=enabled,modifier=Modifier.semantics {contentDescription=label})
 }
}
