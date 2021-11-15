package com.rsschool.rsshool2021_android_task6_music_app

import android.app.Application
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.rsschool.rsshool2021_android_task6_music_app.model.MusicRepository
import com.rsschool.rsshool2021_android_task6_music_app.model.data.Track
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var _trackStateFlow = MutableStateFlow(Track(null, null, null, null, null))
    val trackStateFlow: SharedFlow<Track> = _trackStateFlow.asStateFlow()

    private var _controlsState = MutableStateFlow(MainViewControlsState())
    val controlsState: StateFlow<MainViewControlsState> = _controlsState

    init {
        val musicRepository = MusicRepository.getInstance(application)
        val track = musicRepository.getCurrent()
        setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        setTrack(track)
    }

    fun setTrack(track: Track) {
        _trackStateFlow.value = track
    }

    fun setPlaybackState(state: Int) {
        when (state) {
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> {
                setPlayButton(true)
                setPauseButton(false)
                setStopButton(true)
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                setStopButton(false)
                setPauseButton(false)
                setPlayButton(false)
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                setStopButton(false)
                setPauseButton(true)
                setPlayButton(false)
            }
            else -> {
                setStopButton(false)
                setPauseButton(false)
                setPlayButton(false)
            }
        }
    }

    private fun setPlayButton(isPlaying: Boolean) {
        setControlsState { copy(isPlay = isPlaying) }
    }

    private fun setPauseButton(isPaused: Boolean) {
        setControlsState { copy(isPause = isPaused) }
    }

    private fun setStopButton(isStop: Boolean) {
        setControlsState { copy(isStopped = isStop) }
    }

    private fun setControlsState(modifier: MainViewControlsState.() -> MainViewControlsState) {
        _controlsState.value = _controlsState.value.modifier()
    }
}
