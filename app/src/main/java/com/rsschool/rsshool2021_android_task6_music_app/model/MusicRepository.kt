package com.rsschool.rsshool2021_android_task6_music_app.model

import android.R.attr
import com.rsschool.rsshool2021_android_task6_music_app.model.data.Track
import android.R.attr.data
import android.content.Context
import com.rsschool.rsshool2021_android_task6_music_app.model.api.Api

class MusicRepository(context: Context) {
    private var tracks: List<Track> = Api.getListOfTracksUnSuspend(context)
    private var maxIndex = tracks.size - 1
    private var currentItemIndex = 0

    fun getNext(): Track{
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

    fun getCurrent(): Track{
        return tracks[currentItemIndex]
    }
}
