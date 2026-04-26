package com.example.lab4

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class VideoActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var playerContainer: ViewGroup
    private lateinit var btnSelect: Button
    private lateinit var etUrl: EditText
    private lateinit var btnPlayUrl: Button
    private lateinit var btnFullscreen: ImageView

    private var player: ExoPlayer? = null
    private var currentUri: Uri? = null
    private var normalHeight = 0
    private var isFullscreen = false

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                currentUri = result.data?.data
                currentUri?.let { playVideo(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        playerView = findViewById(R.id.playerView)
        playerContainer = findViewById(R.id.playerContainer)
        btnSelect = findViewById(R.id.btnSelectVideo)
        etUrl = findViewById(R.id.etUrlVideo)
        btnPlayUrl = findViewById(R.id.btnPlayUrlVideo)
        btnFullscreen = findViewById(R.id.btnFullscreen)

        normalHeight = (260 * resources.displayMetrics.density).toInt()

        initPlayer()
        setupButtons()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    private fun setupButtons() {
        btnSelect.setOnClickListener { pickVideo() }

        btnPlayUrl.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.startsWith("http")) {
                playVideo(Uri.parse(url))
            } else {
                Toast.makeText(this, "Некоректний URL", Toast.LENGTH_SHORT).show()
            }
        }

        btnFullscreen.setOnClickListener {
            if (isFullscreen) exitFullscreen()
            else enterFullscreen()
        }
    }

    private fun playVideo(uri: Uri) {
        currentUri = uri
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        videoPicker.launch(intent)
    }

    private fun enterFullscreen() {
        isFullscreen = true

        btnSelect.visibility = View.GONE
        etUrl.visibility = View.GONE
        btnPlayUrl.visibility = View.GONE

        playerContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        playerContainer.requestLayout()

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun exitFullscreen() {
        isFullscreen = false

        btnSelect.visibility = View.VISIBLE
        etUrl.visibility = View.VISIBLE
        btnPlayUrl.visibility = View.VISIBLE

        playerContainer.layoutParams.height = normalHeight
        playerContainer.requestLayout()

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullscreen()
        } else {
            exitFullscreen()
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
