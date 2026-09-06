package com.kotobawall.app

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CycleStatus(val enabled: Boolean=false,val running: Boolean=false,val error: String="")
object ScreenCycleController {
 private val mutable=MutableStateFlow(CycleStatus())
 val state=mutable.asStateFlow()
 private var initialized=false
 @Synchronized fun initialize(context: Context) {
  if(!initialized) {mutable.value=CycleStatus(enabled=preferences(context).getBoolean("enabled",false));initialized=true}
 }
 private fun preferences(context: Context)=context.getSharedPreferences("screen_cycle",Context.MODE_PRIVATE)
 fun isEnabled(context: Context)=preferences(context).getBoolean("enabled",false)
 fun start(context: Context) {
  initialize(context)
  check(NotificationManagerCompat.from(context).areNotificationsEnabled()) {"Allow Kotoba Wall notifications in Android settings before starting screen-off updates."}
  if(Build.VERSION.SDK_INT>=26) {
   val channel=context.getSystemService(NotificationManager::class.java).getNotificationChannel(ScreenCycleService.CHANNEL)
   check(channel==null || channel.importance!=NotificationManager.IMPORTANCE_NONE) {"Enable the Screen-off updates notification channel in Android settings."}
  }
  preferences(context).edit().putBoolean("enabled",true).apply();mutable.value=CycleStatus(enabled=true)
  try {ContextCompat.startForegroundService(context,Intent(context,ScreenCycleService::class.java))}
  catch(e: Exception) {stop(context);failure(e.message ?: "Android could not start screen-off updates.");throw e}
 }
 fun stop(context: Context) {
  preferences(context).edit().putBoolean("enabled",false).apply();mutable.value=CycleStatus();context.stopService(Intent(context,ScreenCycleService::class.java))
 }
 fun started() {mutable.value=CycleStatus(enabled=true,running=true)}
 fun stopped(context: Context) {mutable.value=mutable.value.copy(enabled=isEnabled(context),running=false)}
 fun failure(message: String) {mutable.value=mutable.value.copy(error=message)}
 fun clearedError() {mutable.value=mutable.value.copy(error="")}
}
class ScreenCycleService: Service() {
 companion object {
  const val CHANNEL="screen_off_updates"
  private const val STOP="com.kotobawall.app.STOP_CYCLE"
  private const val NOTIFICATION=81
 }
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private var updateJob: Job?=null
 private var registered=false
 private var ready=false
 private val receiver=object: BroadcastReceiver() {
  override fun onReceive(context: Context,intent: Intent) {
   if(intent.action!=Intent.ACTION_SCREEN_OFF || !ScreenCycleController.isEnabled(this@ScreenCycleService)) return
   if(updateJob?.isActive==true) return
   val pending=goAsync()
   var wake: PowerManager.WakeLock?=null
   try {
    wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"KotobaWall:ScreenOffRender")
    wake.acquire(15_000L)
   } catch(_: Exception) { }
   val heldWake=wake
   val job=scope.launch {
    try {
     if(ScreenCycleController.isEnabled(this@ScreenCycleService)) {
      val repo=(application as KotobaApplication).repository
      repo.apply(advance=true,screenCycle=true)
      ScreenCycleController.clearedError()
     }
    } catch(e: CancellationException) {throw e}
    catch(e: Exception) {ScreenCycleController.failure(e.message ?: "Screen-off wallpaper update failed.")}
   }
   updateJob=job
   job.invokeOnCompletion {
    try {if(heldWake?.isHeld==true) heldWake.release()} catch(_: RuntimeException) { }
    pending.finish()
   }
  }
 }
 override fun onCreate() {
  super.onCreate();ScreenCycleController.initialize(this)
  try {
   if(Build.VERSION.SDK_INT>=26) {
    getSystemService(NotificationManager::class.java).createNotificationChannel(
     NotificationChannel(CHANNEL,"Screen-off updates",NotificationManager.IMPORTANCE_LOW).apply {description="Visible while vocabulary wallpaper updates are active."}
    )
   }
   val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val stop=PendingIntent.getService(this,1,Intent(this,ScreenCycleService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val notification=NotificationCompat.Builder(this,CHANNEL)
    .setSmallIcon(R.drawable.ic_notification).setContentTitle("Kotoba Wall is active")
    .setContentText("Updating your vocabulary wallpaper on screen-off")
    .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW)
    .addAction(0,"Stop updates",stop).build()
   val type=if(Build.VERSION.SDK_INT>=34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
   ServiceCompat.startForeground(this,NOTIFICATION,notification,type)
   ContextCompat.registerReceiver(this,receiver,IntentFilter(Intent.ACTION_SCREEN_OFF),ContextCompat.RECEIVER_NOT_EXPORTED)
   registered=true;ready=true
  } catch(e: Exception) {ScreenCycleController.failure(e.message ?: "Screen-off updates could not start.");stopSelf()}
 }
 override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
  if(intent?.action==STOP) {ScreenCycleController.stop(this);return START_NOT_STICKY}
  if(!ready || !ScreenCycleController.isEnabled(this)) {stopSelf();return START_NOT_STICKY}
  ScreenCycleController.started();return START_STICKY
 }
 override fun onBind(intent: Intent?)=null
 override fun onDestroy() {
  if(registered) unregisterReceiver(receiver)
  scope.cancel();ServiceCompat.stopForeground(this,ServiceCompat.STOP_FOREGROUND_REMOVE)
  ScreenCycleController.stopped(this);super.onDestroy()
 }
}
