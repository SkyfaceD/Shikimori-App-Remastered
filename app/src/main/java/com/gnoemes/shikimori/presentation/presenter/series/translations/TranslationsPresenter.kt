package com.gnoemes.shikimori.presentation.presenter.series.translations

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.domain.series.SeriesInteractor
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.EmbeddedPlayerNavigationData
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.entity.series.presentation.TranslationViewModel
import com.gnoemes.shikimori.entity.series.presentation.TranslationsNavigationData
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.series.translations.converter.TranslationsViewModelConverter
import com.gnoemes.shikimori.presentation.view.series.translations.TranslationsView
import com.gnoemes.shikimori.utils.Utils
import com.gnoemes.shikimori.utils.appendLoadingLogic
import com.gnoemes.shikimori.utils.clearAndAddAll
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

//TODO base series presenter with search logic?
//TODO process 400, 404. add view to retry request
@InjectViewState
class TranslationsPresenter @Inject constructor(
        private val interactor: SeriesInteractor,
        private val settingsSource: SettingsSource,
        private val converter: TranslationsViewModelConverter
) : BaseNetworkPresenter<TranslationsView>() {

    lateinit var navigationData: TranslationsNavigationData
    lateinit var type: TranslationType

    private var setting: TranslationSetting? = null

    private val items = mutableListOf<TranslationViewModel>()
    private lateinit var selectedHosting: TranslationVideo

    override fun initData() {
        super.initData()
        type = settingsSource.translationType
        viewState.setEpisodeName(navigationData.episodeIndex)
        viewState.setBackground(navigationData.image)

        loadData()
    }

    private fun loadData() {
        loadSettingsIfNeed()
                .toSingleDefault(type)
                .flatMap { loadTranslations(it) }
                .doOnSubscribe { viewState.setTranslationType(type) }
                .appendLoadingLogic(viewState)
                .subscribe(this::setData, this::processErrors)
                .addToDisposables()
    }

    private fun loadSettings() =
            interactor.getTranslationSettings(navigationData.animeId)
                    .doOnSuccess { setting = it }
                    .doOnSuccess { type = it.lastType ?: settingsSource.translationType }
                    .ignoreElement()

    private fun loadTranslations(type: TranslationType) =
            interactor.getTranslations(type, navigationData.animeId, navigationData.episodeId, navigationData.isAlternative)
                    .map { converter.convertTranslations(it, setting) }

    private fun loadSettingsIfNeed(): Completable =
            (if (setting == null && settingsSource.useLocalTranslationSettings) loadSettings() else Completable.complete())

    private fun setData(data: List<TranslationViewModel>) {
        val items = data.toMutableList()
        if (items.find { it.isSameAuthor } != null) {
            val priorityItem = items[items.indexOfFirst { it.isSameAuthor }]
            items.remove(priorityItem)
            items.add(0, priorityItem)
        }
        this@TranslationsPresenter.items.clearAndAddAll(items)
        showData(items)
    }

    private fun showData(data: List<TranslationViewModel>, isSearch: Boolean = false) {
        if (data.isNotEmpty()) {
            viewState.showData(data)
            viewState.hideEmptyView()
            viewState.showContent(true)
        } else {
            if (!isSearch) {
                viewState.showEmptyView()
                viewState.showContent(false)
            } else viewState.showSearchEmpty()
        }
    }

    fun onHostingClicked(hosting: TranslationVideo) {
        this.selectedHosting = hosting
        if (!Utils.isHostingSupports(hosting.videoHosting)) openVideo(hosting, PlayerType.WEB)
        else if (settingsSource.isRememberPlayer) openVideo(hosting, settingsSource.playerType)
        else viewState.showPlayerDialog()
    }

    fun onPlayerSelected(playerType: PlayerType) {
        openVideo(selectedHosting, playerType)
    }

    //Only embedded player can process object payload
    //Others o uses urls
    private fun openVideo(payload: TranslationVideo, playerType: PlayerType) {
        if (playerType == PlayerType.EMBEDDED) openPlayer(playerType, EmbeddedPlayerNavigationData(navigationData.name, navigationData.rateId, items.firstOrNull()!!.episodesSize, payload))
        else getVideoAndExecute(payload) { openPlayer(playerType, it.tracks.firstOrNull()?.url) }
    }

    override fun openPlayer(playerType: PlayerType, payload: Any?) {
        super.openPlayer(playerType, payload)

        setEpisodeWatched(selectedHosting)
    }

    private fun setEpisodeWatched(payload: TranslationVideo) {
        (if (settingsSource.isAutoIncrement) interactor.sendEpisodeChanges(EpisodeChanges(payload.animeId, navigationData.episodeIndex, true))
        else Completable.complete())
                .andThen(interactor.saveTranslationSettings(TranslationSetting(payload.animeId, payload.author, payload.type)))
                .doOnSubscribe { onBackPressed() }
                .subscribe({}, this::processErrors)
                .addToDisposables()
    }

    //TODO quality chooser
    private fun getVideoAndExecute(payload: TranslationVideo, onSubscribe: (Video) -> Unit) {
        interactor.getVideo(payload, navigationData.isAlternative)
                .appendLoadingLogic(viewState)
                .subscribe(onSubscribe::invoke, this::processErrors)
                .addToDisposables()
    }

    fun onMenuClicked(category: TranslationMenu) {
        when (category) {
            is TranslationMenu.Download -> showDownloadDialog(category.videos)
        }
    }

    private fun showDownloadDialog(videos: List<TranslationVideo>) {
        val filteredItems = videos.filter { Utils.isHostingSupports(it.videoHosting, true) }

        Observable.fromIterable(filteredItems)
                .flatMapSingle { interactor.getVideo(it, it.videoHosting == VideoHosting.SMOTRET_ANIME) }
                .flatMap { video ->
                    Observable.just(video)
                            .flatMapIterable { it.tracks }
                            .map { converter.convertTrack(video.hosting, it) }
                }
                .toList()
                .appendLoadingLogic(viewState)
                .subscribe(viewState::showDownloadDialog, this::processErrors)
                .addToDisposables()
    }

    fun onTrackForDownloadSelected(url: String) {
        //TODO download
        Log.i("DEVE", url)
    }

    fun onDiscussionClicked() {
        //TODO find related topic (can be parsed from episode link or topic api)
    }

    fun onTypeChanged(newType: TranslationType) {
        this.type = newType
        loadData()
    }

    fun onSearchClicked() {
        viewState.showSearchView()
    }

    fun onQueryChanged(newText: String?) {
        val text = newText ?: ""

        if (text.isBlank()) {
            showData(items)
        } else {
            val searchItems = items.filter { it.authors.contains(text, true) }
            showData(searchItems, true)
        }

        viewState.scrollToPosition(0)
    }

    fun onSearchClosed() {
        viewState.onSearchClosed()
        showData(items)
    }
}