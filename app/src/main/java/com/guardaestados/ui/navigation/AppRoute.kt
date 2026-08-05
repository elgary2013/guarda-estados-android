package com.guardaestados.ui.navigation

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.guardaestados.R

sealed class AppRoute(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int? = null
) {
    data object Home : AppRoute("inicio", R.string.nav_home, R.drawable.ic_nav_home)
    data object States : AppRoute("estados", R.string.nav_states, R.drawable.ic_nav_states)
    data object Saved : AppRoute("guardados", R.string.nav_saved, R.drawable.ic_nav_saved)
    data object Settings : AppRoute("configuracion", R.string.nav_settings, R.drawable.ic_nav_settings)
    data object VideoSplitter : AppRoute("dividir-video", R.string.video_splitter_title)
    data object ImagePreview : AppRoute("vista-previa?imageUri={imageUri}", R.string.preview_title) {
        const val ImageUriArgument = "imageUri"

        fun createRoute(imageUri: String): String {
            return "vista-previa?imageUri=${Uri.encode(imageUri)}"
        }
    }
    data object SavedImagePreview : AppRoute("guardado-vista-previa?savedImageUri={savedImageUri}", R.string.saved_preview_title) {
        const val SavedImageUriArgument = "savedImageUri"

        fun createRoute(imageUri: String): String {
            return "guardado-vista-previa?savedImageUri=${Uri.encode(imageUri)}"
        }
    }
}
