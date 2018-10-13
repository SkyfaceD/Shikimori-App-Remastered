package com.gnoemes.shikimori.presentation.view.bottom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.widget.Toast
import com.arellomobile.mvp.MvpAppCompatFragment
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.di.base.modules.BaseFragmentModule
import com.gnoemes.shikimori.entity.main.LocalCiceroneHolder
import com.gnoemes.shikimori.presentation.view.base.fragment.BackButtonListener
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.Navigator
import ru.terrakok.cicerone.Router
import ru.terrakok.cicerone.android.SupportAppNavigator
import ru.terrakok.cicerone.commands.Command
import javax.inject.Inject
import javax.inject.Named

class BottomTabContainer : MvpAppCompatFragment(), RouterProvider, BackButtonListener, HasSupportFragmentInjector {

    @Inject
    lateinit var ciceroneHolder: LocalCiceroneHolder

    @Inject
    @field:Named(BaseFragmentModule.CHILD_FRAGMENT_MANAGER)
    lateinit var childFM: FragmentManager

    @Inject
    lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    companion object {
        fun newInstance() = BottomTabContainer()
    }

    ///////////////////////////////////////////////////////////////////////////
    // UI METHODS
    ///////////////////////////////////////////////////////////////////////////

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (childFM.findFragmentById(R.id.fragment_container) == null) {
            getCicerone().router.replaceScreen(getContainerName())
        }
    }

    override fun onResume() {
        super.onResume()
        getCicerone().navigatorHolder.setNavigator(localNavigator)
    }

    override fun onPause() {
        getCicerone().navigatorHolder.removeNavigator()
        super.onPause()
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    private fun getContainerName(): String = tag!!

    private fun getCicerone(): Cicerone<Router> = ciceroneHolder.getCicerone(getContainerName())

    override fun onBackPressed(): Boolean {
        val fragment = childFM.findFragmentById(R.id.fragment_container)
        return if (fragment is BaseFragmentView) {
            fragment.onBackPressed()
            true
        } else false
    }

    override val localRouter: Router
        get() = getCicerone().router

    override val localNavigator: Navigator
        get() = object : SupportAppNavigator(activity, childFM, R.id.fragment_container) {
            override fun createFragment(screenKey: String?, data: Any?): Fragment? {
//                when (screenKey) {
//                    BottomScreens.RATES -> {}
//                    BottomScreens.CALENDAR -> {}
//                    BottomScreens.SEARCH -> {}
//                    BottomScreens.MAIN -> {}
//                    BottomScreens.MORE -> {}
//                }
                return null
            }

            override fun createActivityIntent(context: Context?, screenKey: String?, data: Any?): Intent? {
                return null
            }

            override fun showSystemMessage(message: String?) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

            override fun exit() {
                (activity as? RouterProvider)?.localRouter?.exit()
            }

            override fun unknownScreen(command: Command?) {
                (activity as? RouterProvider)?.localNavigator?.applyCommands(arrayOf(command))
            }
        }


}