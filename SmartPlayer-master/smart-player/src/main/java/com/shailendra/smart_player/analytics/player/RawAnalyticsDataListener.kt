package com.shailendra.smart_player.analytics.player

import android.util.Log
import com.shailendra.smart_player.analytics.matomo.MatomoPlayerAnalytics
import com.shailendra.smart_player.util.Constants
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.PlaybackStats
import com.google.android.exoplayer2.analytics.PlaybackStatsListener
import com.orhanobut.logger.Logger

/**
 * Created by Shailendra on 29-07-2021
 * */

/*
To capture player's raw analytics data during the playback.
*/
class RawAnalyticsDataListener(private val _player: SimpleExoPlayer, private val _videoName: String) :
    AnalyticsListener {

    private var _videoDuration: Long = 0L
    private var _isVideoInitialStart = true

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long
    ) {
        super.onRenderedFirstFrame(eventTime, output, renderTimeMs)

        _videoDuration = _player.contentDuration

        Logger.d(_videoDuration)

        when {
            eventTime.currentPlaybackPositionMs == 0L -> {
                /*
                if condition is true ->
                This means that video is being started from 0, we will send
                Event -> 'Video Playback'; Event Action -> 'PLAYBACK_STARTED' to matomo.
                */
                MatomoPlayerAnalytics.trackCustomEvent(
                    Constants.ANALYTICS_CUSTOM_EVENT_VIDEO_PLAYBACK,
                    Constants.ANALYTICS_ACTION_VIDEO_PLAYBACK_STARTED,
                    _videoName,
                    "SmartPlayer-Demo"
                )
            }
            _isVideoInitialStart -> {
                /*
                if condition is true ->
                This means that video is being resumed from previous playback which was abandoned due
                to either user interaction or some error in player.
                */

                /*
                send Event -> 'Video Playback';
                Event Action -> 'PLAYBACK_RESUMED_FROM_PREVIOUSLY_ABANDONED_PLAYBACK' to matomo.
                */
                MatomoPlayerAnalytics.trackCustomEvent(
                    Constants.ANALYTICS_CUSTOM_EVENT_VIDEO_PLAYBACK,
                    Constants.ANALYTICS_ACTION_VIDEO_PLAYBACK_RESUMED_FROM_PREVIOUSLY_ABANDONED_PLAYBACK,
                    _videoName,
                    "SmartPlayer-Demo",
                    getPlaybackPercent(eventTime.currentPlaybackPositionMs).toFloat()
                )
            }
            else -> {
                /*
                if condition is true ->
                This means that video is being resumed after user seeking it forward or backward.
                */

                /*
                send Event -> 'Video Playback';
                Event Action -> 'PLAYBACK_RESUMED_AFTER_SEEK' to matomo.
                */
                MatomoPlayerAnalytics.trackCustomEvent(
                    Constants.ANALYTICS_CUSTOM_EVENT_VIDEO_PLAYBACK,
                    Constants.ANALYTICS_ACTION_VIDEO_PLAYBACK_RESUMED_AFTER_SEEK,
                    _videoName,
                    "SmartPlayer-Demo",
                    getPlaybackPercent(eventTime.currentPlaybackPositionMs).toFloat()
                )
            }
        }

        _isVideoInitialStart = false
    }

    // To detect the error thrown by player during media load or playback.
    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        super.onPlayerError(eventTime, error)

        MatomoPlayerAnalytics.trackCustomEvent(
            Constants.ANALYTICS_CUSTOM_EVENT_VIDEO_PLAYBACK,
            Constants.ANALYTICS_ACTION_FATAL_PLAYER_ERROR,
            _videoName,
            "SmartPlayer-Demo",
            eventTime.currentPlaybackPositionMs.toFloat()
        )

    }

    private fun getPlaybackPercent(currentPlaybackPositionMs: Long): Int =
        ((currentPlaybackPositionMs * 100) / _videoDuration).toInt()
}