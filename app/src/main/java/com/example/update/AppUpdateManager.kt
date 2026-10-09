package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.compositionLocalOf
import com.example.BuildConfig
import com.example.ui.games.OWEN_TAG_WEB_URL
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

val LocalAppUpdateManager = compositionLocalOf<AppUpdateManager?> { null }

enum class UpdateChannel(val id: String, val label: String, val description: String) {
    STABLE("stable", "Stable", "Verified production releases & live patches"),
    BETA("beta", "Beta", "Early access features & experimental game tuning"),
    NIGHTLY("nightly", "Nightly", "Bleeding-edge daily builds & instant OTA sync");

    companion object {
        fun fromId(id: String): UpdateChannel = entries.find { it.id == id } ?: STABLE
    }
}

data class AppReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val otaRevision: Int = 1,
    val channel: String = "stable",
    val releaseTitle: String = "Arcade 10 Live Update",
    val releaseNotes: List<String> = emptyList(),
    val apkDownloadUrl: String = "",
    val liveAnnouncement: String = "",
    val featuredGameId: String = "snake",
    val scoreMultiplier: Int = 1,
    val isMandatory: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis()
)

sealed class UpdateCheckStatus {
    data object Idle : UpdateCheckStatus()
    data class Checking(val stepMessage: String) : UpdateCheckStatus()
    data class UpToDate(val versionName: String, val otaRevision: Int, val checkedAt: Long) : UpdateCheckStatus()
    data class UpdateAvailable(val release: AppReleaseInfo) : UpdateCheckStatus()
    data class DownloadingApk(val release: AppReleaseInfo, val progressPercent: Int) : UpdateCheckStatus()
    data class AutoUpdatedJustNow(val release: AppReleaseInfo, val updatedAt: Long) : UpdateCheckStatus()
    data class Error(val message: String, val checkedAt: Long = System.currentTimeMillis()) : UpdateCheckStatus()
}

data class AutoUpdateSettings(
    val autoCheckOnLaunch: Boolean = true,
    val autoInstallOtaPatches: Boolean = true,
    val backgroundAutoSync: Boolean = true,
    val autoRefreshWebGlAssets: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val channel: UpdateChannel = UpdateChannel.STABLE,
    val customManifestUrl: String = ""
)

/**
 * Manages automatic application updates, Over-The-Air (OTA) live configuration & patch sync via
 * Firebase Firestore and HTTP JSON manifests, WebGL asset cache invalidation, and periodic
 * background data synchronization.
 */
class AppUpdateManager(
    private val context: Context,
    private val onPeriodicDataSync: () -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var firestoreListener: ListenerRegistration? = null
    private var backgroundSyncJob: Job? = null
    private var networkCallbackRegistered = false

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AutoUpdateSettings> = _settings.asStateFlow()

    private val _installedVersionName = MutableStateFlow(loadEffectiveVersionName())
    val installedVersionName: StateFlow<String> = _installedVersionName.asStateFlow()

    private val _installedOtaRevision = MutableStateFlow(loadAppliedOtaRevision())
    val installedOtaRevision: StateFlow<Int> = _installedOtaRevision.asStateFlow()

    private val _status = MutableStateFlow<UpdateCheckStatus>(
        UpdateCheckStatus.UpToDate(
            versionName = _installedVersionName.value,
            otaRevision = _installedOtaRevision.value,
            checkedAt = prefs.getLong(KEY_LAST_CHECKED_AT, System.currentTimeMillis())
        )
    )
    val status: StateFlow<UpdateCheckStatus> = _status.asStateFlow()

    private val _lastCheckedAt = MutableStateFlow(prefs.getLong(KEY_LAST_CHECKED_AT, 0L))
    val lastCheckedAt: StateFlow<Long> = _lastCheckedAt.asStateFlow()

    private val _lastDataSyncAt = MutableStateFlow(System.currentTimeMillis())
    val lastDataSyncAt: StateFlow<Long> = _lastDataSyncAt.asStateFlow()

    private val _isOnline = MutableStateFlow(checkIsOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _liveAnnouncement = MutableStateFlow(
        prefs.getString(KEY_LIVE_ANNOUNCEMENT, "") ?: ""
    )
    val liveAnnouncement: StateFlow<String> = _liveAnnouncement.asStateFlow()

    private val _featuredGameId = MutableStateFlow(
        prefs.getString(KEY_FEATURED_GAME_ID, "snake") ?: "snake"
    )
    val featuredGameId: StateFlow<String> = _featuredGameId.asStateFlow()

    private val _webGlBuildVersionTag = MutableStateFlow(
        prefs.getString(KEY_WEBGL_ETAG, "v1.0-live") ?: "v1.0-live"
    )
    val webGlBuildVersionTag: StateFlow<String> = _webGlBuildVersionTag.asStateFlow()

    private val _webGlCacheBustToken = MutableStateFlow(
        prefs.getLong(KEY_WEBGL_CACHE_BUST, 0L)
    )
    val webGlCacheBustToken: StateFlow<Long> = _webGlCacheBustToken.asStateFlow()

    private val _releaseHistory = MutableStateFlow(defaultReleaseHistory())
    val releaseHistory: StateFlow<List<AppReleaseInfo>> = _releaseHistory.asStateFlow()

    init {
        registerNetworkCallback()
        attachFirestoreReleaseListener()
        startBackgroundAutoSyncLoop()

        if (_settings.value.autoCheckOnLaunch) {
            checkForUpdates(isAutomatic = true)
        }
    }

    private fun loadSettings(): AutoUpdateSettings {
        return AutoUpdateSettings(
            autoCheckOnLaunch = prefs.getBoolean(KEY_AUTO_CHECK_LAUNCH, true),
            autoInstallOtaPatches = prefs.getBoolean(KEY_AUTO_INSTALL_OTA, true),
            backgroundAutoSync = prefs.getBoolean(KEY_BG_AUTO_SYNC, true),
            autoRefreshWebGlAssets = prefs.getBoolean(KEY_AUTO_REFRESH_WEBGL, true),
            wifiOnlyDownloads = prefs.getBoolean(KEY_WIFI_ONLY, false),
            channel = UpdateChannel.fromId(prefs.getString(KEY_UPDATE_CHANNEL, "stable") ?: "stable"),
            customManifestUrl = prefs.getString(KEY_CUSTOM_MANIFEST_URL, "") ?: ""
        )
    }

    private fun loadEffectiveVersionName(): String {
        val baseVersion = BuildConfig.VERSION_NAME.ifBlank { "1.0" }
        val savedVersion = prefs.getString(KEY_APPLIED_VERSION_NAME, null)
        return if (!savedVersion.isNullOrBlank() && compareVersions(savedVersion, baseVersion) >= 0) {
            savedVersion
        } else {
            baseVersion
        }
    }

    private fun loadAppliedOtaRevision(): Int {
        return prefs.getInt(KEY_APPLIED_OTA_REVISION, 3)
    }

    fun updateSettings(transform: (AutoUpdateSettings) -> AutoUpdateSettings) {
        val previous = _settings.value
        val updated = transform(previous)
        _settings.value = updated
        prefs.edit()
            .putBoolean(KEY_AUTO_CHECK_LAUNCH, updated.autoCheckOnLaunch)
            .putBoolean(KEY_AUTO_INSTALL_OTA, updated.autoInstallOtaPatches)
            .putBoolean(KEY_BG_AUTO_SYNC, updated.backgroundAutoSync)
            .putBoolean(KEY_AUTO_REFRESH_WEBGL, updated.autoRefreshWebGlAssets)
            .putBoolean(KEY_WIFI_ONLY, updated.wifiOnlyDownloads)
            .putString(KEY_UPDATE_CHANNEL, updated.channel.id)
            .putString(KEY_CUSTOM_MANIFEST_URL, updated.customManifestUrl.trim())
            .apply()

        if (previous.channel != updated.channel) {
            attachFirestoreReleaseListener()
            checkForUpdates(isAutomatic = false)
        }
        if (previous.backgroundAutoSync != updated.backgroundAutoSync) {
            startBackgroundAutoSyncLoop()
        }
    }

    /**
     * Checks all active update sources:
     * 1. Custom JSON manifest URL (if configured)
     * 2. Firebase Firestore `app_releases` collection
     * 3. Remote WebGL build ETag/Last-Modified header for Owen's Tag
     */
    fun checkForUpdates(isAutomatic: Boolean = false) {
        scope.launch {
            _status.value = UpdateCheckStatus.Checking(
                if (isAutomatic) "Auto-checking cloud release channels..."
                else "Contacting release server & Firestore..."
            )
            delay(450)

            val now = System.currentTimeMillis()
            _lastCheckedAt.value = now
            prefs.edit().putLong(KEY_LAST_CHECKED_AT, now).apply()

            // Also trigger live leaderboard & data sync
            onPeriodicDataSync()
            _lastDataSyncAt.value = now

            // 1. Check WebGL asset headers if enabled
            if (_settings.value.autoRefreshWebGlAssets) {
                checkWebGlBuildUpdate()
            }

            // 2. Check custom manifest URL if provided
            val customUrl = _settings.value.customManifestUrl.trim()
            if (customUrl.isNotBlank()) {
                val manifestRelease = fetchCustomManifestRelease(customUrl)
                if (manifestRelease != null) {
                    handleDiscoveredRelease(manifestRelease)
                    return@launch
                }
            }

            // 3. Check Firestore cloud release document
            val firestoreRelease = fetchFirestoreLatestRelease(_settings.value.channel)
            if (firestoreRelease != null) {
                handleDiscoveredRelease(firestoreRelease)
                return@launch
            }

            // 4. Up to date
            _status.value = UpdateCheckStatus.UpToDate(
                versionName = _installedVersionName.value,
                otaRevision = _installedOtaRevision.value,
                checkedAt = now
            )
        }
    }

    private suspend fun checkWebGlBuildUpdate() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(OWEN_TAG_WEB_URL)
                .head()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val etag = response.header("ETag")
                        ?: response.header("Last-Modified")
                        ?: "HTTP-${response.code}"
                    val cleanTag = etag.replace("\"", "").take(22)
                    val previousTag = prefs.getString(KEY_WEBGL_ETAG, null)
                    if (cleanTag.isNotBlank() && cleanTag != previousTag) {
                        prefs.edit()
                            .putString(KEY_WEBGL_ETAG, cleanTag)
                            .putLong(KEY_WEBGL_CACHE_BUST, System.currentTimeMillis())
                            .apply()
                        _webGlBuildVersionTag.value = cleanTag
                        _webGlCacheBustToken.value = System.currentTimeMillis()
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore transient offline errors during WebGL header check
        }
    }

    private suspend fun fetchCustomManifestRelease(url: String): AppReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                // Support both standard Arcade10 manifest format and GitHub Releases API format
                val versionName = json.optString("versionName")
                    .ifBlank { json.optString("tag_name").removePrefix("v") }
                    .ifBlank { return@withContext null }
                val versionCode = json.optInt("versionCode", BuildConfig.VERSION_CODE + 1)
                val otaRevision = json.optInt("otaRevision", _installedOtaRevision.value + 1)
                val title = json.optString("releaseTitle")
                    .ifBlank { json.optString("name", "Arcade 10 v$versionName") }

                val notesList = mutableListOf<String>()
                val notesArray = json.optJSONArray("releaseNotes")
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        val line = notesArray.optString(i)
                        if (line.isNotBlank()) notesList.add(line)
                    }
                } else {
                    val bodyText = json.optString("body")
                    if (bodyText.isNotBlank()) {
                        notesList.addAll(bodyText.lines().map { it.trim().removePrefix("- ").removePrefix("* ") }.filter { it.isNotBlank() }.take(5))
                    }
                }

                var apkUrl = json.optString("apkDownloadUrl")
                if (apkUrl.isBlank()) {
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        apkUrl = assets.optJSONObject(0)?.optString("browser_download_url") ?: ""
                    }
                }

                AppReleaseInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    otaRevision = otaRevision,
                    channel = _settings.value.channel.id,
                    releaseTitle = title,
                    releaseNotes = notesList.ifEmpty { listOf("Updated game assets and performance improvements") },
                    apkDownloadUrl = apkUrl,
                    liveAnnouncement = json.optString("liveAnnouncement", ""),
                    featuredGameId = json.optString("featuredGameId", "snake"),
                    scoreMultiplier = json.optInt("scoreMultiplier", 1),
                    isMandatory = json.optBoolean("isMandatory", false),
                    publishedAt = json.optLong("publishedAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFirestore(): FirebaseFirestore? {
        return try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isNotEmpty()) apps.last() else FirebaseApp.initializeApp(context)
            if (app != null) FirebaseFirestore.getInstance(app) else null
        } catch (_: Exception) {
            null
        }
    }

    fun attachFirestoreReleaseListener() {
        firestoreListener?.remove()
        firestoreListener = null

        val db = resolveFirestore() ?: return
        val docId = "latest_${_settings.value.channel.id}"

        try {
            firestoreListener = db.collection(COLLECTION_APP_RELEASES)
                .document(docId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val release = parseFirestoreSnapshot(snapshot.data ?: return@addSnapshotListener)
                    scope.launch {
                        handleDiscoveredRelease(release)
                    }
                }
        } catch (_: Exception) {
        }
    }

    private suspend fun fetchFirestoreLatestRelease(channel: UpdateChannel): AppReleaseInfo? =
        withContext(Dispatchers.IO) {
            val db = resolveFirestore() ?: return@withContext null
            try {
                val docId = "latest_${channel.id}"
                val task = db.collection(COLLECTION_APP_RELEASES).document(docId).get()
                var waited = 0
                while (!task.isComplete && waited < 4000) {
                    delay(100)
                    waited += 100
                }
                if (task.isSuccessful && task.result != null && task.result.exists()) {
                    val data = task.result.data
                    if (data != null) {
                        return@withContext parseFirestoreSnapshot(data)
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun parseFirestoreSnapshot(data: Map<String, Any>): AppReleaseInfo {
        val versionName = (data["versionName"] as? String)?.ifBlank { "1.2.0" } ?: "1.2.0"
        val versionCode = (data["versionCode"] as? Number)?.toInt() ?: 2
        val otaRevision = (data["otaRevision"] as? Number)?.toInt() ?: 4
        val channel = (data["channel"] as? String) ?: "stable"
        val title = (data["releaseTitle"] as? String) ?: "Arcade 10 Cloud OTA Update"
        val rawNotes = (data["releaseNotes"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val apkUrl = (data["apkDownloadUrl"] as? String) ?: ""
        val announcement = (data["liveAnnouncement"] as? String) ?: ""
        val featuredGame = (data["featuredGameId"] as? String) ?: "snake"
        val multiplier = (data["scoreMultiplier"] as? Number)?.toInt() ?: 1
        val mandatory = (data["isMandatory"] as? Boolean) ?: false
        val publishedAt = (data["publishedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

        return AppReleaseInfo(
            versionName = versionName,
            versionCode = versionCode,
            otaRevision = otaRevision,
            channel = channel,
            releaseTitle = title,
            releaseNotes = rawNotes.ifEmpty {
                listOf("Live OTA configuration & arcade enhancements")
            },
            apkDownloadUrl = apkUrl,
            liveAnnouncement = announcement,
            featuredGameId = featuredGame,
            scoreMultiplier = multiplier,
            isMandatory = mandatory,
            publishedAt = publishedAt
        )
    }

    private fun handleDiscoveredRelease(release: AppReleaseInfo) {
        // Add to release history if not already present
        val currentHistory = _releaseHistory.value
        if (currentHistory.none { it.versionName == release.versionName && it.otaRevision == release.otaRevision }) {
            _releaseHistory.value = listOf(release) + currentHistory
        }

        val isNewerVersion = compareVersions(release.versionName, _installedVersionName.value) > 0
        val isNewerOta = release.otaRevision > _installedOtaRevision.value

        if (isNewerVersion || isNewerOta) {
            if (_settings.value.autoInstallOtaPatches && release.apkDownloadUrl.isBlank()) {
                // Automatically apply the OTA update right away!
                applyOtaUpdateNow(release, wasAutomatic = true)
            } else {
                _status.value = UpdateCheckStatus.UpdateAvailable(release)
            }
        } else {
            // Update live announcement/featured game even if on same revision
            if (release.liveAnnouncement.isNotBlank()) {
                _liveAnnouncement.value = release.liveAnnouncement
                prefs.edit().putString(KEY_LIVE_ANNOUNCEMENT, release.liveAnnouncement).apply()
            }
            _status.value = UpdateCheckStatus.UpToDate(
                versionName = _installedVersionName.value,
                otaRevision = _installedOtaRevision.value,
                checkedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Applies an Over-The-Air (OTA) live update immediately, updating version state,
     * live announcement banners, featured game, and refreshing WebGL/leaderboard caches.
     */
    fun applyOtaUpdateNow(release: AppReleaseInfo, wasAutomatic: Boolean = false) {
        scope.launch {
            if (!wasAutomatic) {
                _status.value = UpdateCheckStatus.Checking("Applying OTA Patch v${release.versionName} (Rev ${release.otaRevision})...")
                delay(650)
            }

            _installedVersionName.value = release.versionName
            _installedOtaRevision.value = release.otaRevision
            _liveAnnouncement.value = release.liveAnnouncement
            _featuredGameId.value = release.featuredGameId
            val now = System.currentTimeMillis()
            _webGlCacheBustToken.value = now

            prefs.edit()
                .putString(KEY_APPLIED_VERSION_NAME, release.versionName)
                .putInt(KEY_APPLIED_OTA_REVISION, release.otaRevision)
                .putString(KEY_LIVE_ANNOUNCEMENT, release.liveAnnouncement)
                .putString(KEY_FEATURED_GAME_ID, release.featuredGameId)
                .putLong(KEY_WEBGL_CACHE_BUST, now)
                .putLong(KEY_LAST_CHECKED_AT, now)
                .apply()

            // Add to history if missing
            val existing = _releaseHistory.value
            if (existing.none { it.versionName == release.versionName && it.otaRevision == release.otaRevision }) {
                _releaseHistory.value = listOf(release) + existing
            }

            onPeriodicDataSync()
            _lastDataSyncAt.value = now

            _status.value = UpdateCheckStatus.AutoUpdatedJustNow(
                release = release,
                updatedAt = now
            )
        }
    }

    /**
     * Downloads an APK update package via Android's system DownloadManager or opens the release URL.
     */
    fun downloadAndInstallApk(release: AppReleaseInfo) {
        val url = release.apkDownloadUrl.trim()
        if (url.isBlank()) {
            applyOtaUpdateNow(release, wasAutomatic = false)
            return
        }

        scope.launch {
            try {
                if (!url.endsWith(".apk", ignoreCase = true)) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    return@launch
                }

                _status.value = UpdateCheckStatus.DownloadingApk(release, 5)
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (dm == null) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    return@launch
                }

                val fileName = "Arcade10-v${release.versionName}.apk"
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle("Arcade 10 Update v${release.versionName}")
                    .setDescription("Downloading update package...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(!_settings.value.wifiOnlyDownloads)
                    .setAllowedOverRoaming(true)

                val downloadId = dm.enqueue(request)
                var downloading = true
                var polls = 0

                while (downloading && polls < 120) {
                    delay(500)
                    polls++
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    dm.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val st = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_RUNNING
                            val soFar = if (bytesIdx >= 0) cursor.getLong(bytesIdx) else 0L
                            val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L

                            if (total > 0) {
                                val pct = ((soFar * 100L) / total).toInt().coerceIn(5, 99)
                                _status.value = UpdateCheckStatus.DownloadingApk(release, pct)
                            }

                            if (st == DownloadManager.STATUS_SUCCESSFUL || st == DownloadManager.STATUS_FAILED) {
                                downloading = false
                            }
                        } else {
                            downloading = false
                        }
                    }
                }

                // Also apply OTA metadata
                applyOtaUpdateNow(release, wasAutomatic = false)
            } catch (e: Exception) {
                _status.value = UpdateCheckStatus.Error(
                    e.localizedMessage ?: "Failed to download APK update"
                )
            }
        }
    }

    /**
     * Publishes a new Cloud OTA Release to Firebase Firestore (`app_releases/latest_<channel>`)
     * and immediately broadcasts it so all connected devices auto-update in real time.
     */
    fun publishCloudOtaRelease(
        versionName: String,
        releaseTitle: String,
        releaseNotesText: String,
        liveAnnouncement: String,
        featuredGameId: String,
        apkDownloadUrl: String = ""
    ) {
        scope.launch {
            val nextOtaRevision = _installedOtaRevision.value + 1
            val notes = releaseNotesText.lines()
                .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("Live OTA arcade update & leaderboard sync improvements") }

            val newRelease = AppReleaseInfo(
                versionName = versionName.trim().removePrefix("v").ifBlank { _installedVersionName.value },
                versionCode = BuildConfig.VERSION_CODE + nextOtaRevision,
                otaRevision = nextOtaRevision,
                channel = _settings.value.channel.id,
                releaseTitle = releaseTitle.trim().ifBlank { "Arcade 10 Live OTA Update" },
                releaseNotes = notes,
                apkDownloadUrl = apkDownloadUrl.trim(),
                liveAnnouncement = liveAnnouncement.trim(),
                featuredGameId = featuredGameId.ifBlank { "snake" },
                scoreMultiplier = 2,
                isMandatory = false,
                publishedAt = System.currentTimeMillis()
            )

            // Push to Firestore if connected
            val db = resolveFirestore()
            if (db != null) {
                val docId = "latest_${_settings.value.channel.id}"
                val payload = mapOf(
                    "versionName" to newRelease.versionName,
                    "versionCode" to newRelease.versionCode,
                    "otaRevision" to newRelease.otaRevision,
                    "channel" to newRelease.channel,
                    "releaseTitle" to newRelease.releaseTitle,
                    "releaseNotes" to newRelease.releaseNotes,
                    "apkDownloadUrl" to newRelease.apkDownloadUrl,
                    "liveAnnouncement" to newRelease.liveAnnouncement,
                    "featuredGameId" to newRelease.featuredGameId,
                    "scoreMultiplier" to newRelease.scoreMultiplier,
                    "isMandatory" to newRelease.isMandatory,
                    "publishedAt" to newRelease.publishedAt
                )
                try {
                    db.collection(COLLECTION_APP_RELEASES)
                        .document(docId)
                        .set(payload, SetOptions.merge())
                } catch (_: Exception) {
                }
            }

            handleDiscoveredRelease(newRelease)
        }
    }

    fun dismissAutoUpdateBanner() {
        _status.value = UpdateCheckStatus.UpToDate(
            versionName = _installedVersionName.value,
            otaRevision = _installedOtaRevision.value,
            checkedAt = _lastCheckedAt.value.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
    }

    fun clearLiveAnnouncement() {
        _liveAnnouncement.value = ""
        prefs.edit().putString(KEY_LIVE_ANNOUNCEMENT, "").apply()
    }

    fun forceRefreshWebGlCache() {
        val now = System.currentTimeMillis()
        _webGlCacheBustToken.value = now
        _webGlBuildVersionTag.value = "OTA-${now.toString().takeLast(5)}"
        prefs.edit()
            .putLong(KEY_WEBGL_CACHE_BUST, now)
            .putString(KEY_WEBGL_ETAG, _webGlBuildVersionTag.value)
            .apply()
    }

    private fun startBackgroundAutoSyncLoop() {
        backgroundSyncJob?.cancel()
        if (!_settings.value.backgroundAutoSync) return

        backgroundSyncJob = scope.launch {
            while (isActive) {
                delay(30_000L) // Auto-sync every 30 seconds
                if (_settings.value.backgroundAutoSync && checkIsOnline()) {
                    val now = System.currentTimeMillis()
                    onPeriodicDataSync()
                    _lastDataSyncAt.value = now
                    if (_settings.value.autoRefreshWebGlAssets) {
                        checkWebGlBuildUpdate()
                    }
                    val firestoreRelease = fetchFirestoreLatestRelease(_settings.value.channel)
                    if (firestoreRelease != null) {
                        handleDiscoveredRelease(firestoreRelease)
                    }
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) return
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val wasOffline = !_isOnline.value
                    _isOnline.value = true
                    if (wasOffline && _settings.value.backgroundAutoSync) {
                        // Immediately auto-sync leaderboards & check for updates when network reconnects
                        checkForUpdates(isAutomatic = true)
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkIsOnline()
                }
            })
            networkCallbackRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun checkIsOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.removePrefix("v").split(".", "-").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.removePrefix("v").split(".", "-").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    private fun defaultReleaseHistory(): List<AppReleaseInfo> {
        return listOf(
            AppReleaseInfo(
                versionName = "1.2.0",
                versionCode = 3,
                otaRevision = 3,
                channel = "stable",
                releaseTitle = "Auto-Update Engine & Lyria 3 AI Soundtrack",
                releaseNotes = listOf(
                    "Added real-time Cloud OTA Auto-Update Engine with background sync",
                    "Added custom 8-bit chiptune synthesizer & Lyria 3 AI Music Studio for all games",
                    "Added automatic WebGL asset cache invalidation & network reconnect auto-sync",
                    "Added Global Top 10 Firestore leaderboards with instant live updates"
                ),
                publishedAt = System.currentTimeMillis() - 60_000L
            ),
            AppReleaseInfo(
                versionName = "1.1.0",
                versionCode = 2,
                otaRevision = 2,
                channel = "stable",
                releaseTitle = "Owen's Tag Arena & Global Leaderboards",
                releaseNotes = listOf(
                    "Added Owen's Tag survival arena with dual Native 60fps & Unity WebGL modes",
                    "Integrated Firebase Firestore Global Top 10 Hall of Fame across all games",
                    "Added player callsign customization and offline score synchronization"
                ),
                publishedAt = System.currentTimeMillis() - 86_400_000L
            ),
            AppReleaseInfo(
                versionName = "1.0.0",
                versionCode = 1,
                otaRevision = 1,
                channel = "stable",
                releaseTitle = "Arcade 10 Initial Launch",
                releaseNotes = listOf(
                    "Launched 10 classic retro arcade games with neon CRT styling",
                    "Local Room database persistence for personal bests and games played"
                ),
                publishedAt = System.currentTimeMillis() - 172_800_000L
            )
        )
    }

    fun release() {
        firestoreListener?.remove()
        backgroundSyncJob?.cancel()
    }

    companion object {
        private const val PREFS_NAME = "arcade_10_auto_update_prefs"
        private const val COLLECTION_APP_RELEASES = "app_releases"

        private const val KEY_AUTO_CHECK_LAUNCH = "auto_check_on_launch"
        private const val KEY_AUTO_INSTALL_OTA = "auto_install_ota_patches"
        private const val KEY_BG_AUTO_SYNC = "background_auto_sync"
        private const val KEY_AUTO_REFRESH_WEBGL = "auto_refresh_webgl_assets"
        private const val KEY_WIFI_ONLY = "wifi_only_downloads"
        private const val KEY_UPDATE_CHANNEL = "update_channel"
        private const val KEY_CUSTOM_MANIFEST_URL = "custom_manifest_url"

        private const val KEY_APPLIED_VERSION_NAME = "applied_version_name"
        private const val KEY_APPLIED_OTA_REVISION = "applied_ota_revision"
        private const val KEY_LAST_CHECKED_AT = "last_checked_at"
        private const val KEY_LIVE_ANNOUNCEMENT = "live_announcement"
        private const val KEY_FEATURED_GAME_ID = "featured_game_id"
        private const val KEY_WEBGL_ETAG = "webgl_etag"
        private const val KEY_WEBGL_CACHE_BUST = "webgl_cache_bust"
    }
}
