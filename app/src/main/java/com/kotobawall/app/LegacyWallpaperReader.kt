package com.kotobawall.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat

/** Compatibility boundary for the API 24-32 wallpaper-reading permission model. */
object LegacyWallpaperReader {
 fun supportsSdk(sdk: Int): Boolean = sdk in 24..32

 // compileSdk 35 annotates getWallpaperFile with MANAGE_EXTERNAL_STORAGE or
 // READ_WALLPAPER_INTERNAL. Those annotations do not describe API 24-32,
 // where READ_EXTERNAL_STORAGE is documented as sufficient:
 // https://developer.android.com/reference/android/app/WallpaperManager#getWallpaperFile(int)
 // Suppress ONLY these legacy calls, never project-wide. Both guards below
 // must remain inside this helper so no future caller can bypass them.
 @SuppressLint("MissingPermission")
 fun open(context: Context): ParcelFileDescriptor? {
  check(supportsSdk(Build.VERSION.SDK_INT)) {
   "Android 13+ blocks normal access to another app’s current wallpaper. Choose the original image with Photo Picker, or reuse Last used."
  }
  if(ContextCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
   throw SecurityException("Storage permission is needed on Android 12 and earlier to import the current wallpaper. Photo Picker works without it.")
  }
  val manager=WallpaperManager.getInstance(context)
  return manager.getWallpaperFile(WallpaperManager.FLAG_LOCK)
   ?: manager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
 }
}
