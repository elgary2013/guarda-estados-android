package com.guardaestados.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import com.guardaestados.R

sealed class AppRoute(
    val route: String,
    @param:StringRes val labelRes: Int
) {
    data object Home : AppRoute("inicio", R.string.nav_home)
    data object States : AppRoute("estados", R.string.nav_states)
    data object Settings : AppRoute("configuracion", R.string.nav_settings)
    data object ImagePreview : AppRoute("vista-previa?imageUri={imageUri}", R.string.preview_title) {
        const val ImageUriArgument = "imageUri"

        fun createRoute(imageUri: String): String {
            return "vista-previa?imageUri=${Uri.encode(imageUri)}"
        }
    }
}
