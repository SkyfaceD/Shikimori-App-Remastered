package com.gnoemes.shikimori.data.local.preference

import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.domain.TranslationType

interface SettingsSource {

    var isAutoStatus : Boolean

    var isRomadziNaming: Boolean

    var isRememberTranslationType : Boolean

    var isRememberPlayer : Boolean

    var isNotificationsEnabled : Boolean

    var translationType : TranslationType

    var playerType : PlayerType
}