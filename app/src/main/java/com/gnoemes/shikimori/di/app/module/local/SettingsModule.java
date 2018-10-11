package com.gnoemes.shikimori.di.app.module.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.gnoemes.shikimori.data.local.preference.SettingsSource;
import com.gnoemes.shikimori.data.local.preference.SettingsSourceImpl;
import com.gnoemes.shikimori.data.local.preference.UserSource;
import com.gnoemes.shikimori.data.local.preference.UserSourceImpl;
import com.gnoemes.shikimori.di.app.annotations.SettingsQualifier;
import com.gnoemes.shikimori.di.app.annotations.UserQualifier;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public interface SettingsModule {

    @Provides
    @SettingsQualifier
    @Singleton
    static SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @UserQualifier
    @Singleton
    static SharedPreferences provideUserQualifierSharedPreferences(Context context) {
        return context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
    }

    @Binds
    @Singleton
    UserSource bindUserSource(UserSourceImpl source);

    @Binds
    @Singleton
    SettingsSource bindSettingsSource(SettingsSourceImpl source);
}
