package com.rsschool.rsshool2021_android_task6_music_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.rsschool.rsshool2021_android_task6_music_app.model.MusicRepository
import com.rsschool.rsshool2021_android_task6_music_app.model.data.Track

class MediaPlaybackService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    private var notificationManager: NotificationManager? = null

    private lateinit var player: Player
    private val playerListener = PlayerEventListener()

    private lateinit var musicRepository: MusicRepository
    private val metadataBuilder = MediaMetadataCompat.Builder()

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(true)
                .setAudioAttributes(audioAttributes)
                .build()
        }

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        musicRepository = MusicRepository.getInstance(this)
        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val appContext = applicationContext

        mediaSession = MediaSessionCompat(baseContext, LOG_TAG).apply {
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            setPlaybackState(
                stateBuilder.setState(
                    PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1F
                ).build()
            )
            setCallback(sessionCallback)
            setSessionToken(sessionToken)

            val mediaButtonIntent = Intent(
                Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver::class.java
            )

            setMediaButtonReceiver(
                PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0)
            )

            val activityIntent = Intent(appContext, MainActivity::class.java)
            setSessionActivity(PendingIntent.getActivity(appContext, 0, activityIntent, 0))

        }

        player = ExoPlayer.Builder(this@MediaPlaybackService).build().apply {
            addListener(playerListener)
        }

        createChannel()
        setMediaPlaybackState(PlaybackState.STATE_STOPPED)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return if (true) {
            BrowserRoot(MEDIA_ROOT_ID, null)
        } else {
            BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
        }
    }

    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val key = MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession.run {
            isActive = false
            release()
        }
        player.removeListener(playerListener)
        player.release()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, importance
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun getNotification(playbackState: Int): Notification {
        val builder = MediaStyleHelper.from(this, mediaSession, CHANNEL_ID)
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                getString(R.string.notification_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
        )

        if (playbackState == PlaybackStateCompat.STATE_PLAYING) builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
        ) else builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                getString(R.string.notification_play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
        )

        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                getString(R.string.notification_next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setMediaSession(mediaSession.sessionToken)
        )
        builder.setSmallIcon(R.mipmap.ic_launcher)

        builder.color = ContextCompat.getColor(
            this,
            R.color.black
        )
        builder.setShowWhen(false)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setOnlyAlertOnce(true)
        builder.setChannelId(CHANNEL_ID)
        return builder.build()
    }

    private fun refreshNotificationAndForegroundStatus(playbackState: Int) {

        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }

            PlaybackStateCompat.STATE_PAUSED -> {
                NotificationManagerCompat.from(this).notify(
                    NOTIFICATION_ID,
                    getNotification(playbackState)
                )
                stopForeground(false)
            }

            else -> {
                stopForeground(true)
            }
        }
    }

    private inner class PlayerEventListener : Player.Listener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) {
                            stopForeground(false)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    sessionCallback.onSkipToNext()
                }
                else -> {
                    stopForeground(true)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val cause = error.cause
            when (cause) {
                is HttpDataSource.HttpDataSourceException -> {
                    val requestDataSpec = cause.dataSpec
                    if (cause is HttpDataSource.InvalidResponseCodeException) {
                        Log.i(
                            TAG,
                            " ${cause.message.toString()}\n ${requestDataSpec.uri}\n ${requestDataSpec.httpRequestHeaders}"
                        )
                    }
                }
                else -> Log.i(
                    TAG,
                    " ${cause?.message}"
                )
            }
        }
    }

    private val sessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {

            var currentState = PlaybackStateCompat.STATE_STOPPED

            override fun onPlay() {
                if (!player.playWhenReady) {
                    startService(Intent(applicationContext, MediaPlaybackService::class.java))

                    val track = musicRepository.getCurrent()
                    updateMetadataFromTrack(track)
                    prepareToPlay(track.trackUri)

                    if (!audioFocusRequested) {
                        audioFocusRequested = true
                        val audioFocusResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            audioManager!!.requestAudioFocus(audioFocusRequest!!)
                        } else {
                            audioManager!!.requestAudioFocus(
                                audioFocusChangeListener,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN
                            )
                        }
                        if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
                    }
                    mediaSession.isActive = true
                    player.play()

                }
                currentState = PlaybackStateCompat.STATE_PLAYING
                setMediaPlaybackState(currentState)
                refreshNotificationAndForegroundStatus(currentState)
            }

            override fun onStop() {
                if (player.playWhenReady) {
                    player.playWhenReady = false
                }
                if (audioFocusRequested) {
                    audioFocusRequested = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
                    } else {
                        audioManager!!.abandonAudioFocus(
                            audioFocusChangeListener
                        )
                    }
                }
                mediaSession.isActive = false

                currentState = PlaybackStateCompat.STATE_STOPPED
                setMediaPlaybackState(currentState)
                refreshNotificationAndForegroundStatus(currentState)
                stopSelf()
            }

            override fun onPause() {
                if (player.playWhenReady) {
                    currentState = PlaybackStateCompat.STATE_PAUSED
                    player.playWhenReady = false
                }
                setMediaPlaybackState(currentState)
                refreshNotificationAndForegroundStatus(currentState)
            }

            override fun onSkipToNext() {
                startService(Intent(applicationContext, MediaPlaybackService::class.java))
                val track = musicRepository.getNext()
                updateMetadataFromTrack(track)
                prepareToPlay(track.trackUri)
                currentState = PlaybackStateCompat.STATE_SKIPPING_TO_NEXT
                setMediaPlaybackState(currentState)
                refreshNotificationAndForegroundStatus(currentState)
            }

            override fun onSkipToPrevious() {
                startService(Intent(applicationContext, MediaPlaybackService::class.java))
                val track = musicRepository.getPrevious()
                updateMetadataFromTrack(track)
                prepareToPlay(track.trackUri)
                mediaSession.isActive = true
                player.playWhenReady = true
                currentState = PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS
                setMediaPlaybackState(currentState)
                refreshNotificationAndForegroundStatus(currentState)
            }

            private fun prepareToPlay(uriString: String?) {
                val uri = Uri.parse(uriString ?: "")
                val mediaItem = MediaItem.fromUri(uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
            }
        }

    private val audioFocusChangeListener =
        OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> sessionCallback.onPlay()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> sessionCallback.onPause()
                else -> sessionCallback.onPause()
            }
        }

    private fun updateMetadataFromTrack(track: Track) {
        metadataBuilder.apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            putString(
                MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                track.bitmapUri
            )
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, track.trackUri)

            putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                track.duration?.toLong() ?: 0
            )
        }
        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun setMediaPlaybackState(state: Int) {
        val playbackstateBuilder = PlaybackStateCompat.Builder()
        val pbAction = when (state) {
            PlaybackStateCompat.STATE_PLAYING -> (
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_PAUSE
                )

            PlaybackStateCompat.STATE_PAUSED -> (
                PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )

            PlaybackStateCompat.STATE_STOPPED -> PlaybackStateCompat.ACTION_STOP

            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> PlaybackStateCompat.ACTION_SKIP_TO_NEXT

            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

            else -> 0
        }
        playbackstateBuilder.setActions(pbAction)
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mediaSession.setPlaybackState(playbackstateBuilder.build())
    }

    private companion object {
        private const val MEDIA_ROOT_ID = "media_root_id"
        private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        private const val LOG_TAG = "MediaSessionCompat is started!"
        private const val CHANNEL_ID = "Channel_ID"
        private const val NOTIFICATION_ID = 777
        private const val CHANNEL_NAME = "Audio player channel"
        private const val TAG = "MediaPlaybackService"
    }
}
