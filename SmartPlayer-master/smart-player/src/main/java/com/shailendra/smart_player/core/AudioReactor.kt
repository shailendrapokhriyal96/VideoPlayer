package com.shailendra.smart_player.core

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.audiofx.AudioEffect
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioRendererEventListener

class AudioReactor(
    private val context: Context,
    private val player: SimpleExoPlayer
) : OnAudioFocusChangeListener, AudioRendererEventListener {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null

    init {
        request = if (SHOULD_BUILD_FOCUS_REQUEST) {
            AudioFocusRequest.Builder(FOCUS_GAIN_TYPE)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(this)
                .build()
        } else {
            null
        }
    }

    fun dispose() {
        abandonAudioFocus()
    }

    fun requestAudioFocus() {
        if (SHOULD_BUILD_FOCUS_REQUEST) {
            audioManager.requestAudioFocus(request!!)
        } else {
            audioManager.requestAudioFocus(this, STREAM_TYPE, FOCUS_GAIN_TYPE)
        }
    }

    private fun abandonAudioFocus() {
        if (SHOULD_BUILD_FOCUS_REQUEST) {
            audioManager.abandonAudioFocusRequest(request!!)
        } else {
            audioManager.abandonAudioFocus(this)
        }
    }

    var volume: Int
        get() = audioManager.getStreamVolume(STREAM_TYPE)
        set(volume) {
            audioManager.setStreamVolume(STREAM_TYPE, volume, 0)
        }

    val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(STREAM_TYPE)

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> onAudioFocusGain()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onAudioFocusLossCanDuck()
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onAudioFocusLoss()
        }
    }

    private fun onAudioFocusGain() {
        player.volume = DUCK_AUDIO_TO
        animateAudio(DUCK_AUDIO_TO, 1f)
        player.playWhenReady = true
    }

    private fun onAudioFocusLoss() {
        player.playWhenReady = false
    }

    private fun onAudioFocusLossCanDuck() {
        // Set the volume to 1/10 on ducking
        animateAudio(player.volume, DUCK_AUDIO_TO)
    }

    private fun animateAudio(from: Float, to: Float) {
        val valueAnimator = ValueAnimator()
        valueAnimator.setFloatValues(from, to)
        valueAnimator.duration = DUCK_DURATION.toLong()
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                player.volume = from
            }

            override fun onAnimationCancel(animation: Animator) {
                player.volume = to
            }

            override fun onAnimationEnd(animation: Animator) {
                player.volume = to
            }
        })
        valueAnimator.addUpdateListener { animation: ValueAnimator ->
            player.volume = animation.animatedValue as Float
        }
        valueAnimator.start()
    }

    fun onAudioSessionId(i: Int) {
        if (!isUsingDSP()) return
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, i)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(intent)
    }

    companion object {
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
        private val SHOULD_BUILD_FOCUS_REQUEST = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        private const val DUCK_DURATION = 1500
        private const val DUCK_AUDIO_TO = .2f
        private const val FOCUS_GAIN_TYPE = AudioManager.AUDIOFOCUS_GAIN
        private const val STREAM_TYPE = AudioManager.STREAM_MUSIC

        fun isUsingDSP(): Boolean {
            return true
        }
    }
}