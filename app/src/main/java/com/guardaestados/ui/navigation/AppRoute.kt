package com.guardaestados.ui.navigation

import androidx.annotation.StringRes
import com.guardaestados.R

sealed class AppRoute(
    val route: String,
    @param:StringRes val labelRes: Int
) {
    data object Home : AppRoute("inicio", R.string.nav_home)
    data object States : AppRoute("estados", R.string.nav_states)
    data object Settings : AppRoute("configuracion", R.string.nav_settings)
}
