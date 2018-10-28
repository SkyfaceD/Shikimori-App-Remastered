package com.gnoemes.shikimori.presentation.presenter.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.gnoemes.shikimori.entity.auth.AuthType
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.main.BottomScreens
import com.gnoemes.shikimori.presentation.view.anime.AnimeFragment
import com.gnoemes.shikimori.presentation.view.auth.AuthActivity
import com.gnoemes.shikimori.presentation.view.calendar.CalendarFragment
import com.gnoemes.shikimori.presentation.view.character.CharacterFragment
import com.gnoemes.shikimori.presentation.view.person.PersonFragment
import com.gnoemes.shikimori.presentation.view.rates.RatesContainerFragment

object RouteHolder {

    fun createFragment(screenKey: String?, data: Any?): Fragment? {
        return when (screenKey) {
            BottomScreens.RATES -> RatesContainerFragment.newInstance(data as? Long)
            BottomScreens.CALENDAR -> CalendarFragment.newInstance()
//                    BottomScreens.SEARCH ->
//                    BottomScreens.MAIN ->
//                    BottomScreens.MORE ->
            Screens.ANIME_DETAILS -> AnimeFragment.newInstance(data as Long)
            Screens.CHARACTER_DETAILS -> CharacterFragment.newInstance(data as Long)
            Screens.PERSON_DETAILS -> PersonFragment.newInstance(data as Long)
            else -> null
        }
    }

    fun createActivityIntent(context: Context?, screenKey: String?, data: Any?): Intent? {
        return when (screenKey) {
            Screens.AUTHORIZATION -> AuthActivity.newIntent(context, data as AuthType)
            //TODO check settings to open in internal on external browser
            Screens.WEB -> Intent(Intent.ACTION_VIEW, Uri.parse(data as String))
            else -> null
        }
    }
}