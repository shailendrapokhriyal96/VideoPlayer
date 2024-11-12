package com.shailendra.smart_player.analytics.player

import com.shailendra.smart_player.analytics.matomo.MatomoPlayerAnalytics
import com.shailendra.smart_player.util.Constants
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.PlaybackStats
import com.google.android.exoplayer2.analytics.PlaybackStatsListener

class ProcessedAnalyticsDataListener(
    private val _player: SimpleExoPlayer,
    private val _videoName: String
) {

    private var _videoDuration: Long = 0L

    private var playBackStatsListener: PlaybackStatsListener =
        PlaybackStatsListener(true) { eventTime: AnalyticsListener.EventTime?, playbackStats: PlaybackStats? ->
            _videoDuration = _player.contentDuration

            if (_videoDuration != 0L) {
                val playBackStatesHistory = playbackStats?.playbackStateHistory
                val lastStateInPlaybackHistory =
                    playBackStatesHistory?.get(playBackStatesHistory.size - 1)
                val playbackPercentWhenAbandoned =
                    (((lastStateInPlaybackHistory?.eventTime?.currentPlaybackPositionMs
                        ?: 0L) * 100) / _videoDuration).toInt()

                if (lastStateInPlaybackHistory?.playbackState == PlaybackStats.PLAYBACK_STATE_ABANDONED) {
                    if (playbackPercentWhenAbandoned > 0 || lastStateInPlaybackHistory.eventTime.currentPlaybackPositionMs > 0) {
                        /*
                         Check that the playback wasn't seeked to it's previous position when abandoned last time previously
                         before hitting the callback as that would mean that playback is continuing from previously played
                         percentage of the same video and we don't want to send abandoned event in that case.
                        */
                        if (playBackStatesHistory[playBackStatesHistory.size - 2].playbackState != PlaybackStats.PLAYBACK_STATE_SEEKING) {
                            // Share Event for Video Playback Event for video abandoned action.
                            MatomoPlayerAnalytics.trackCustomEvent(
                                Constants.ANALYTICS_CUSTOM_EVENT_VIDEO_PLAYBACK,
                                Constants.ANALYTICS_ACTION_VIDEO_PLAYBACK_ABANDONED,
                                _videoName,
                                "SmartPlayer-Demo",
                                playbackPercentWhenAbandoned.toFloat()
                            )

                            // send standard categorized playback percentage events for 25%, 50%, 75%, 90%, 95%
                            sendPlaybackWatchedPercentEvent(playbackPercentWhenAbandoned)
                        }
                    }
                }
            }

            /*
            Send mean network bandwidth of a single video playback session to analytics.
            */
            playbackStats?.meanBandwidth?.let { meanNetworkBandwidth ->
                /*
                convert meanNetworkBandwidth data from bits/sec to Mb/sec and send to analytics.
                */
                if (meanNetworkBandwidth != -1)
                    MatomoPlayerAnalytics.trackCustomEvent(
                        Constants.ANALYTICS_CUSTOM_EVENT_VIDEO_PLAYBACK,
                        Constants.ANALYTICS_ACTION_MEAN_NETWORK_BANDWIDTH,
                        _videoName,
                        "Player",
                        String.format("%.2f", meanNetworkBandwidth.toDouble() / 8000000)
                            .toFloat()
                    )
            }
        }

    private fun sendPlaybackWatchedPercentEvent(playbackPercentWhenAbandoned: Int) {
        when (playbackPercentWhenAbandoned) {
            in 25..49 -> sendPlaybackWatchedEvent(
                25,
                playbackPercentWhenAbandoned
            )
            in 50..74 -> sendPlaybackWatchedEvent(
                50,
                playbackPercentWhenAbandoned
            )
            in 75..89 -> sendPlaybackWatchedEvent(
                75,
                playbackPercentWhenAbandoned
            )
            in 90..94 -> sendPlaybackWatchedEvent(
                90,
                playbackPercentWhenAbandoned
            )
            in 95..99 -> sendPlaybackWatchedEvent(
                95,
                playbackPercentWhenAbandoned
            )
        }

    }

    private fun sendPlaybackWatchedEvent(
        watchedPercent: Int,
        playbackPercentMsWhenAbandoned: Int
    ) {
        MatomoPlayerAnalytics.trackCustomEvent(
            Constants.ANALYTICS_CUSTOM_EVENT_VIDEO_PLAYBACK,
            "${Constants.ANALYTICS_ACTION_PLAYBACK_WATCHED}$watchedPercent",
            _videoName,
            "SmartPlayer-Demo",
            playbackPercentMsWhenAbandoned.toFloat()
        )
    }

    fun getPlaybackStatsListener(): PlaybackStatsListener {
        return playBackStatsListener
    }
}