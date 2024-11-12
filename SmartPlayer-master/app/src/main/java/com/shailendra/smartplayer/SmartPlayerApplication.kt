package com.shailendra.smartplayer

import android.util.Log
import com.shailendra.smartplayer.utils.Constants
import org.matomo.sdk.TrackMe
import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.extra.DimensionQueue
import org.matomo.sdk.extra.DownloadTracker
import org.matomo.sdk.extra.MatomoApplication
import org.matomo.sdk.extra.TrackHelper

class SmartPlayerApplication : MatomoApplication() {

    private var mDimensionQueue: DimensionQueue? = null

    override fun onCreate() {
        super.onCreate()

        onInitTracker()
    }

    override fun onCreateTrackerConfig(): TrackerBuilder {
        return TrackerBuilder.createDefault(Constants.MATOMO_TRACKER_URL, 1)
    }

    private fun onInitTracker() {
        // When working on an app we don't want to skew tracking results.
        // getMatomo().setDryRun(BuildConfig.DEBUG);

        // If you want to set a specific userID other than the random UUID token, do it NOW to ensure all future actions use that token.
        // Changing it later will track new events as belonging to a different user.
        val email = "abc@gmail.com"
        tracker.userId = email

        // Track this app install, this will only trigger once per app version.
        // i.e. "http://org.matomo.demo:1/185DECB5CFE28FDB2F45887022D668B4"
        TrackHelper.track().download().identifier(DownloadTracker.Extra.ApkChecksum(this)).with(tracker)
        // Alternative:
        // i.e. "http://org.matomo.demo:1/com.android.vending"
        // getTracker().download();
        mDimensionQueue = DimensionQueue(tracker)

        // This will be send the next time something is tracked.
        mDimensionQueue?.add(0, "test")
        tracker.addTrackingCallback { trackMe: TrackMe? ->
            Log.i("Tracker.onTrack(%s)", trackMe.toString())
            trackMe
        }
    }

    fun setUserIdInMatomoTracker(userId: String) {
        tracker.userId = userId
    }
}