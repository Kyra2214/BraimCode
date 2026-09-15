package com.sandbox.app

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat

/**
 * Thin launcher wrapper that keeps the foreground execution service alive
 * independently from the Compose Activity lifecycle.
 */
class BrainCodeActivity : MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val serviceIntent = Intent(this, BrainCodeExecutionService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        super.onCreate(savedInstanceState)
    }
}
