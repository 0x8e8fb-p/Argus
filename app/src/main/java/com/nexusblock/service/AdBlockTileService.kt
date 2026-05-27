package com.nexusblock.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.N)
class AdBlockTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        observeJob?.cancel()
        observeJob = scope.launch {
            NexusVpnService.runningState.collect { running ->
                qsTile?.let { tile ->
                    tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = "Ad Block"
                    tile.updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val running = NexusVpnService.isRunning
        val intent = Intent(this, NexusVpnService::class.java).apply {
            action = if (running) NexusVpnService.ACTION_STOP else NexusVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // State refresh is handled by onStartListening + collected flow above.
    }
}
