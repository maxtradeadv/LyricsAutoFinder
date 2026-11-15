package com.maxsave.lyricsautofinder

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class LyricsNotificationListener : NotificationListenerService() {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null
    private var callback: MediaController.Callback? = null
    private var lastKey = ""
    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { choose(it ?: emptyList()) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            manager?.addOnActiveSessionsChangedListener(listener, ComponentName(this, LyricsNotificationListener::class.java), main)
            refresh()
            status("Menunggu musik diputar…")
        } catch (_: Exception) { status("Periksa izin Notification Access.") }
    }
    override fun onListenerDisconnected() {
        try { manager?.removeOnActiveSessionsChangedListener(listener) } catch (_: Exception) {}
        detach(); super.onListenerDisconnected()
    }
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) = refresh()

    private fun refresh() {
        try { choose(manager?.getActiveSessions(ComponentName(this, LyricsNotificationListener::class.java)) ?: emptyList()) }
        catch (_: Exception) { status("Aktifkan Notification Access.") }
    }
    private fun choose(list: List<MediaController>) {
        val chosen = list.firstOrNull { c ->
            !c.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() && c.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: list.firstOrNull { !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() }
        if (chosen == null) { playback(false, 0L, 1f); return }
        if (controller?.sessionToken != chosen.sessionToken) attach(chosen)
        metadata(chosen.metadata); playback(chosen.playbackState)
    }
    private fun attach(c: MediaController) {
        detach(); controller = c
        val cb = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = this@LyricsNotificationListener.metadata(metadata)
            override fun onPlaybackStateChanged(state: PlaybackState?) = playback(state)
        }
        callback = cb
        try { c.registerCallback(cb, main) } catch (_: Exception) {}
        metadata(c.metadata); playback(c.playbackState)
    }
    private fun detach() {
        try { if (controller != null && callback != null) controller?.unregisterCallback(callback!!) } catch (_: Exception) {}
        controller = null; callback = null
    }
    private fun metadata(m: MediaMetadata?) {
        if (m == null) return
        val title = m.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = (m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST))?.trim().orEmpty()
        val album = m.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim().orEmpty()
        val duration = m.getLong(MediaMetadata.METADATA_KEY_DURATION) / 1000
        if (title.isBlank() || artist.isBlank()) return
        val key = "$artist|$title|$album"
        prefs().edit().putString("artist", artist).putString("title", title).apply()
        if (key == lastKey) return
        lastKey = key; status("Mencari lirik: $artist — $title")
        io.execute { lookup(artist, title, album, duration) }
    }
    private fun playback(s: PlaybackState?) {
        if (s != null) playback(s.state == PlaybackState.STATE_PLAYING, s.position.coerceAtLeast(0), s.playbackSpeed)
    }
    private fun playback(playing: Boolean, position: Long, speed: Float) {
        prefs().edit().putBoolean("playing", playing).putLong("position", position).putFloat("speed", speed)
            .putLong("updatedAt", android.os.SystemClock.elapsedRealtime()).apply()
    }
    private fun lookup(artist: String, title: String, album: String, duration: Long) {
        try {
            val q = "track_name=${enc(title)}&artist_name=${enc(artist)}" +
                (if (album.isNotBlank()) "&album_name=${enc(album)}" else "") +
                (if (duration in 1..3600) "&duration=$duration" else "")
            var item = getObject("https://lrclib.net/api/get?$q")
            if (item == null) {
                val arr = getArray("https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}")
                if (arr != null && arr.length() > 0) item = arr.optJSONObject(0)
            }
            if (item == null) { saveLyrics("", "Lirik tidak ditemukan di LRCLIB."); return }
            val synced = item.optString("syncedLyrics").takeUnless { it == "null" || it.isBlank() }
            val plain = item.optString("plainLyrics").takeUnless { it == "null" || it.isBlank() }
            val text = synced ?: plain
            if (text == null) saveLyrics("", "Lirik belum tersedia.")
            else prefs().edit().putString("lyrics", text).putBoolean("synced", synced != null)
                .putString("status", if (synced != null) "Lirik bertimestamp ditemukan" else "Lirik ditemukan (tanpa sinkronisasi)").apply()
        } catch (_: Exception) { saveLyrics("", "Gagal mengambil lirik. Periksa internet.") }
    }
    private fun connection(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10000; readTimeout = 12000
        setRequestProperty("User-Agent", "LyricsAutoFinder/2.0 (Android)")
        setRequestProperty("Accept", "application/json")
    }
    private fun getObject(url: String): JSONObject? {
        val c = connection(url)
        return try { if (c.responseCode == 200) JSONObject(c.inputStream.bufferedReader().use { it.readText() }) else null } finally { c.disconnect() }
    }
    private fun getArray(url: String): JSONArray? {
        val c = connection(url)
        return try { if (c.responseCode == 200) JSONArray(c.inputStream.bufferedReader().use { it.readText() }) else null } finally { c.disconnect() }
    }
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun status(s: String) { prefs().edit().putString("status", s).apply() }
    private fun saveLyrics(s: String, msg: String) { prefs().edit().putString("lyrics", s).putBoolean("synced", false).putString("status", msg).apply() }
    companion object { const val PREFS = "lyrics_auto_finder" }
}
