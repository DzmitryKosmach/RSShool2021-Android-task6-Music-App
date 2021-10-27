package com.rsschool.rsshool2021_android_task6_music_app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rsschool.rsshool2021_android_task6_music_app.model.MusicRepository
import com.rsschool.rsshool2021_android_task6_music_app.model.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var _trackStateFlow = MutableStateFlow<Track>(Track(null, null, null, null, null))
    val trackStateFlow: SharedFlow<Track> = _trackStateFlow.asStateFlow()

    init {
        val musicRepository = MusicRepository(application)
        val track = musicRepository.getCurrent()
        setTrack(track)
    }

    fun setTrack(track: Track) {
        _trackStateFlow.value = track
    }
}
