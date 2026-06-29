package com.kanagawa.yamada.darjeeling

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build
import android.os.Handler
import android.os.Looper

class CaffeineTileService : TileService() {
    companion object {
        const val ACTION_UPDATE_TILE = "com.kanagawa.yamada.darjeeling.UPDATE_TILE"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (CaffeineService.currentState in 1..4) {
                updateTileState()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val updateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            updateTileState()
        }
    }

    override fun onClick() {
        super.onClick()
        
        CaffeineService.currentState = when (CaffeineService.currentState) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            4 -> 5
            5 -> 0
            else -> 0
        }
        
        updateTileState()
        
        val intent = Intent(this, CaffeineService::class.java)
        intent.action = "ACTION_APPLY_CURRENT_STATE"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val filter = android.content.IntentFilter(ACTION_UPDATE_TILE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }
        updateTileState()
        handler.post(ticker)
    }
    
    override fun onStopListening() {
        super.onStopListening()
        handler.removeCallbacks(ticker)
        try {
            unregisterReceiver(updateReceiver)
        } catch (e: Exception) {}
    }
    
    private fun getCountdownText(defaultMinutes: String): String {
        val remainingMillis = CaffeineService.endTime - System.currentTimeMillis()
        if (remainingMillis > 0) {
            val totalSeconds = remainingMillis / 1000
            val m = totalSeconds / 60
            val s = totalSeconds % 60
            return String.format("%02d:%02d", m, s)
        }
        return defaultMinutes
    }
    
    private fun updateTileState() {
        val tile = qsTile ?: return
        when (CaffeineService.currentState) {
            0 -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "Off"
            }
            1 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("5 minutes")
            }
            2 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("10 minutes")
            }
            3 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("15 minutes")
            }
            4 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("30 minutes")
            }
            5 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "Unlimited"
            }
        }
        tile.updateTile()
    }
}
