package com.gnoemes.shikimori.domain.series

import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.reactivex.Completable
import io.reactivex.Single

interface SeriesInteractor {

    fun getEpisodes(id : Long, alternative : Boolean) : Single<List<Episode>>

    fun getTranslations(type: TranslationType, animeId: Long, episodeId: Long, alternative: Boolean): Single<List<Translation>>

    fun getTranslationSettings(animeId: Long, episodeIndex: Int) : Single<TranslationSetting>

    fun getVideo(payload : TranslationVideo) : Single<Video>

    fun setEpisodeWatched(animeId: Long, episodeId: Int, rateId: Long = Constants.NO_ID): Completable

    fun setEpisodeUnwatched(animeId: Long, episodeId: Int, rateId: Long) : Completable

    fun setEpisodeStatus(animeId: Long, episodeId: Int, rateId: Long, isWatching : Boolean) : Completable
}