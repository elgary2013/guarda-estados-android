package com.guardaestados

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.guardaestados.ui.GuardaEstadosApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val shouldAttemptAppOpenAd = savedInstanceState == null
        setContent {
            GuardaEstadosApp(shouldAttemptAppOpenAd = shouldAttemptAppOpenAd)
        }
    }
}
