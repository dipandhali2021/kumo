package com.kotobawall.app

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Appearance offered by the top-bar switcher: follow Android, or force light or dark. */
enum class ThemeMode(val storageKey: String,val label: String) {
 System("system","System theme"),
 Light("light","Light mode"),
 Dark("dark","Dark mode");
 /** Switcher order: system, light, dark, and back to system. */
 fun next(): ThemeMode = when(this) {
  ThemeMode.System -> ThemeMode.Light
  ThemeMode.Light -> ThemeMode.Dark
  ThemeMode.Dark -> ThemeMode.System
 }
 companion object {
  fun from(storageKey: String?): ThemeMode = values().firstOrNull {it.storageKey==storageKey} ?: ThemeMode.System
 }
}
/** Compose-observable wrapper around one small preference, so the choice survives restarts. */
class ThemeController(context: Context) {
 private val preferences=context.applicationContext.getSharedPreferences("kumo_appearance",Context.MODE_PRIVATE)
 var mode: ThemeMode by mutableStateOf(ThemeMode.from(preferences.getString("theme_mode",null)))
  private set
 fun select(value: ThemeMode) {
  mode=value
  preferences.edit().putString("theme_mode",value.storageKey).apply()
 }
 /** Moves to the next appearance and reports it, so callers can confirm the change. */
 fun advance(): ThemeMode {select(mode.next());return mode}
}
@Composable
fun rememberThemeController(): ThemeController {
 val context=LocalContext.current
 return remember(context) {ThemeController(context)}
}
/** System keeps following Android's own setting, including changes made while Kumo is open. */
@Composable
fun ThemeController.isDark(): Boolean = when(mode) {
 ThemeMode.System -> isSystemInDarkTheme()
 ThemeMode.Light -> false
 ThemeMode.Dark -> true
}
