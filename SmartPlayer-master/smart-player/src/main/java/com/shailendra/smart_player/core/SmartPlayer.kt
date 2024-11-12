package com.shailendra.smart_player.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shailendra.smart_player.R
import com.shailendra.smart_player.adapters.VideoTrackSelectorViewAdapter
import com.shailendra.smart_player.analytics.matomo.MatomoPlayerAnalytics
import com.shailendra.smart_player.analytics.player.ProcessedAnalyticsDataListener
import com.shailendra.smart_player.analytics.player.RawAnalyticsDataListener
import com.shailendra.smart_player.data.VideoTracksInfo
import com.shailendra.smart_player.gestures.AnimationUtils
import com.shailendra.smart_player.gestures.AnimationUtils.animateView
import com.shailendra.smart_player.gestures.GestureListener
import com.shailendra.smart_player.interfaces.VideoTrackSelectionNotifier
import com.shailendra.smart_player.ui.SmartPlayerView
import com.shailendra.smart_player.util.Constants
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory

import com.google.android.exoplayer2.source.MediaSourceFactory


/**
 * Created by Shailendra  on 29-07-2021
 * */
/*
Includes/Provides default configuration of exoplayer as needed in all apps so as to exclude boilerplate
code from every app needs to implement exoplayer module.
*/
class SmartPlayer(
    private val context: Context,
    private val appName: String,
    private val smartPlayerView: SmartPlayerView,
    private val videoName: String,
    private val videoUrl: String,
    private val adsUrl: String,
    private val smartPlayerFullScreenToggleNotifier: SmartPlayerFullScreenToggleNotifier
) : SmartPlayerFullScreenToggleNotifier by smartPlayerFullScreenToggleNotifier,
    VideoTrackSelectionNotifier {

    private lateinit var supportedVideoTracksAvailable: ArrayList<VideoTracksInfo>
    private lateinit var trackSelector: DefaultTrackSelector
    private var player: StyledPlayerView? = null
    private var requestMap: LinkedHashMap<String, String>? = null
    private var _player: SimpleExoPlayer? = null
    private var _playWhenReady: Boolean = true
    private var _currentWindow: Int = 0
    private var _playbackPosition: Long = 0L
    var enableVideoAnalytics = false

    private var progressBarBrightness: ProgressBar
    private var progressBarVolume: ProgressBar
    private var layoutBrightnessControl: ConstraintLayout
    private var layoutVolumeControl: ConstraintLayout
    private var icBrightness: ImageView
    private var icVolume: ImageView
    private var videoTrackSelector: TextView
    private var recyclerViewVideoTracks: RecyclerView

    private var maxGestureLength = 0
    private var maxVolume = 0

    private var audioReactor: AudioReactor? = null

    private var selectedVideoTrackPosition = 0
    private var isVideoTracksLoadingFirstTime = true

    private var adsLoader: ImaAdsLoader? = null

    init {
        // Initializing pretty logger for better logging
        Logger.addLogAdapter(AndroidLogAdapter())

        progressBarBrightness = smartPlayerView.findViewById(R.id.progress_bar_brightness)
        progressBarVolume = smartPlayerView.findViewById(R.id.progress_bar_volume)
        layoutBrightnessControl = smartPlayerView.findViewById(R.id.layout_brightness_control)
        layoutVolumeControl = smartPlayerView.findViewById(R.id.layout_volume_control)
        icBrightness = smartPlayerView.findViewById(R.id.ic_brightness)
        icVolume = smartPlayerView.findViewById(R.id.ic_volume)
        player = smartPlayerView.findViewById(R.id.styled_exo_player_view)
        videoTrackSelector =
            (player?.findViewById(R.id.exo_controller) as StyledPlayerControlView).findViewById(
                R.id.exo_video_quality
            )

        recyclerViewVideoTracks =
            smartPlayerView.findViewById(R.id.recycler_view_video_tracks)

        adsLoader = ImaAdsLoader.Builder( /* context= */context).build()

    }

    fun initializePlayer() {
        trackSelector = DefaultTrackSelector(context)

        val dataSourceFactory = DefaultDataSourceFactory(context, buildHttpDataSourceFactory())

        val mediaSourceFactory: MediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setAdsLoaderProvider { adsLoader }
            .setAdViewProvider(player)

        _player = SimpleExoPlayer
            .Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        audioReactor = _player?.let { AudioReactor(context, it) }
        maxVolume = audioReactor?.maxVolume ?: 0

        if (enableVideoAnalytics)
            enableVideoAnalyticsListener()

        player?.setShowNextButton(false)
        player?.setShowPreviousButton(false)
        player?.player = _player
        adsLoader?.setPlayer(_player)

        val mediaItem: MediaItem =
            MediaItem.Builder().setUri(Uri.parse(videoUrl)).setAdTagUri(Uri.parse(adsUrl)).build()

        enableFullScreenOption()

        /*val mediaSource: MediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(videoUrl))
        )*/

        _player?.setMediaItem(mediaItem)
        _player?.playWhenReady = _playWhenReady
        _player?.prepare()

        player?.addOnLayoutChangeListener { view: View?, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or: Int, ob: Int ->
            if (l != ol || t != ot || r != or || b != ob) {
                // Use smaller value to be consistent between screen orientations
                // (and to make usage easier)
                val width = r - l
                val height = b - t
                maxGestureLength =
                    (width.coerceAtMost(height) * Constants.MAX_GESTURE_LENGTH).toInt()

                progressBarBrightness.max = maxGestureLength
                progressBarVolume.max = maxGestureLength
                setInitialGestureValues()
            }
        }

        setUpGestureControls()
        setUserEventListeners()
    }

    private fun setUserEventListeners() {
        setClickListeners()
    }

    private fun setClickListeners() {
        videoTrackSelector.setOnClickListener {
            /*VideoTrackSelectorView(trackSelector).show(
                (context as AppCompatActivity).supportFragmentManager,
                appName
            )*/
            displayAvailableVideoTracks()
        }
    }

    private fun displayAvailableVideoTracks() {
        supportedVideoTracksAvailable = getSupportVideoTracksAvailable()
        if (isVideoTracksLoadingFirstTime) {
            selectedVideoTrackPosition = supportedVideoTracksAvailable.size - 1
            isVideoTracksLoadingFirstTime = false
        }
        prepareRecyclerViewForDisplay(supportedVideoTracksAvailable)
    }

    private fun prepareRecyclerViewForDisplay(supportedVideoTracksAvailable: ArrayList<VideoTracksInfo>) {
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        recyclerViewVideoTracks.layoutManager = layoutManager
        recyclerViewVideoTracks.setHasFixedSize(true)

        recyclerViewVideoTracks.adapter =
            VideoTrackSelectorViewAdapter(
                context,
                supportedVideoTracksAvailable,
                this,
                selectedVideoTrackPosition
            )

        animateView(
            recyclerViewVideoTracks,
            AnimationUtils.Type.SLIDE_RIGHT_AND_ALPHA,
            true,
            400,
            200
        )
    }

    private fun getSupportVideoTracksAvailable(): ArrayList<VideoTracksInfo> {
        val videoTracksInfo = ArrayList<VideoTracksInfo>()

        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        //val parameters = trackSelector.parameters

        mappedTrackInfo?.run {

            for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
                val trackType = mappedTrackInfo.getRendererType(rendererIndex)
                if (trackType == C.TRACK_TYPE_VIDEO) {
                    val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
                    /*val isRendererDisabled = parameters.getRendererDisabled(rendererIndex)
                    val selectionOverride =
                        parameters.getSelectionOverride(rendererIndex, trackGroupArray)*/

                    for (groupIndex in 0 until trackGroupArray.length) {
                        for (trackIndex in 0 until trackGroupArray[groupIndex].length) {
                            /*val trackName = DefaultTrackNameProvider(resources).getTrackName(
                                trackGroupArray[groupIndex].getFormat(trackIndex)
                            )*/
                            val isTrackSupported = mappedTrackInfo.getTrackSupport(
                                rendererIndex,
                                groupIndex,
                                trackIndex
                            ) == C.FORMAT_HANDLED

                            if (isTrackSupported)
                                videoTracksInfo.add(
                                    VideoTracksInfo(
                                        trackGroupArray[groupIndex].getFormat(
                                            trackIndex
                                        ).height.toString(),
                                        groupIndex, rendererIndex, trackIndex
                                    )
                                )
                        }
                    }
                }
            }

            videoTracksInfo.sortByDescending { v -> v.videoTrackQuality.toInt() }
            videoTracksInfo.add(VideoTracksInfo("Auto", 0, 0, -1))
        }

        return videoTracksInfo
    }

    private fun setInitialGestureValues() {
        if (audioReactor != null) {
            val audioReactorVolume = audioReactor?.volume ?: 0
            val currentVolumeNormalized = audioReactorVolume.div(maxVolume)
            progressBarVolume.progress = progressBarVolume.max * currentVolumeNormalized
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpGestureControls() {
        player?.let {
            it.setOnTouchListener(
                SmartPlayerGestureListener(
                    context,
                    it
                )
            )

            it.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    it.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    it.height //height is ready
                }
            })
        }
    }

    private fun enableVideoAnalyticsListener() {
        _player?.let {
            it.addAnalyticsListener(RawAnalyticsDataListener(it, videoName))
            it.addAnalyticsListener(
                ProcessedAnalyticsDataListener(
                    it,
                    videoName
                ).getPlaybackStatsListener()
            )
        }
    }

    private fun enableFullScreenOption() {
        player?.setControllerOnFullScreenModeChangedListener {
            //toggleFullScreen(it)
            smartPlayerFullScreenToggleNotifier.togglePlayerFullScreen(it)
        }
    }

    fun releasePlayer() {
        if (_player != null) {
            _playbackPosition = _player?.currentPosition ?: 0L
            _currentWindow = _player?.currentWindowIndex ?: 0
            _playWhenReady = _player?.playWhenReady ?: false
            _player?.release()
            _player = null
        }
    }

    fun enableVideoAnalytics(enableVideoAnalytics: Boolean): SmartPlayer {
        this.enableVideoAnalytics = enableVideoAnalytics
        return this
    }

    fun analyticsBuilder(matomoTracker: Tracker, trackHelper: TrackHelper): SmartPlayer {
        MatomoPlayerAnalytics.matomoTracker = matomoTracker
        MatomoPlayerAnalytics.trackHelper = trackHelper
        return this
    }

    private fun buildHttpDataSourceFactory(): DataSource.Factory {
        val defaultHttpDataSourceFactory =
            DefaultHttpDataSource.Factory().setUserAgent(Util.getUserAgent(context, appName))

        requestMap?.let { defaultHttpDataSourceFactory.setDefaultRequestProperties(it) }

        return defaultHttpDataSourceFactory
    }

    fun setRequestProperties(requestMap: LinkedHashMap<String, String>): SmartPlayer {
        this.requestMap = requestMap
        return this
    }

    private fun onScrollOver() {
        if (layoutVolumeControl.visibility == View.VISIBLE)
            animateView(
                layoutVolumeControl,
                AnimationUtils.Type.SCALE_AND_ALPHA,
                false,
                200,
                200
            )
        if (layoutBrightnessControl.visibility == View.VISIBLE) {
            animateView(
                layoutBrightnessControl,
                AnimationUtils.Type.SCALE_AND_ALPHA,
                false,
                200,
                200
            )
        }
    }

    inner class SmartPlayerGestureListener constructor(context: Context, rootView: View) :
        GestureListener(context, rootView) {

        private val gestureSeek = false

        override fun onTap() {
            player?.let {
                if (it.isControllerFullyVisible)
                    it.hideController()
                else
                    it.showController()
            }

            if (recyclerViewVideoTracks.isVisible)
                animateView(
                    recyclerViewVideoTracks,
                    AnimationUtils.Type.SLIDE_RIGHT_AND_ALPHA,
                    false,
                    400,
                    200
                )
        }

        override fun brightness(value: Int) {
            if (!gestureSeek) {
                progressBarBrightness.incrementProgressBy(value)
                val currentProgressPercent =
                    progressBarBrightness.progress.toFloat() / maxGestureLength
                val layoutParams = (context as AppCompatActivity).window.attributes
                layoutParams.screenBrightness = currentProgressPercent
                context.window.attributes = layoutParams

                val resId =
                    when {
                        currentProgressPercent < 0.25 -> R.drawable.ic_brightness_low
                        currentProgressPercent < 0.75 -> R.drawable.ic_brightness_medium
                        else -> R.drawable.ic_brightness
                    }

                icBrightness.setImageDrawable(
                    AppCompatResources.getDrawable(context.applicationContext, resId)
                )

                if (layoutBrightnessControl.visibility != View.VISIBLE) {
                    animateView(
                        layoutBrightnessControl,
                        AnimationUtils.Type.SCALE_AND_ALPHA,
                        true,
                        200
                    )
                }
            }
        }

        override fun volume(value: Int) {
            if (!gestureSeek) {
                progressBarVolume.incrementProgressBy(value)
                val currentProgressPercent =
                    progressBarVolume.progress.toFloat() / maxGestureLength
                val currentVolume = (maxVolume * currentProgressPercent).toInt()

                audioReactor?.volume = currentVolume

                val resId =
                    when {
                        currentProgressPercent <= 0 -> R.drawable.ic_volume_off
                        currentProgressPercent < 0.25 -> R.drawable.ic_volume_mute
                        currentProgressPercent < 0.75 -> R.drawable.ic_volume_down
                        else -> R.drawable.ic_volume
                    }

                icVolume.setImageDrawable(
                    AppCompatResources.getDrawable(context.applicationContext, resId)
                )
                if (layoutVolumeControl.visibility != View.VISIBLE) {
                    animateView(layoutVolumeControl, AnimationUtils.Type.SCALE_AND_ALPHA, true, 200)
                }
                if (layoutBrightnessControl.visibility == View.VISIBLE) {
                    layoutBrightnessControl.visibility = View.GONE
                }
            }

        }

        override fun onScrollEnd() {
            onScrollOver()
        }
    }

    override fun notifySelectedVideoTrackPosition(position: Int) {
        selectedVideoTrackPosition = position

        val parameters = trackSelector.parameters
        val builder = parameters.buildUpon()

        if (position != supportedVideoTracksAvailable.size - 1) {
            videoTrackSelector.text =
                "${supportedVideoTracksAvailable[position].videoTrackQuality}p"

            builder.clearSelectionOverrides(supportedVideoTracksAvailable[position].rendererIndex)
                .setRendererDisabled(
                    supportedVideoTracksAvailable[position].rendererIndex,
                    false
                )

            val override = DefaultTrackSelector.SelectionOverride(
                supportedVideoTracksAvailable[position].groupIndex,
                supportedVideoTracksAvailable[position].trackIndex
            )

            builder.setSelectionOverride(
                supportedVideoTracksAvailable[position].rendererIndex,
                trackSelector.currentMappedTrackInfo!!.getTrackGroups(
                    supportedVideoTracksAvailable[position].rendererIndex
                ),
                override
            )
        } else {
            videoTrackSelector.text = supportedVideoTracksAvailable[position].videoTrackQuality

            builder.clearSelectionOverrides(supportedVideoTracksAvailable[position].rendererIndex)
                .setRendererDisabled(
                    supportedVideoTracksAvailable[position].rendererIndex,
                    false
                )

            builder.setAllowMultipleAdaptiveSelections(true)
        }

        trackSelector.setParameters(builder)

        if (recyclerViewVideoTracks.isVisible)
            animateView(
                recyclerViewVideoTracks,
                AnimationUtils.Type.SLIDE_RIGHT_AND_ALPHA,
                false,
                400,
                200
            )
    }
}