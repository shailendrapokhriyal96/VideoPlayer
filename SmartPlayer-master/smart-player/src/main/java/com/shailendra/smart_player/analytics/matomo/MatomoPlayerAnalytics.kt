package com.shailendra.smart_player.analytics.matomo

import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

/**
 * Created by Shailendra on 29-07-2021
 * */
object MatomoPlayerAnalytics {

    lateinit var matomoTracker: Tracker
    lateinit var trackHelper: TrackHelper

    fun trackCustomEvent(category: String, action: String, name: String, path: String) {
        trackHelper.event(category, action).name(name).path(path).with(matomoTracker)
    }

    fun trackCustomEvent(
        category: String,
        action: String,
        name: String,
        path: String,
        value: Float
    ) {
        trackHelper.event(category, action).name(name).path(path).value(value)
            .with(matomoTracker)
    }
}