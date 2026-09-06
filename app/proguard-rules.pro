-keep class com.kotobawall.app.RotationWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
# AndroidViewModel factories create these constructors reflectively.
-keepclassmembers class com.kotobawall.app.WallViewModel { public <init>(android.app.Application); }
-keepclassmembers class com.kotobawall.app.WallpaperBrowserViewModel { public <init>(android.app.Application); }
