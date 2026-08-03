package com.guardaestados.ui

import androidx.compose.runtime.Composable
import com.guardaestados.ui.navigation.AppNavigation
import com.guardaestados.ui.theme.GuardaEstadosTheme

@Composable
fun GuardaEstadosApp() {
    GuardaEstadosTheme {
        AppNavigation()
    }
}
