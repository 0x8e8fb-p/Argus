package com.nexusblock.data.worker

import android.content.Context
import android.content.SharedPreferences
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
    private val okHttpClient: OkHttpClient,
    private val sharedPreferences: SharedPreferences
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NexusBlock/Updater"
        private const val ETAG_PREFIX = "blocklist_etag_"

        /** Work output keys */
        const val KEY_UPDATED = "updated_sources"
        const val KEY_SKIPPED = "skipped_sources"
        const val KEY_FAILED = "failed_sources"
        const val KEY_TOTAL_RULES = "total_rules"

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
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting blocklist update")
        val updatedSources = mutableListOf<String>()
        val skippedSources = mutableListOf<String>()
        val failedSources = mutableListOf<String>()
        var totalRules = 0

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
                if (domains == null) {
                    // 304 Not Modified — already up to date
                    skippedSources.add(source.name)
                } else if (domains.isNotEmpty()) {
                    blocklistRepo.replaceSource(source.id, domains, source.defaultEnabled)
                    totalRules += domains.size
                    updatedSources.add("${source.name} (${domains.size})")
                    Log.i(TAG, "Updated ${source.id} with ${domains.size} domains")
                } else {
                    Log.w(TAG, "${source.id} returned no domains")
                    failedSources.add(source.name)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update ${source.id}", e)
                failedSources.add(source.name)
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
            totalRules += builtins.size
            updatedSources.add("Built-in (${builtins.size})")
            Log.i(TAG, "Updated built-in rules: ${builtins.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update built-in domains", e)
            failedSources.add("Built-in")
        }

        // Reload engine rules if VPN is running
        if (NexusVpnService.isRunning) {
            try {
                dnsEngine.reloadRules()
                Log.i(TAG, "DNS engine rules reloaded after blocklist update")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reload DNS engine rules", e)
            }
        }

        val output = workDataOf(
            KEY_UPDATED to updatedSources.joinToString(", "),
            KEY_SKIPPED to skippedSources.joinToString(", "),
            KEY_FAILED to failedSources.joinToString(", "),
            KEY_TOTAL_RULES to totalRules
        )

        return@withContext if (updatedSources.isNotEmpty() || skippedSources.isNotEmpty()) {
            Result.success(output)
        } else {
            Result.retry()
        }
    }

    private suspend fun fetchAndParse(
        sourceId: String,
        url: String,
        format: BlocklistFormat
    ): List<BlockedDomain>? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "NexusBlock/1.0")

        // Add If-None-Match header if we have a cached ETag
        val cachedEtag = sharedPreferences.getString(ETAG_PREFIX + sourceId, null)
        if (cachedEtag != null) {
            requestBuilder.header("If-None-Match", cachedEtag)
        }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 304) {
                Log.i(TAG, "$sourceId not modified (304), skipping")
                return@withContext null
            }
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }

            // Store ETag for future conditional requests
            response.header("ETag")?.let { etag ->
                sharedPreferences.edit().putString(ETAG_PREFIX + sourceId, etag).apply()
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
