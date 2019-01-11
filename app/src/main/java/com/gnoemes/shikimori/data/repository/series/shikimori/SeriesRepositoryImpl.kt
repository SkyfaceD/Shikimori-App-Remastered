package com.gnoemes.shikimori.data.repository.series.shikimori

import com.gnoemes.shikimori.data.local.db.AnimeRateSyncDbSource
import com.gnoemes.shikimori.data.local.db.EpisodeDbSource
import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.data.repository.series.shikimori.converter.EpisodeResponseConverter
import com.gnoemes.shikimori.data.repository.series.shikimori.converter.TranslationResponseConverter
import com.gnoemes.shikimori.entity.series.domain.Episode
import com.gnoemes.shikimori.entity.series.domain.Translation
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

class SeriesRepositoryImpl @Inject constructor(
        private val api: VideoApi,
        private val converter: EpisodeResponseConverter,
        private val translationConverter: TranslationResponseConverter,
        private val episodeSource: EpisodeDbSource,
        private val syncSource: AnimeRateSyncDbSource
) : SeriesRepository {

    override fun getEpisodes(id: Long): Single<List<Episode>> =
            api.getEpisodes(id)
                    .map(converter)
                    .flatMap { episodes ->
                        episodeSource.saveEpisodes(episodes).toSingleDefault(episodes)
                                .flatMap { syncEpisodes(id, it) }
                    }

    override fun getTranslations(type: TranslationType, animeId: Long, episodeId: Int): Single<List<Translation>> =
            api.getTranslations(animeId, episodeId, type)
                    .map(translationConverter)

    override fun setEpisodeWatched(animeId: Long, episodeId: Int): Completable = episodeSource.episodeWatched(animeId, episodeId)

    override fun isEpisodeWatched(animeId: Long, episodeId: Int): Single<Boolean> = episodeSource.isEpisodeWatched(animeId, episodeId)

    private fun syncEpisodes(id: Long, list: List<Episode>): Single<List<Episode>> {
        return Single.fromCallable { list }
                .flatMap { episodes ->
                    Single.zip(episodeSource.getWatchedEpisodesCount(id), syncSource.getEpisodeCount(id), BiFunction<Int, Int, Boolean> { local, remote -> local == remote })
                            .flatMap { same -> if (same) Single.fromCallable { episodes } else updateFromSync(id, episodes) }
                }
    }

    private fun updateFromSync(id: Long, episodes: List<Episode>): Single<List<Episode>> {
        return Single.zip(episodeSource.getWatchedEpisodesCount(id), syncSource.getEpisodeCount(id), BiFunction<Int, Int, Int> { local, remote -> remote.minus(local) })
                .filter { it > 0 }
                .flatMapCompletable { count ->
                    Observable.fromIterable(episodes)
                            .flatMapSingle { updateIfWatched(it) }
                            .filter { !it.isWatched }
                            .take(count.toLong())
                            .flatMapCompletable { episodeSource.episodeWatched(id, it.id) }
                }
                .toSingleDefault(episodes)
                .flatMap { updateIfWatched(it) }
    }

    private fun updateIfWatched(episodes: List<Episode>): Single<List<Episode>> {
        return Observable.fromIterable(episodes)
                .flatMapSingle { updateIfWatched(it) }
                .toList()
    }

    private fun updateIfWatched(episode: Episode): Single<Episode> {
        return episodeSource.isEpisodeWatched(episode.animeId, episode.id)
                .map { episode.copy(isWatched = it) }
    }
}

