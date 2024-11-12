package com.shailendra.smartplayer

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.shailendra.smart_player.core.SmartPlayer
import com.shailendra.smart_player.core.SmartPlayerFullScreenToggleNotifier
import com.shailendra.smart_player.ui.SmartPlayerView
import com.shailendra.smartplayer.R
import com.shailendra.smartplayer.utils.Constants.VIDEO_NAME
import com.shailendra.smartplayer.utils.Constants.VIDEO_URL
import org.matomo.sdk.extra.TrackHelper


class MainActivity : AppCompatActivity(), SmartPlayerFullScreenToggleNotifier {

    private lateinit var smartPlayerView: SmartPlayerView
    private var smartPlayer: SmartPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        smartPlayerView = findViewById(R.id.smart_player_view)
    }

    override fun onStart() {
        super.onStart()

        smartPlayer = SmartPlayer(
            this@MainActivity,
            resources.getString(R.string.app_name),
            smartPlayerView,
            VIDEO_NAME,
            VIDEO_URL,
            resources.getString(R.string.ad_tag_url),
            this
        )

        val requestMap = LinkedHashMap<String, String>()
        requestMap["Cookie"] = "Testing cookie"

        smartPlayer
            ?.enableVideoAnalytics(true)
            ?.analyticsBuilder((application as SmartPlayerApplication).tracker, TrackHelper.track())
            //?.setRequestProperties(requestMap)
            ?.initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        smartPlayer?.releasePlayer()
    }

    override fun togglePlayerFullScreen(makeFullScreen: Boolean) {
        // Listen to player full screen button toggle presses here.
        toggleFullScreen(makeFullScreen)
    }

    private fun toggleFullScreen(makeFullScreen: Boolean) {
        if (makeFullScreen) {
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
            smartPlayerView.layoutParams = params
            hideSystemUI()
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            val params =
                ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 500)
            smartPlayerView.layoutParams = params
            showSystemUI()
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(
            window,
            window.decorView.rootView
        ).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        supportActionBar?.hide()
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView.rootView).show(
            WindowInsetsCompat.Type.systemBars()
        )
        supportActionBar?.show()
    }
}