package com.material.podcast.data.repository

import com.material.podcast.data.api.ItunesApi
import com.material.podcast.data.api.ItunesResult
import com.material.podcast.data.model.Podcast
import com.material.podcast.data.model.PodcastEpisode
import com.material.podcast.data.rss.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class PodcastRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Echoes Podcast/0.1 Android")
                .build()
            chain.proceed(req)
        }
        .build()

    private val api: ItunesApi = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ItunesApi::class.java)

    private val rssParser = RssParser()

    // Simple in-memory caches
    private val podcastCache = mutableMapOf<String, Podcast>()
    private val episodeCache = mutableMapOf<String, List<PodcastEpisode>>()
    private var featuredCache: List<Podcast>? = null

    // Curated podcast IDs — stable, popular, freely accessible shows
    private val curatedIds = listOf(
        "1200361736", // The Daily – NYT
        "1438054347", // Conan O'Brien Needs a Friend
        "1028908750", // Hidden Brain
        "394775318",  // 99% Invisible
        "278981407",  // Stuff You Should Know
        "290783428",  // Planet Money
        "354668519",  // Freakonomics Radio
        "1150510297", // How I Built This
        "917918570",  // Serial
        "160904630",  // TED Talks Daily
        "381986900",  // Fresh Air – NPR
        "1322200189", // Crime Junkie
    )

    suspend fun getFeaturedPodcasts(): List<Podcast> = withContext(Dispatchers.IO) {
        featuredCache?.let { return@withContext it }
        val result = api.lookup(curatedIds.joinToString(","))
            .results
            .filter { it.feedUrl.isNotEmpty() && it.effectiveId.isNotEmpty() }
            .map { it.toPodcast() }
            .sortedBy { podcast -> curatedIds.indexOfFirst { it == podcast.id } }
        featuredCache = result
        result.forEach { podcastCache[it.id] = it }
        result
    }

    suspend fun searchPodcasts(query: String): List<Podcast> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        api.search(term = query, limit = 20)
            .results
            .filter { it.feedUrl.isNotEmpty() && it.effectiveId.isNotEmpty() }
            .map { it.toPodcast() }
            .also { list -> list.forEach { podcastCache[it.id] = it } }
    }

    suspend fun searchByGenre(genre: String): List<Podcast> = withContext(Dispatchers.IO) {
        api.search(term = genre, limit = 10)
            .results
            .filter { it.feedUrl.isNotEmpty() && it.effectiveId.isNotEmpty() }
            .map { it.toPodcast() }
    }

    suspend fun getPodcast(id: String): Podcast? = withContext(Dispatchers.IO) {
        podcastCache[id]?.let { return@withContext it }
        api.lookup(id).results
            .firstOrNull { it.feedUrl.isNotEmpty() }
            ?.toPodcast()
            ?.also { podcastCache[id] = it }
    }

    suspend fun getEpisodes(podcast: Podcast): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        episodeCache[podcast.id]?.let { return@withContext it }
        try {
            val request = Request.Builder().url(podcast.feedUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body ?: return@withContext emptyList()
                val rssEpisodes = rssParser.parse(body.byteStream())
                val episodes = rssEpisodes.map { ep ->
                    PodcastEpisode(
                        guid = ep.guid,
                        title = ep.title,
                        description = ep.description,
                        audioUrl = ep.enclosureUrl,
                        artworkUrl = ep.imageUrl ?: podcast.artworkUrl,
                        publishedDate = ep.pubDate,
                        durationSeconds = ep.durationSeconds,
                        podcastTitle = podcast.title,
                        podcastId = podcast.id,
                        podcastAuthor = podcast.author,
                        transcriptUrl = ep.transcriptUrl ?: "",
                        chaptersUrl = ep.chaptersUrl ?: "",
                    )
                }
                episodeCache[podcast.id] = episodes
                episodes
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchTranscript(url: String): List<com.material.podcast.data.model.TranscriptCue> =
        withContext(Dispatchers.IO) {
            if (url.isBlank()) return@withContext emptyList()
            try {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    com.material.podcast.data.rss.TranscriptParser.parse(body)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun fetchChapters(url: String): List<com.material.podcast.data.model.Chapter> =
        withContext(Dispatchers.IO) {
            if (url.isBlank()) return@withContext emptyList()
            try {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    com.material.podcast.data.rss.ChaptersParser.parse(body)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun ItunesResult.toPodcast() = Podcast(
        id = effectiveId,
        title = collectionName.ifEmpty { artistName },
        author = artistName,
        artworkUrl = artworkUrl600.ifEmpty { artworkUrl100 },
        feedUrl = feedUrl,
        genre = primaryGenreName,
        episodeCount = trackCount,
        description = description.ifEmpty { shortDescription },
    )
}
