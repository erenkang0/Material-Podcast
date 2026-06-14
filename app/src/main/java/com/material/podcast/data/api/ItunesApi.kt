package com.material.podcast.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApi {

    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 20,
        @Query("country") country: String = "us",
    ): ItunesSearchResponse

    @GET("lookup")
    suspend fun lookup(
        @Query("id") ids: String,
        @Query("entity") entity: String = "podcast",
    ): ItunesSearchResponse
}
