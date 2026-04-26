package com.example.lab4

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AudioActivity : AppCompatActivity() {

    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnAddTracks: Button
    private lateinit var btnAddUrl: Button
    private lateinit var btnClearQueue: Button
    private lateinit var btnBack: Button
    private lateinit var btnLoop: Button
    private lateinit var tvQueueInfo: TextView
    private lateinit var rvPlaylist: RecyclerView

    private val playlist = mutableListOf<Uri>()
    private var currentIndex = 0
    private var loopEnabled = false

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var adapter: AudioTrackAdapter

    private val audioPicker =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris != null && uris.isNotEmpty()) {
                for (uri in uris) {
                    playlist.add(uri)
                    adapter.notifyItemInserted(playlist.size - 1)
                }

                if (playlist.size == uris.size) {
                    currentIndex = 0
                    loadTrack(playlist[0])
                }

                updateQueueInfo()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)

        ivCover = findViewById(R.id.ivCover)
        tvTitle = findViewById(R.id.tvTitle)
        tvArtist = findViewById(R.id.tvArtist)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvDuration = findViewById(R.id.tvDuration)
        btnPlay = findViewById(R.id.btnPlay)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        btnAddTracks = findViewById(R.id.btnAddTracks)
        btnAddUrl = findViewById(R.id.btnAddUrl)
        btnClearQueue = findViewById(R.id.btnClearQueue)
        btnBack = findViewById(R.id.btnBack)
        btnLoop = findViewById(R.id.btnLoop)
        tvQueueInfo = findViewById(R.id.tvQueueInfo)
        rvPlaylist = findViewById(R.id.rvPlaylist)

        btnBack.setOnClickListener { finish() }

        adapter = AudioTrackAdapter(
            this,
            playlist,
            onClick = { index ->
                currentIndex = index
                loadTrack(playlist[currentIndex])
                adapter.notifyDataSetChanged()
                updateQueueInfo()
            },
            getCurrentIndex = { currentIndex }
        )

        rvPlaylist.layoutManager = LinearLayoutManager(this)
        rvPlaylist.adapter = adapter

        enableDragAndSwipe()
        updateQueueInfo()

        btnAddTracks.setOnClickListener {
            audioPicker.launch(arrayOf("audio/*"))
        }

        btnAddUrl.setOnClickListener {
            showAddUrlDialog()
        }

        btnClearQueue.setOnClickListener {
            clearQueueExceptCurrent()
        }

        btnLoop.setOnClickListener {
            loopEnabled = !loopEnabled
            btnLoop.text = if (loopEnabled) "🔂" else "🔁"
        }

        btnPlay.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlay.text = "▶"
            } else {
                mediaPlayer?.start()
                btnPlay.text = "⏸"
                updateSeekBar()
            }
        }

        btnNext.setOnClickListener { skipTrack(true) }
        btnPrev.setOnClickListener { skipTrack(false) }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun showAddUrlDialog() {
        val input = EditText(this)
        input.hint = "https://example.com/music.mp3"

        AlertDialog.Builder(this)
            .setTitle("Додати трек за URL")
            .setView(input)
            .setPositiveButton("Додати") { _, _ ->
                val url = input.text.toString().trim()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    addTrackFromUrl(url)
                } else {
                    Toast.makeText(this, "Некоректний URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Відміна", null)
            .show()
    }

    private fun addTrackFromUrl(url: String) {
        val uri = Uri.parse(url)
        playlist.add(uri)
        adapter.notifyItemInserted(playlist.size - 1)

        if (playlist.size == 1) {
            currentIndex = 0
            loadTrack(uri)
        }

        updateQueueInfo()
    }

    private fun skipTrack(forward: Boolean) {
        if (playlist.isEmpty()) return

        if (!loopEnabled) removeCurrentTrack()

        if (playlist.isEmpty()) {
            clearPlayerCompletely()
            return
        }

        currentIndex = if (forward) {
            currentIndex % playlist.size
        } else {
            if (currentIndex == 0) playlist.size - 1 else currentIndex - 1
        }

        loadTrack(playlist[currentIndex])
        adapter.notifyDataSetChanged()
        updateQueueInfo()
    }

    private fun removeCurrentTrack() {
        if (playlist.isEmpty()) return

        if (playlist.size == 1) {
            clearPlayerCompletely()
            return
        }

        playlist.removeAt(currentIndex)

        if (currentIndex >= playlist.size) currentIndex = 0

        adapter.notifyDataSetChanged()
        updateQueueInfo()
    }

    private fun enableDragAndSwipe() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to = target.adapterPosition

                val item = playlist.removeAt(from)
                playlist.add(to, item)

                if (currentIndex == from) currentIndex = to
                else if (from < currentIndex && to >= currentIndex) currentIndex--
                else if (from > currentIndex && to <= currentIndex) currentIndex++

                adapter.notifyItemMoved(from, to)
                adapter.notifyDataSetChanged()
                updateQueueInfo()
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.adapterPosition

                if (pos == currentIndex) {
                    adapter.notifyItemChanged(pos)
                    return
                }

                playlist.removeAt(pos)
                if (pos < currentIndex) currentIndex--

                adapter.notifyDataSetChanged()
                updateQueueInfo()
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(rvPlaylist)
    }

    private fun clearQueueExceptCurrent() {
        if (playlist.isEmpty()) return

        val currentTrack = playlist[currentIndex]
        playlist.clear()
        playlist.add(currentTrack)
        currentIndex = 0

        adapter.notifyDataSetChanged()
        updateQueueInfo()
    }

    private fun clearPlayerCompletely() {
        playlist.clear()
        adapter.notifyDataSetChanged()
        currentIndex = 0

        releasePlayer()

        tvTitle.text = ""
        tvArtist.text = ""
        tvDuration.text = "00:00"
        tvCurrentTime.text = "00:00"
        seekBar.progress = 0
        ivCover.setImageResource(R.drawable.ic_music_placeholder)
        btnPlay.text = "▶"

        updateQueueInfo()
    }

    private fun loadTrack(uri: Uri) {
        releasePlayer()

        val retriever = MediaMetadataRetriever()

        val isUrl = uri.scheme == "http" || uri.scheme == "https"

        if (isUrl) {
            val raw = uri.lastPathSegment ?: uri.toString()
            val clean = raw.substringBefore('?').substringBeforeLast('.')
            tvTitle.text = clean
            tvArtist.text = "Unknown Artist"
            ivCover.setImageResource(R.drawable.ic_music_placeholder)
        } else {
            val retriever = MediaMetadataRetriever()
            var title: String? = null
            var artist: String? = null
            var art: ByteArray? = null

            try {
                retriever.setDataSource(this, uri)
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                art = retriever.embeddedPicture
            } catch (_: Exception) {}

            if (title.isNullOrBlank()) {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex("_display_name")
                    if (nameIndex >= 0 && it.moveToFirst()) {
                        title = it.getString(nameIndex)
                    }
                }
            }

            if (title.isNullOrBlank()) title = "Unknown Title"
            if (artist.isNullOrBlank()) artist = "Unknown Artist"

            tvTitle.text = title
            tvArtist.text = artist

            if (art != null) {
                ivCover.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.size))
            } else {
                ivCover.setImageResource(R.drawable.ic_music_placeholder)
            }
        }



        val art = retriever.embeddedPicture
        if (art != null) {
            ivCover.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.size))
        } else {
            ivCover.setImageResource(R.drawable.ic_music_placeholder)
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                if (uri.scheme == "http" || uri.scheme == "https") {
                    setDataSource(uri.toString())
                } else {
                    setDataSource(this@AudioActivity, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AudioActivity, "Не вдалося відкрити трек", Toast.LENGTH_SHORT).show()
                return
            }

            prepareAsync()

            setOnPreparedListener {
                seekBar.max = it.duration
                tvDuration.text = formatTime(it.duration)
                it.start()
                btnPlay.text = "⏸"
                updateSeekBar()
            }

            setOnCompletionListener {
                if (loopEnabled) {
                    seekTo(0)
                    start()
                } else {
                    removeCurrentTrack()
                    if (playlist.isNotEmpty()) {
                        loadTrack(playlist[currentIndex])
                    } else {
                        clearPlayerCompletely()
                    }
                }
            }
        }

        updateQueueInfo()
    }

    private fun updateQueueInfo() {
        val total = playlist.size
        tvQueueInfo.text = if (total == 0) {
            "Черга порожня"
        } else {
            "Трек ${currentIndex + 1} з $total"
        }
    }

    private fun updateSeekBar() {
        mediaPlayer?.let {
            seekBar.progress = it.currentPosition
            tvCurrentTime.text = formatTime(it.currentPosition)

            if (it.isPlaying) {
                handler.postDelayed({ updateSeekBar() }, 200)
            }
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun formatTime(ms: Int): String {
        val sec = ms / 1000
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
