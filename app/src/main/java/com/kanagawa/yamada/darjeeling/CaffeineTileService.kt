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
    
    private fun createTextIcon(text: String): android.graphics.drawable.Icon {
        val size = 100
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 60f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val y = (size / 2f) + (bounds.height() / 2f)
        canvas.drawText(text, size / 2f, y, paint)
        return android.graphics.drawable.Icon.createWithBitmap(bitmap)
    }
    
    private fun updateTileState() {
        val tile = qsTile ?: return
        when (CaffeineService.currentState) {
            0 -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "Off"
                tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_caffeine)
            }
            1 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("5 minutes")
                tile.icon = createTextIcon("5")
            }
            2 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("10 minutes")
                tile.icon = createTextIcon("10")
            }
            3 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("15 minutes")
                tile.icon = createTextIcon("15")
            }
            4 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = getCountdownText("30 minutes")
                tile.icon = createTextIcon("30")
            }
            5 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Darjeeling"
                tile.subtitle = "Unlimited"
                tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_caffeine)
            }
        }
        tile.updateTile()
    }
}
