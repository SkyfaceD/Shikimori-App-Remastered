package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.series.data.EpisodeResponse
import com.gnoemes.shikimori.entity.series.data.TranslationResponse
import com.gnoemes.shikimori.entity.series.data.VideoResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VideoApi {

    @GET("/api/anime/{id}/series")
    fun getEpisodes(@Path("id") id: Long): Single<List<EpisodeResponse>>

    @GET("/api/anime/alternative/{id}/series")
    fun getEpisodesAlternative(@Path("id") id: Long): Single<List<EpisodeResponse>>

    @GET("/api/anime/{animeId}/{episodeId}/translations")
    fun getTranslations(@Path("animeId") animeId: Long,
                        @Path("episodeId") episodeId: Long,
                        @Query("type") type: String
    ): Single<List<TranslationResponse>>

    @GET("/api/anime/alternative/{animeId}/{episodeId}/translations")
    fun getTranslationsAlternative(@Path("animeId") animeId: Long,
                                   @Path("episodeId") episodeId: Long,
                                   @Query("type") type: String
    ): Single<List<TranslationResponse>>

    @GET("/api/anime/{animeId}/{episodeId}/video/{videoId}")
    fun getVideo(@Path("animeId") animeId: Long,
                 @Path("episodeId") episodeId: Int,
                 @Path("videoId") videoId: String,
                 @Query("language") language: String,
                 @Query("kind") type: String,
                 @Query("author") author: String,
                 @Query("hosting") hosting: String
    ): Single<VideoResponse>
}