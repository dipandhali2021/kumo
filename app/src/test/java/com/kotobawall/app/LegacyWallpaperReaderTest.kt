package com.kotobawall.app
import org.junit.Assert.*
import org.junit.Test
class LegacyWallpaperReaderTest {
 @Test fun onlyLegacyAndroidCanReadSystemWallpaper() {
  assertFalse(LegacyWallpaperReader.supportsSdk(23))
  for(sdk in 24..32) assertTrue(LegacyWallpaperReader.supportsSdk(sdk))
  for(sdk in 33..40) assertFalse(LegacyWallpaperReader.supportsSdk(sdk))
 }
}
