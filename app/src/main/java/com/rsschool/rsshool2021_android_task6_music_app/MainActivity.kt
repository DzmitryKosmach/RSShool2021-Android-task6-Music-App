package com.rsschool.rsshool2021_android_task6_music_app

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.load
import com.rsschool.rsshool2021_android_task6_music_app.databinding.ActivityMainBinding
import com.rsschool.rsshool2021_android_task6_music_app.model.data.Track
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::bind)
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var mediaBrowser: MediaBrowserCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding.textViewSongText.movementMethod = ScrollingMovementMethod()

        mainViewModel.trackStateFlow.onEach(::renderTrack).launchIn(lifecycleScope)

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks,
            null
        )
    }

    public override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    public override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    public override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {

            mediaBrowser.sessionToken.also { token ->

                val mediaController = MediaControllerCompat(
                    this@MainActivity,
                    token
                )

                MediaControllerCompat.setMediaController(
                    this@MainActivity,
                    mediaController
                )
            }
            buildTransportControls()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }

    fun buildTransportControls() {
        val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
        binding.buttonPlay.apply {
            setOnClickListener {
                mediaController.transportControls.play()
            }
        }

        binding.buttonPause.apply {
            setOnClickListener {
                mediaController.transportControls.pause()
            }
        }

        binding.buttonStop.apply {
            setOnClickListener {
                mediaController.transportControls.stop()
            }
        }

        binding.buttonNext.apply {
            setOnClickListener {
                mediaController.transportControls.skipToNext()
            }
        }

        binding.buttonPrev.apply {
            setOnClickListener {
                mediaController.transportControls.skipToPrevious()
            }
        }
        mediaController.registerCallback(controllerCallback)
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (metadata == null) return
            val track = Track(
                metadata.getString(METADATA_KEY_TITLE), metadata.getString(
                    METADATA_KEY_ARTIST
                ), metadata.getString(
                    METADATA_KEY_DISPLAY_ICON_URI
                ), metadata.getString(
                    METADATA_KEY_MEDIA_URI
                ), metadata.getLong(
                    METADATA_KEY_DURATION
                ).toInt()
            )
            mainViewModel.setTrack(track)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state == null) return
            val currentState = state.state
            binding.buttonPlay.isSelected =
                currentState == PlaybackStateCompat.STATE_PLAYING ||
                    currentState == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT ||
                    currentState == PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS
        }
    }

    private fun renderTrack(track: Track) {
        binding.imageView.load(track.bitmapUri)
        binding.textViewSongName.text = track.artist
        binding.textViewSongText.text = track.title
    }
}
