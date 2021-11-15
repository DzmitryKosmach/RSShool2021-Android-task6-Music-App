package com.rsschool.rsshool2021_android_task6_music_app.model.api

import android.content.Context
import android.util.Log
import com.rsschool.rsshool2021_android_task6_music_app.R
import com.rsschool.rsshool2021_android_task6_music_app.model.data.ApiData
import com.rsschool.rsshool2021_android_task6_music_app.model.data.Track
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

object Api {
    private var moshi = Moshi.Builder().build()

    fun getListOfTracksUnSuspend(context: Context): List<Track> {
        val type: Type = Types.newParameterizedType(
            MutableList::class.java,
            ApiData::class.java
        )
        val json = readJSONFromRaw(context)
        val adapter: JsonAdapter<List<ApiData>> = moshi.adapter(type)
        val list = adapter.fromJson(json)!!
        return list.map { Track(it.title, it.artist, it.bitmapUri, it.trackUri, it.duration) }
    }
}

private fun readJSONFromRaw(context: Context): String {
    return context.resources.openRawResource(R.raw.playlist).bufferedReader().use { it.readText() }
}
