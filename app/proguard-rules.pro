-keep class com.kumo.app.RotationWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
# AndroidViewModel factories create these constructors reflectively.
-keepclassmembers class com.kumo.app.WallViewModel { public <init>(android.app.Application); }
-keepclassmembers class com.kumo.app.WallpaperBrowserViewModel { public <init>(android.app.Application); }
