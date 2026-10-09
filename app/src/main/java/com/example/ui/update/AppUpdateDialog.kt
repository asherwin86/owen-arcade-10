package com.example.ui.update

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GameRegistry
import com.example.ui.components.HapticHelper
import com.example.ui.theme.ArcadeTheme
import com.example.update.AppReleaseInfo
import com.example.update.AppUpdateManager
import com.example.update.AutoUpdateSettings
import com.example.update.UpdateChannel
import com.example.update.UpdateCheckStatus

@Composable
fun AutoUpdateStatusChip(
    updateManager: AppUpdateManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val versionName by updateManager.installedVersionName.collectAsState()
    val status by updateManager.status.collectAsState()
    val settings by updateManager.settings.collectAsState()

    val isUpdateAvailable = status is UpdateCheckStatus.UpdateAvailable
    val isChecking = status is UpdateCheckStatus.Checking || status is UpdateCheckStatus.DownloadingApk

    val badgeColor = when {
        isUpdateAvailable -> colors.scoreGold
        isChecking -> colors.neonPurple
        settings.autoInstallOtaPatches || settings.backgroundAutoSync -> colors.neonCyan
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val badgeLabel = when (val st = status) {
        is UpdateCheckStatus.UpdateAvailable -> "⚡ UPDATE v${st.release.versionName}"
        is UpdateCheckStatus.Checking -> "SYNCING..."
        is UpdateCheckStatus.DownloadingApk -> "DL ${st.progressPercent}%"
        is UpdateCheckStatus.AutoUpdatedJustNow -> "✓ UPDATED v$versionName"
        else -> if (settings.autoInstallOtaPatches) "v$versionName • AUTO-UPDATE" else "v$versionName • MANUAL"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.14f))
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 2.dp)
            .testTag("auto_update_status_chip")
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(badgeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = badgeLabel,
            style = textStyles.hudLabel.copy(fontSize = 9.sp),
            color = badgeColor,
            maxLines = 1
        )
    }
}

@Composable
fun AutoUpdateNotificationBanner(
    updateManager: AppUpdateManager,
    onOpenUpdateCenter: () -> Unit,
    onPlayFeaturedGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val status by updateManager.status.collectAsState()
    val liveAnnouncement by updateManager.liveAnnouncement.collectAsState()
    val featuredGameId by updateManager.featuredGameId.collectAsState()

    when (val st = status) {
        is UpdateCheckStatus.UpdateAvailable -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF231C0D)),
                border = BorderStroke(1.5.dp, colors.scoreGold),
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("update_available_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = colors.scoreGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UPDATE AVAILABLE: v${st.release.versionName}",
                                style = textStyles.hudLabel.copy(fontSize = 11.sp),
                                color = colors.scoreGold
                            )
                            Text(
                                text = st.release.releaseTitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            HapticHelper.playClick(context)
                            updateManager.downloadAndInstallApk(st.release)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.scoreGold,
                            contentColor = Color(0xFF261900)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("banner_install_update_button")
                    ) {
                        Text(
                            text = "UPDATE NOW",
                            style = textStyles.hudLabel.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        is UpdateCheckStatus.AutoUpdatedJustNow -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2422)),
                border = BorderStroke(1.dp, colors.neonCyan.copy(alpha = 0.7f)),
                modifier = modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticHelper.playClick(context)
                        onOpenUpdateCenter()
                    }
                    .testTag("auto_updated_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Auto Updated",
                            tint = colors.neonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUTO-UPDATED TO v${st.release.versionName} (OTA REV ${st.release.otaRevision})",
                                style = textStyles.hudLabel.copy(fontSize = 10.sp),
                                color = colors.neonCyan
                            )
                            Text(
                                text = st.release.releaseTitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            HapticHelper.playClick(context)
                            updateManager.dismissAutoUpdateBanner()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        else -> {
            if (liveAnnouncement.isNotBlank()) {
                val featuredGame = remember(featuredGameId) { GameRegistry.getGame(featuredGameId) }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1128)),
                    border = BorderStroke(1.dp, colors.neonPurple.copy(alpha = 0.7f)),
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticHelper.playClick(context)
                            onPlayFeaturedGame(featuredGame.id)
                        }
                        .testTag("live_announcement_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "LIVE ARCADE BROADCAST • FEATURED: ${featuredGame.title.uppercase()}",
                                    style = textStyles.hudLabel.copy(fontSize = 9.sp),
                                    color = colors.neonPurple
                                )
                                Text(
                                    text = liveAnnouncement,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                HapticHelper.playClick(context)
                                updateManager.clearLiveAnnouncement()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Broadcast",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUpdateDialog(
    isOpen: Boolean,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    val settings by updateManager.settings.collectAsState()
    val installedVersion by updateManager.installedVersionName.collectAsState()
    val installedOtaRev by updateManager.installedOtaRevision.collectAsState()
    val status by updateManager.status.collectAsState()
    val lastCheckedAt by updateManager.lastCheckedAt.collectAsState()
    val lastDataSyncAt by updateManager.lastDataSyncAt.collectAsState()
    val isOnline by updateManager.isOnline.collectAsState()
    val webGlTag by updateManager.webGlBuildVersionTag.collectAsState()
    val releaseHistory by updateManager.releaseHistory.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.5.dp, colors.neonCyan.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("app_update_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(colors.neonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, colors.neonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Auto-Update Engine",
                                tint = colors.neonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ARCADE AUTO-UPDATE",
                                style = textStyles.gameHeaderTitle.copy(fontSize = 16.sp),
                                color = Color.White
                            )
                            Text(
                                text = "v$installedVersion (OTA REV $installedOtaRev) • ${settings.channel.label.uppercase()} CHANNEL",
                                style = textStyles.hudLabel.copy(fontSize = 10.sp),
                                color = colors.neonCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticHelper.playClick(context)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_update_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.surfaceElevated,
                    contentColor = colors.neonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = colors.neonCyan,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            HapticHelper.playClick(context)
                            selectedTab = 0
                        },
                        text = {
                            Text(
                                text = "AUTO-UPDATE",
                                style = textStyles.hudLabel.copy(fontSize = 10.sp),
                                color = if (selectedTab == 0) colors.neonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("update_tab_status")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            HapticHelper.playClick(context)
                            selectedTab = 1
                        },
                        text = {
                            Text(
                                text = "WHAT'S NEW",
                                style = textStyles.hudLabel.copy(fontSize = 10.sp),
                                color = if (selectedTab == 1) colors.neonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("update_tab_changelog")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            HapticHelper.playClick(context)
                            selectedTab = 2
                        },
                        text = {
                            Text(
                                text = "CLOUD OTA",
                                style = textStyles.hudLabel.copy(fontSize = 10.sp),
                                color = if (selectedTab == 2) colors.neonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("update_tab_cloud_ota")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> UpdateStatusAndSettingsTab(
                        settings = settings,
                        status = status,
                        installedVersion = installedVersion,
                        installedOtaRev = installedOtaRev,
                        lastCheckedAt = lastCheckedAt,
                        lastDataSyncAt = lastDataSyncAt,
                        isOnline = isOnline,
                        webGlTag = webGlTag,
                        onCheckNow = {
                            HapticHelper.playClick(context)
                            updateManager.checkForUpdates(isAutomatic = false)
                        },
                        onInstallRelease = { release ->
                            HapticHelper.playScore(context)
                            updateManager.downloadAndInstallApk(release)
                        },
                        onUpdateSettings = { transform ->
                            HapticHelper.playClick(context)
                            updateManager.updateSettings(transform)
                        },
                        onForceRefreshWebGl = {
                            HapticHelper.playClick(context)
                            updateManager.forceRefreshWebGlCache()
                        }
                    )

                    1 -> ReleaseNotesHistoryTab(
                        releaseHistory = releaseHistory,
                        installedVersion = installedVersion,
                        installedOtaRev = installedOtaRev,
                        onReapplyRelease = { release ->
                            HapticHelper.playScore(context)
                            updateManager.applyOtaUpdateNow(release, wasAutomatic = false)
                            selectedTab = 0
                        }
                    )

                    2 -> CloudOtaPublisherTab(
                        settings = settings,
                        installedVersion = installedVersion,
                        installedOtaRev = installedOtaRev,
                        onSaveCustomUrl = { url ->
                            HapticHelper.playClick(context)
                            updateManager.updateSettings { it.copy(customManifestUrl = url) }
                            updateManager.checkForUpdates(isAutomatic = false)
                            selectedTab = 0
                        },
                        onPublishCloudOta = { ver, title, notes, announcement, featuredGame, apkUrl ->
                            HapticHelper.playScore(context)
                            updateManager.publishCloudOtaRelease(
                                versionName = ver,
                                releaseTitle = title,
                                releaseNotesText = notes,
                                liveAnnouncement = announcement,
                                featuredGameId = featuredGame,
                                apkDownloadUrl = apkUrl
                            )
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusAndSettingsTab(
    settings: AutoUpdateSettings,
    status: UpdateCheckStatus,
    installedVersion: String,
    installedOtaRev: Int,
    lastCheckedAt: Long,
    lastDataSyncAt: Long,
    isOnline: Boolean,
    webGlTag: String,
    onCheckNow: () -> Unit,
    onInstallRelease: (AppReleaseInfo) -> Unit,
    onUpdateSettings: ((AutoUpdateSettings) -> AutoUpdateSettings) -> Unit,
    onForceRefreshWebGl: () -> Unit
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val scrollState = rememberScrollState()

    val relativeLastChecked = if (lastCheckedAt > 0L) {
        DateUtils.getRelativeTimeSpanString(
            lastCheckedAt,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        ).toString()
    } else {
        "Just now"
    }

    val relativeDataSync = DateUtils.getRelativeTimeSpanString(
        lastDataSyncAt,
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS
    ).toString()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Live Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            border = BorderStroke(
                1.dp,
                when (status) {
                    is UpdateCheckStatus.UpdateAvailable -> colors.scoreGold
                    is UpdateCheckStatus.Error -> Color(0xFFEF4444)
                    else -> colors.neonCyan.copy(alpha = 0.5f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (status) {
                                is UpdateCheckStatus.UpdateAvailable -> Icons.Default.NewReleases
                                is UpdateCheckStatus.Checking, is UpdateCheckStatus.DownloadingApk -> Icons.Default.CloudSync
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when (status) {
                                is UpdateCheckStatus.UpdateAvailable -> colors.scoreGold
                                is UpdateCheckStatus.Checking, is UpdateCheckStatus.DownloadingApk -> colors.neonPurple
                                else -> colors.neonCyan
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (val st = status) {
                                is UpdateCheckStatus.Checking -> st.stepMessage
                                is UpdateCheckStatus.DownloadingApk -> "Downloading v${st.release.versionName} (${st.progressPercent}%)"
                                is UpdateCheckStatus.UpdateAvailable -> "New Update Ready: v${st.release.versionName}"
                                is UpdateCheckStatus.AutoUpdatedJustNow -> "Auto-Updated to v${st.release.versionName}!"
                                is UpdateCheckStatus.Error -> st.message
                                else -> "App is Up to Date (v$installedVersion)"
                            },
                            style = textStyles.hudLabel.copy(fontSize = 12.sp),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (isOnline) Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color(0xFFEF4444).copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE",
                            style = textStyles.hudLabel.copy(fontSize = 9.sp),
                            color = if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                if (status is UpdateCheckStatus.Checking || status is UpdateCheckStatus.DownloadingApk) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = colors.neonCyan,
                        trackColor = colors.surfaceCard
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Checked: $relativeLastChecked • Data Sync: $relativeDataSync • WebGL: $webGlTag",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // If an update is available, show details + install button
                if (status is UpdateCheckStatus.UpdateAvailable) {
                    val rel = status.release
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141122), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = rel.releaseTitle,
                            style = textStyles.hudLabel.copy(fontSize = 11.sp),
                            color = colors.scoreGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        rel.releaseNotes.take(3).forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onInstallRelease(rel) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.scoreGold,
                                contentColor = Color(0xFF281D00)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("dialog_install_update_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (rel.apkDownloadUrl.isNotBlank()) "DOWNLOAD & INSTALL v${rel.versionName}"
                                else "INSTALL LIVE OTA UPDATE v${rel.versionName}",
                                fontWeight = FontWeight.Bold,
                                style = textStyles.hudLabel.copy(fontSize = 11.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCheckNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.neonCyan,
                            contentColor = Color(0xFF003730)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("check_for_updates_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Check Now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHECK & SYNC NOW",
                            style = textStyles.hudLabel.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onForceRefreshWebGl,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.neonPurple.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("refresh_webgl_assets_button")
                    ) {
                        Text(
                            text = "SYNC WEBGL",
                            style = textStyles.hudLabel.copy(fontSize = 10.sp),
                            color = colors.neonPurple
                        )
                    }
                }
            }
        }

        // Update Channel Selector
        Column {
            Text(
                text = "RELEASE CHANNEL",
                style = textStyles.hudLabel.copy(fontSize = 10.sp),
                color = colors.neonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UpdateChannel.entries.forEach { channel ->
                    val selected = settings.channel == channel
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateSettings { it.copy(channel = channel) } },
                        label = {
                            Text(
                                text = channel.label.uppercase(),
                                style = textStyles.hudLabel.copy(fontSize = 10.sp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.neonPink,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surfaceElevated,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = if (selected) colors.neonPink else colors.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("channel_chip_${channel.id}")
                    )
                }
            }
        }

        // Auto-Update Toggles
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AutoUpdateToggleRow(
                    title = "Auto-Check on App Launch",
                    subtitle = "Automatically check for updates and sync scores when Arcade 10 starts",
                    checked = settings.autoCheckOnLaunch,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(autoCheckOnLaunch = checked) }
                    },
                    testTag = "toggle_auto_check_launch"
                )

                AutoUpdateToggleRow(
                    title = "Auto-Install Cloud OTA Patches",
                    subtitle = "Apply live Firestore patches, game configs & hotfixes without restarting",
                    checked = settings.autoInstallOtaPatches,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(autoInstallOtaPatches = checked) }
                    },
                    testTag = "toggle_auto_install_ota"
                )

                AutoUpdateToggleRow(
                    title = "Background Live Sync (Every 30s)",
                    subtitle = "Continuously auto-update Global Top 10 leaderboards & release state",
                    checked = settings.backgroundAutoSync,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(backgroundAutoSync = checked) }
                    },
                    testTag = "toggle_background_sync"
                )

                AutoUpdateToggleRow(
                    title = "Auto-Update Unity WebGL Assets",
                    subtitle = "Check ETag headers and refresh Owen's Tag WebGL build automatically",
                    checked = settings.autoRefreshWebGlAssets,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(autoRefreshWebGlAssets = checked) }
                    },
                    testTag = "toggle_auto_refresh_webgl"
                )
            }
        }
    }
}

@Composable
private fun AutoUpdateToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = textStyles.hudLabel.copy(fontSize = 11.sp),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF003730),
                checkedTrackColor = colors.neonCyan,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceCard
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ReleaseNotesHistoryTab(
    releaseHistory: List<AppReleaseInfo>,
    installedVersion: String,
    installedOtaRev: Int,
    onReapplyRelease: (AppReleaseInfo) -> Unit
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 440.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(releaseHistory, key = { "${it.versionName}_${it.otaRevision}" }) { release ->
            val isCurrent = release.versionName == installedVersion && release.otaRevision == installedOtaRev
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                border = BorderStroke(
                    1.dp,
                    if (isCurrent) colors.neonCyan else colors.border
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (isCurrent) colors.neonCyan else colors.scoreGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v${release.versionName} • OTA REV ${release.otaRevision}",
                                style = textStyles.hudLabel.copy(fontSize = 12.sp),
                                color = if (isCurrent) colors.neonCyan else colors.scoreGold
                            )
                        }

                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .background(colors.neonCyan.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "INSTALLED",
                                    style = textStyles.hudLabel.copy(fontSize = 9.sp),
                                    color = colors.neonCyan
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onReapplyRelease(release) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = "APPLY",
                                    style = textStyles.hudLabel.copy(fontSize = 9.sp),
                                    color = colors.neonCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = release.releaseTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    release.releaseNotes.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudOtaPublisherTab(
    settings: AutoUpdateSettings,
    installedVersion: String,
    installedOtaRev: Int,
    onSaveCustomUrl: (String) -> Unit,
    onPublishCloudOta: (
        versionName: String,
        releaseTitle: String,
        releaseNotesText: String,
        liveAnnouncement: String,
        featuredGameId: String,
        apkDownloadUrl: String
    ) -> Unit
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val scrollState = rememberScrollState()

    var customUrlInput by remember(settings.customManifestUrl) {
        mutableStateOf(settings.customManifestUrl)
    }

    val suggestedNextVersion = remember(installedVersion, installedOtaRev) {
        val parts = installedVersion.split(".").map { it.toIntOrNull() ?: 0 }
        if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.${parts[2] + 1}"
        } else {
            "1.2.${installedOtaRev}"
        }
    }

    var newVersionName by remember(suggestedNextVersion) { mutableStateOf(suggestedNextVersion) }
    var releaseTitle by remember { mutableStateOf("Arcade 10 Live OTA Update") }
    var releaseNotesText by remember {
        mutableStateOf(
            "Updated game balancing and real-time leaderboard sync\nRefreshed Owen's Tag Arena & chiptune synth tracks"
        )
    }
    var liveAnnouncement by remember {
        mutableStateOf("⚡ Double High-Score Challenge Active! Compete in the Global Top 10!")
    }
    var selectedFeaturedGameId by remember { mutableStateOf("snake") }
    var apkDownloadUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Push Live Cloud OTA Update to Firestore
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            border = BorderStroke(1.dp, colors.neonPurple.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = colors.neonPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PUSH LIVE CLOUD OTA UPDATE",
                        style = textStyles.hudLabel.copy(fontSize = 11.sp),
                        color = colors.neonPurple
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Broadcast a live OTA update to Firebase Firestore (app_releases/latest_${settings.channel.id}). Connected devices auto-update immediately!",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newVersionName,
                        onValueChange = { newVersionName = it },
                        label = { Text("Version", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.neonCyan,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier
                            .weight(0.38f)
                            .testTag("ota_version_input")
                    )

                    OutlinedTextField(
                        value = releaseTitle,
                        onValueChange = { releaseTitle = it },
                        label = { Text("Update Title", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.neonCyan,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier
                            .weight(0.62f)
                            .testTag("ota_title_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = liveAnnouncement,
                    onValueChange = { liveAnnouncement = it },
                    label = { Text("Live Arcade Broadcast Banner", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonPurple,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ota_announcement_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "FEATURED GAME FOR THIS UPDATE",
                    style = textStyles.hudLabel.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(GameRegistry.games, key = { it.id }) { game ->
                        val isSelected = selectedFeaturedGameId == game.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFeaturedGameId = game.id },
                            label = {
                                Text(
                                    text = "${game.iconEmoji} ${game.title}",
                                    fontSize = 10.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.neonCyan,
                                selectedLabelColor = Color(0xFF003730),
                                containerColor = colors.surfaceCard,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = releaseNotesText,
                    onValueChange = { releaseNotesText = it },
                    label = { Text("Release Notes (one per line)", fontSize = 10.sp) },
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonCyan,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ota_notes_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apkDownloadUrl,
                    onValueChange = { apkDownloadUrl = it },
                    label = { Text("Optional Direct APK / Release URL", fontSize = 10.sp) },
                    placeholder = { Text("https://.../Arcade10-release.apk", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonCyan,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onPublishCloudOta(
                            newVersionName,
                            releaseTitle,
                            releaseNotesText,
                            liveAnnouncement,
                            selectedFeaturedGameId,
                            apkDownloadUrl
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.neonPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("publish_cloud_ota_button")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPLOY & AUTO-UPDATE NOW",
                        style = textStyles.hudLabel.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Custom Manifest Feed Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CUSTOM UPDATE MANIFEST / GITHUB RELEASES URL",
                    style = textStyles.hudLabel.copy(fontSize = 10.sp),
                    color = colors.neonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Optionally point Arcade 10 to a JSON update manifest or GitHub Releases API endpoint (e.g., https://api.github.com/repos/owner/repo/releases/latest).",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    placeholder = { Text("https://api.github.com/repos/.../releases/latest", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonCyan,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_manifest_url_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onSaveCustomUrl(customUrlInput) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.neonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("save_manifest_url_button")
                ) {
                    Text(
                        text = "SAVE FEED URL & CHECK",
                        style = textStyles.hudLabel.copy(fontSize = 10.sp),
                        color = colors.neonCyan
                    )
                }
            }
        }
    }
}
