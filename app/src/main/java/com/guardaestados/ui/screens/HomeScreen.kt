package com.guardaestados.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.ui.theme.BrandGlassCard
import com.guardaestados.ui.theme.BrandPrimaryButton
import com.guardaestados.ui.theme.LocalGuardaEstadosColors

@Composable
fun HomeScreen(
    folderSelectionState: FolderSelectionState,
    statusGalleryState: StatusGalleryState,
    onOpenStates: () -> Unit,
    onOpenFolderSettings: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenVideoSplitter: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val colors = LocalGuardaEstadosColors.current
    val needsFolder = folderSelectionState !is FolderSelectionState.Selected
    Surface(modifier = modifier.fillMaxSize(), color = colors.background) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x55245CFF), Color.Transparent), radius = 900f))) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                    .padding(top = 28.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(stringResource(R.string.home_title_visible), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = colors.title)
                Text(stringResource(R.string.home_modern_tagline), style = MaterialTheme.typography.titleMedium, color = colors.body)
                BrandGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp), shape = RoundedCornerShape(28.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = colors.active.copy(alpha = 0.16f), contentColor = colors.active) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(30.dp)) }
                        }
                        Text(stringResource(if (needsFolder) R.string.home_connect_states_folder_title else R.string.home_available_statuses_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.title)
                        Text(homeModernStatus(folderSelectionState, statusGalleryState), style = MaterialTheme.typography.bodyLarge, color = colors.body)
                        BrandPrimaryButton(stringResource(if (needsFolder) R.string.folder_action_select else R.string.home_primary_action), if (needsFolder) onOpenFolderSettings else onOpenStates, Modifier.fillMaxWidth())
                    }
                }
                Text(stringResource(R.string.home_quick_access_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.title)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeQuickCard(stringResource(R.string.nav_saved), stringResource(R.string.home_quick_saved_body), R.drawable.ic_app_guardados, onOpenSaved, Modifier.weight(1f))
                    HomeQuickCard(stringResource(R.string.video_splitter_title), stringResource(R.string.home_quick_split_body), R.drawable.ic_app_dividir, onOpenVideoSplitter, Modifier.weight(1f))
                }
                Text(stringResource(R.string.home_tools_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.title)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeToolCard(stringResource(R.string.preview_action_save_short), Icons.Filled.Download, Modifier.weight(1f))
                    HomeToolCard(stringResource(R.string.nav_split), Icons.Filled.ContentCut, Modifier.weight(1f))
                    HomeToolCard(stringResource(R.string.share_action_short), Icons.Filled.Share, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeQuickCard(title: String, body: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalGuardaEstadosColors.current
    BrandGlassCard(modifier.clickable(role = Role.Button, onClick = onClick), PaddingValues(16.dp), RoundedCornerShape(22.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(icon), null, tint = colors.activeAlt, modifier = Modifier.size(32.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.title)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.body)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = colors.active)
        }
    }
}

@Composable
private fun HomeToolCard(title: String, icon: ImageVector, modifier: Modifier) {
    val colors = LocalGuardaEstadosColors.current
    BrandGlassCard(modifier, PaddingValues(vertical = 16.dp, horizontal = 8.dp), RoundedCornerShape(20.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = colors.active, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.title)
        }
    }
}

@Composable
private fun homeModernStatus(folderState: FolderSelectionState, galleryState: StatusGalleryState): String = when {
    folderState !is FolderSelectionState.Selected -> stringResource(R.string.home_connect_states_folder_body)
    galleryState == StatusGalleryState.Loading -> stringResource(R.string.home_status_loading)
    galleryState == StatusGalleryState.Empty -> stringResource(R.string.home_status_empty)
    galleryState is StatusGalleryState.Content -> stringResource(R.string.home_status_ready_real)
    else -> stringResource(R.string.home_status_read_error)
}
