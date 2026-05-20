package com.nexusblock.data.repository

import android.content.Context
import com.nexusblock.R
import com.nexusblock.data.model.BlockedDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RawBlocklistLoader {

    suspend fun loadBuiltinBlocklist(context: Context): List<BlockedDomain> = withContext(Dispatchers.IO) {
        val domains = mutableListOf<BlockedDomain>()
        val seen = HashSet<String>()

        context.resources.openRawResource(R.raw.blocklist_hosts).use { stream ->
            val parsed = BlocklistParsers.parseAdGuardFilter(stream.bufferedReader().readText())
            parsed.forEach { host ->
                if (host !in seen) {
                    seen.add(host)
                    domains.add(BlockedDomain(host = host, source = "oisd_builtin"))
                }
            }
        }

        domains
    }
}
