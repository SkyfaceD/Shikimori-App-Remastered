package com.gnoemes.shikimori.presentation.presenter.person

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.roles.PersonInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.roles.domain.PersonDetails
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.person.converter.PersonDetailsViewModelConverter
import com.gnoemes.shikimori.presentation.view.person.PersonView
import com.gnoemes.shikimori.utils.appendLoadingLogic
import javax.inject.Inject

@InjectViewState
class PersonPresenter @Inject constructor(
        val interactor: PersonInteractor,
        val converter: PersonDetailsViewModelConverter
) : BaseNetworkPresenter<PersonView>() {

    var id: Long = Constants.NO_ID
    private lateinit var currentPerson: PersonDetails

    override fun initData() {
        loadPerson()
    }

    private fun loadPerson() =
            interactor.getDetails(id)
                    .appendLoadingLogic(viewState)
                    .doOnSuccess { currentPerson = it }
                    .map(converter)
                    .subscribe({ viewState.setData(it) }, this::processErrors)

    fun onOpenInBrowser() {
        if (::currentPerson.isInitialized) onOpenWeb(currentPerson.url)
    }
}