package com.guardaestados

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guardaestados.ui.GuardaEstadosApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val shouldAttemptAppOpenAd = savedInstanceState == null
        setContent {
            GuardaEstadosApp(shouldAttemptAppOpenAd = shouldAttemptAppOpenAd)
        }
    }
}
