package com.nexusblock.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class AdBlockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
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
        // Delay tile update slightly to let service state change
        qsTile?.let { tile ->
            tile.state = if (running) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.state = if (NexusVpnService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Ad Block"
            tile.updateTile()
        }
    }
}
