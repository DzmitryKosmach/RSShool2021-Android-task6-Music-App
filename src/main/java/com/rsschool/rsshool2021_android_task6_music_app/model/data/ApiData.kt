package com.rsschool.rsshool2021_android_task6_music_app.model.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiData(
    @Json(name = "title") val title: String,
    @Json(name = "artist") val artist: String,
    @Json(name = "bitmapUri") val bitmapUri: String,
    @Json(name = "trackUri") val trackUri: String,
    @Json(name = "duration") val duration: Int
)
