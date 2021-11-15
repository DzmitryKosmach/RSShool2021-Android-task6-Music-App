package com.rsschool.rsshool2021_android_task6_music_app.model

import android.content.Context
import com.rsschool.rsshool2021_android_task6_music_app.model.api.Api
import com.rsschool.rsshool2021_android_task6_music_app.model.data.Track

object MusicRepository {
    private var tracks: List<Track> = emptyList()
    private var maxIndex = 0
    private var currentItemIndex = 0

    fun getInstance(context: Context): MusicRepository {
        if (tracks.isEmpty()) {
            tracks = Api.getListOfTracksUnSuspend(context)
            maxIndex = tracks.size - 1
        }
        return this
    }

    fun getNext(): Track {
        if (currentItemIndex == maxIndex)
            currentItemIndex = 0
        else
            currentItemIndex++
        return getCurrent();
    }

    fun getPrevious(): Track {
        if (currentItemIndex == 0)
            currentItemIndex = maxIndex
        else
            currentItemIndex--
        return getCurrent()
    }

    fun getCurrent(): Track {
        return tracks[currentItemIndex]
    }
}
