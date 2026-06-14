package com.material.podcast.data.api

import com.google.gson.annotations.SerializedName

data class ItunesSearchResponse(
    @SerializedName("resultCount") val resultCount: Int = 0,
    @SerializedName("results") val results: List<ItunesResult> = emptyList(),
)

data class ItunesResult(
    @SerializedName("collectionId") val collectionId: Long = 0,
    @SerializedName("trackId") val trackId: Long = 0,
    @SerializedName("wrapperType") val wrapperType: String = "",
    @SerializedName("kind") val kind: String = "",
    @SerializedName("collectionName") val collectionName: String = "",
    @SerializedName("artistName") val artistName: String = "",
    @SerializedName("artworkUrl100") val artworkUrl100: String = "",
    @SerializedName("artworkUrl600") val artworkUrl600: String = "",
    @SerializedName("feedUrl") val feedUrl: String = "",
    @SerializedName("primaryGenreName") val primaryGenreName: String = "",
    @SerializedName("trackCount") val trackCount: Int = 0,
    @SerializedName("description") val description: String = "",
    @SerializedName("shortDescription") val shortDescription: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
) {
    val effectiveId: String get() =
        collectionId.takeIf { it > 0 }?.toString()
            ?: trackId.takeIf { it > 0 }?.toString()
            ?: ""
}
