package com.kotobawall.app

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

class RotationWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
 override suspend fun doWork(): Result {
  val repo=(applicationContext as KotobaApplication).repository
  return try {
   repo.apply(advance=true,scheduled=true)
   Result.success()
  } catch(e: CancellationException) { throw e }
  catch(e: Exception) {
   try { repo.edit { it.copy(lastError=e.message ?: "Automatic update failed") } }
   catch(c: CancellationException) { throw c }
   catch(_: Exception) { /* Preserve the original result if storage is unavailable. */ }
   if(e is IOException && runAttemptCount<3) Result.retry() else Result.failure()
  }
 }
}
object RotationSchedule {
 private const val NAME="kotoba_rotation"
 fun set(context: Context,hours: Int) {
  val manager=WorkManager.getInstance(context)
  if(hours==0) { manager.cancelUniqueWork(NAME); return }
  require(hours in listOf(6,12,24))
  val work=PeriodicWorkRequestBuilder<RotationWorker>(hours.toLong(),TimeUnit.HOURS)
   .setInitialDelay(hours.toLong(),TimeUnit.HOURS)
   .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
   .build()
  manager.enqueueUniquePeriodicWork(NAME,ExistingPeriodicWorkPolicy.UPDATE,work)
 }
}
