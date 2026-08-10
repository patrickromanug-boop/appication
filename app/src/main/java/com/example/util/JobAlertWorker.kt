package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.preference.PreferencesManager
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.UserProfile
import com.example.ui.AppViewModel
import com.example.ui.MockJob
import com.example.util.CacheUtils.parseMockJobList
import com.example.util.CacheUtils.parseUserProfile
import com.example.util.CacheUtils.toJsonString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class JobAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "JobAlertWorker"
        private const val WORK_NAME_PERIODIC = "job_alert_sync_periodic_v1"
        private const val WORK_NAME_ONCE_ONLINE = "job_alert_sync_once_online_v1"

        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<JobAlertWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
                Log.d(TAG, "Periodic JobAlertWorker scheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule periodic JobAlertWorker: ${e.message}")
            }
        }

        fun checkNowWhenOnline(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val oneTimeRequest = OneTimeWorkRequestBuilder<JobAlertWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_ONCE_ONLINE,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
                Log.d(TAG, "OneTime JobAlertWorker checkNowWhenOnline enqueued")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue checkNowWhenOnline: ${e.message}")
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "JobAlertWorker doWork() started - Checking for new job uploads")
            val prefs = PreferencesManager(appContext)
            val client = SupabaseClient(appContext)

            val remoteJobs: List<MockJob> = if (client.isRealConfigActive) {
                when (val res = client.fetchJobs()) {
                    is SupabaseClient.ApiResult.Success -> res.data
                    else -> emptyList()
                }
            } else {
                emptyList()
            }

            val sourceList = if (remoteJobs.isNotEmpty()) {
                remoteJobs
            } else {
                val cached = prefs.cachedJobsJson
                if (!cached.isNullOrBlank()) {
                    try { parseMockJobList(cached) } catch (_: Exception) { emptyList() }
                } else {
                    emptyList()
                }
            }

            // Merge with custom uploaded jobs & exclude deleted IDs
            val deletedIds = prefs.deletedJobIds
            val filtered = sourceList.filter { it.id !in deletedIds }.toMutableList()

            val customJson = prefs.customUploadedJobsJson
            if (!customJson.isNullOrBlank()) {
                try {
                    val customJobs = parseMockJobList(customJson)
                    for (customJob in customJobs) {
                        if (customJob.id in deletedIds) continue
                        val existingIdx = filtered.indexOfFirst { it.id == customJob.id }
                        if (existingIdx >= 0) {
                            filtered[existingIdx] = customJob
                        } else {
                            filtered.add(0, customJob)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing custom jobs: ${e.message}")
                }
            }

            val mergedJobs = filtered.sortedByDescending { it.createdAt }
            if (mergedJobs.isEmpty()) {
                return@withContext Result.success()
            }

            val seedJobIds = setOf("1", "2", "3", "4", "5", "6", "7", "8")
            val notifiedSet = prefs.notifiedJobIds.toMutableSet()

            if (!prefs.hasCompletedInitialJobSync) {
                seedJobIds.forEach { notifiedSet.add(it) }
                prefs.notifiedJobIds = notifiedSet
                prefs.hasCompletedInitialJobSync = true
            }

            val newJobsToNotify = mutableListOf<MockJob>()
            mergedJobs.forEach { job ->
                if (job.status != "inactive" && !notifiedSet.contains(job.id)) {
                    newJobsToNotify.add(job)
                }
            }

            if (newJobsToNotify.isNotEmpty()) {
                val userProfileJson = prefs.cachedProfileJson
                val profile = if (!userProfileJson.isNullOrBlank()) {
                    try { parseUserProfile(userProfileJson) } catch (_: Exception) { null }
                } else {
                    null
                }

                val notifyAll = profile?.notifyAllJobs ?: true
                val notifyMatching = profile?.notifyMatchingPreferences ?: false

                var dispatchedCount = 0
                newJobsToNotify.forEach { job ->
                    val matchResult = AppViewModel.evaluateJobMatchStatic(job, profile)
                    val isTargeted = (profile != null) && notifyMatching && matchResult.isMatch
                    val shouldNotify = if (notifyMatching) {
                        matchResult.isMatch
                    } else {
                        notifyAll
                    }

                    if (shouldNotify) {
                        val dispatched = NotificationHelper.showJobAlertNotification(
                            appContext,
                            jobId = job.id,
                            title = job.title,
                            organization = job.organization,
                            location = job.location,
                            isTargetedMatch = isTargeted,
                            matchedReason = if (isTargeted) matchResult.matchedReason else null
                        )
                        if (dispatched) {
                            notifiedSet.add(job.id)
                            dispatchedCount++
                        }
                    } else {
                        notifiedSet.add(job.id)
                    }
                }

                if (dispatchedCount >= 2) {
                    NotificationHelper.showOfflineSummaryNotification(
                        appContext,
                        dispatchedCount
                    )
                }

                prefs.notifiedJobIds = notifiedSet
                prefs.cachedJobsJson = mergedJobs.toJsonString()
                prefs.cachedJobsTimestamp = System.currentTimeMillis()
                Log.d(TAG, "Dispatched $dispatchedCount new job notifications.")
            } else {
                Log.d(TAG, "No new unnotified jobs found.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "JobAlertWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
