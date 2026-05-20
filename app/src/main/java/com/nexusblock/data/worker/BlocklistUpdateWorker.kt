package com.nexusblock.data.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nexusblock.Constants
import com.nexusblock.data.model.BlockedDomain
import com.nexusblock.data.repository.BuiltInBlockRules
import com.nexusblock.data.repository.BlocklistFormat
import com.nexusblock.data.repository.BlocklistParsers
import com.nexusblock.data.repository.BlocklistRepository
import com.nexusblock.data.repository.BlocklistSources
import com.nexusblock.engine.DnsFilterEngine
import com.nexusblock.service.NexusVpnService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class BlocklistUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val blocklistRepo: BlocklistRepository,
    private val dnsEngine: DnsFilterEngine,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NexusBlock/Updater"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BlocklistUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(24, TimeUnit.HOURS)
                .addTag(Constants.WORK_TAG_BLOCKLIST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_BLOCKLIST,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<BlocklistUpdateWorker>()
                .setConstraints(constraints)
                .addTag(Constants.WORK_TAG_BLOCKLIST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${Constants.WORK_TAG_BLOCKLIST}_initial",
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(Constants.WORK_TAG_BLOCKLIST)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting blocklist update")
        var successCount = 0
        var failureCount = 0

        for (source in BlocklistSources.remote) {
            val url = source.url ?: continue
            try {
                if (!blocklistRepo.isSourceEnabled(source.id, source.defaultEnabled)) {
                    Log.i(TAG, "Skipping disabled source ${source.id}")
                    continue
                }

                Log.i(TAG, "Fetching ${source.id} from $url")
                val domains = fetchAndParse(
                    sourceId = source.id,
                    url = url,
                    format = source.format
                )
                if (domains.isNotEmpty()) {
                    blocklistRepo.replaceSource(source.id, domains, source.defaultEnabled)
                    Log.i(TAG, "Updated ${source.id} with ${domains.size} domains")
                    successCount++
                } else {
                    Log.w(TAG, "${source.id} returned no domains")
                    failureCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update ${source.id}", e)
                failureCount++
            }
        }

        // Add built-in ad domains
        try {
            val builtins = getBuiltinRules()
            blocklistRepo.replaceSource(
                BlocklistSources.builtin.id,
                builtins,
                BlocklistSources.builtin.defaultEnabled
            )
            Log.i(TAG, "Updated built-in rules: ${builtins.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update built-in domains", e)
        }

        // Reload engine rules if VPN is running
        if (NexusVpnService.isRunning) {
            try {
                dnsEngine.reloadBlocklists()
                Log.i(TAG, "DNS engine rules reloaded after blocklist update")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reload DNS engine rules", e)
            }
        }

        if (successCount > 0) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    private suspend fun fetchAndParse(
        sourceId: String,
        url: String,
        format: BlocklistFormat
    ): List<BlockedDomain> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NexusBlock/1.0")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty body")
            val hosts = when (format) {
                BlocklistFormat.HOSTS -> BlocklistParsers.parseHostFile(body)
                BlocklistFormat.ADGUARD -> BlocklistParsers.parseAdGuardFilter(body)
            }
            hosts.map { BlockedDomain(host = it, source = sourceId) }
        }
    }

    private fun getBuiltinRules(): List<BlockedDomain> {
        return BuiltInBlockRules.emergencyRules()
            .map { it.copy(source = BlocklistSources.builtin.id) }
    }
}
