package com.maxsave.lyricsautofinder

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var song: TextView
    private lateinit var state: TextView
    private lateinit var lyric: TextView
    private lateinit var scroller: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private var raw = ""
    private var parsed: List<Pair<Long, String>> = emptyList()
    private var active = -2
    private val tick = object : Runnable { override fun run() { updateScreen(); handler.postDelayed(this, 500) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,14); setBackgroundColor(Color.rgb(13,17,23)) }
        root.addView(TextView(this).apply { text="LYRICS AUTO FINDER"; textSize=21f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(0,8,0,18) }, LinearLayout.LayoutParams(-1,-2))
        root.addView(Button(this).apply { text="AKTIFKAN NOTIFICATION ACCESS"; setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }, LinearLayout.LayoutParams(-1,-2))
        song = TextView(this).apply { text="Menunggu metadata lagu…"; textSize=18f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER; setTextColor(Color.rgb(100,190,255)); setPadding(4,18,4,6) }
        root.addView(song, LinearLayout.LayoutParams(-1,-2))
        state = TextView(this).apply { textSize=12f; gravity=Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(4,2,4,14) }
        root.addView(state, LinearLayout.LayoutParams(-1,-2))
        scroller = ScrollView(this)
        lyric = TextView(this).apply { text="Lirik akan tampil otomatis setelah lagu terdeteksi."; textSize=22f; gravity=Gravity.CENTER; setLineSpacing(12f,1.12f); setTextColor(Color.WHITE); setPadding(10,24,10,40) }
        scroller.addView(lyric, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroller, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply { text="AUTO DETECT • INTERNET LYRICS • LRC SYNC"; textSize=10f; gravity=Gravity.CENTER; setTextColor(Color.GRAY); setPadding(0,12,0,0) })
        setContentView(root)
    }
    override fun onResume() { super.onResume(); handler.removeCallbacks(tick); handler.post(tick) }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }

    private fun updateScreen() {
        val p = getSharedPreferences(LyricsNotificationListener.PREFS, MODE_PRIVATE)
        val artist = p.getString("artist", "").orEmpty(); val title = p.getString("title", "").orEmpty()
        song.text = if (artist.isNotBlank() && title.isNotBlank()) "$artist\n$title" else "Menunggu metadata lagu…"
        state.text = p.getString("status", "Putar musik dari player yang mendukung metadata media.").orEmpty()
        val current = p.getString("lyrics", "").orEmpty()
        if (current != raw) { raw=current; parsed=parseLrc(raw); active=-2; if (parsed.isEmpty()) lyric.text=raw.ifBlank { "Lirik akan tampil otomatis setelah lagu terdeteksi." }; scroller.post { scroller.scrollTo(0,0) } }
        if (parsed.isEmpty()) return
        val position = if (p.getBoolean("playing", false)) {
            val base=p.getLong("position",0L); val elapsed=android.os.SystemClock.elapsedRealtime()-p.getLong("updatedAt",android.os.SystemClock.elapsedRealtime())
            base+(elapsed*p.getFloat("speed",1f)).toLong()
        } else p.getLong("position",0L)
        val line=parsed.indexOfLast { it.first<=position }; if (line==active) return; active=line
        val b=SpannableStringBuilder(); var start=-1; var end=-1
        parsed.forEachIndexed { i,pair -> val offset=b.length; b.append(pair.second); if(i==line){start=offset;end=b.length}; if(i!=parsed.lastIndex)b.append("\n\n") }
        if(start>=0 && end>start) b.setSpan(ForegroundColorSpan(Color.rgb(75,190,255)),start,end,SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        lyric.text=b
        if(start>=0) scroller.post { val layout=lyric.layout; if(layout!=null){val y=layout.getLineTop(layout.getLineForOffset(start));scroller.smoothScrollTo(0,(y-scroller.height/2).coerceAtLeast(0))} }
    }
    private fun parseLrc(text:String):List<Pair<Long,String>> {
        val result=mutableListOf<Pair<Long,String>>()
        val regex=Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\](.*)")
        text.lines().forEach { line ->
            val m=regex.matchEntire(line) ?: return@forEach
            val min=m.groupValues[1].toLongOrNull()?:0L; val sec=m.groupValues[2].toLongOrNull()?:0L; val f=m.groupValues[3]
            val ms=when(f.length){1->(f.toLongOrNull()?:0)*100;2->(f.toLongOrNull()?:0)*10;3->f.toLongOrNull()?:0;else->0}
            result.add((min*60000+sec*1000+ms) to m.groupValues[4].trim())
        }
        return result.sortedBy { it.first }
    }
}
