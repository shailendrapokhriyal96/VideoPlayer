package com.shailendra.smart_player.gestures

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import java.lang.Exception

object AnimationUtils {
    private const val TAG = "AnimationUtils"
    private const val DEBUG = true

    fun animateView(view: View, enterOrExit: Boolean, duration: Long) {
        animateView(view, Type.ALPHA, enterOrExit, duration, 0, null)
    }

    fun animateView(view: View, enterOrExit: Boolean, duration: Long, delay: Long) {
        animateView(view, Type.ALPHA, enterOrExit, duration, delay, null)
    }

    fun animateView(
        view: View,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long,
        execOnEnd: Runnable?
    ) {
        animateView(view, Type.ALPHA, enterOrExit, duration, delay, execOnEnd)
    }

    /**
     * Animate the view
     *
     * @param view          view that will be animated
     * @param animationType [Type] of the animation
     * @param enterOrExit   true to enter, false to exit
     * @param duration      how long the animation will take, in milliseconds
     * @param delay         how long the animation will wait to start, in milliseconds
     * @param execOnEnd     runnable that will be executed when the animation ends
     */
    @JvmOverloads
    fun animateView(
        view: View,
        animationType: Type?,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long = 0,
        execOnEnd: Runnable? = null
    ) {
        if (DEBUG) {
            val id: String? = try {
                view.resources.getResourceEntryName(view.id)
            } catch (e: Exception) {
                view.id.toString() + ""
            }
            val msg = String.format(
                "%8s →  [%s:%s] [%s %s:%s] execOnEnd=%s",
                enterOrExit,
                view.javaClass.simpleName,
                id,
                animationType,
                duration,
                delay,
                execOnEnd
            )
            Log.d(TAG, "animateView()$msg")
        }
        if (view.visibility == View.VISIBLE && enterOrExit) {
            if (DEBUG) Log.d(
                TAG,
                "animateView() view was already visible > view = [$view]"
            )
            view.animate().setListener(null).cancel()
            view.visibility = View.VISIBLE
            view.alpha = 1f
            execOnEnd?.run()
            return
        } else if ((view.visibility == View.GONE || view.visibility == View.INVISIBLE) && !enterOrExit) {
            if (DEBUG) Log.d(
                TAG,
                "animateView() view was already gone > view = [$view]"
            )
            view.animate().setListener(null).cancel()
            view.visibility = View.GONE
            view.alpha = 0f
            execOnEnd?.run()
            return
        }
        view.animate().setListener(null).cancel()
        view.visibility = View.VISIBLE
        when (animationType) {
            Type.ALPHA -> animateAlpha(view, enterOrExit, duration, delay, execOnEnd)
            Type.SCALE_AND_ALPHA -> animateScaleAndAlpha(
                view,
                enterOrExit,
                duration,
                delay,
                execOnEnd
            )
            Type.LIGHT_SCALE_AND_ALPHA -> animateLightScaleAndAlpha(
                view,
                enterOrExit,
                duration,
                delay,
                execOnEnd
            )
            Type.SLIDE_AND_ALPHA -> animateSlideAndAlpha(
                view,
                enterOrExit,
                duration,
                delay,
                execOnEnd
            )
            Type.LIGHT_SLIDE_AND_ALPHA -> animateLightSlideAndAlpha(
                view,
                enterOrExit,
                duration,
                delay,
                execOnEnd
            )
            Type.SLIDE_RIGHT_AND_ALPHA -> animateSlideRightAndAlpha(
                view,
                enterOrExit,
                duration,
                delay,
                execOnEnd
            )
        }
    }

    /**
     * Animate the background color of a view
     */
    fun animateBackgroundColor(
        view: View,
        duration: Long,
        @ColorInt colorStart: Int,
        @ColorInt colorEnd: Int
    ) {
        if (DEBUG) {
            Log.d(
                TAG,
                "animateBackgroundColor() called with: view = [$view], duration = [$duration], colorStart = [$colorStart], colorEnd = [$colorEnd]"
            )
        }
        val EMPTY = arrayOf(IntArray(0))
        val viewPropertyAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorStart, colorEnd)
        viewPropertyAnimator.interpolator = FastOutSlowInInterpolator()
        viewPropertyAnimator.duration = duration
        viewPropertyAnimator.addUpdateListener { animation ->
            ViewCompat.setBackgroundTintList(
                view,
                ColorStateList(EMPTY, intArrayOf(animation.animatedValue as Int))
            )
        }
        viewPropertyAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                ViewCompat.setBackgroundTintList(view, ColorStateList(EMPTY, intArrayOf(colorEnd)))
            }

            override fun onAnimationCancel(animation: Animator) {
                onAnimationEnd(animation)
            }
        })
        viewPropertyAnimator.start()
    }

    /**
     * Animate the text color of any view that extends [TextView] (Buttons, EditText...)
     */
    fun animateTextColor(
        view: TextView,
        duration: Long,
        @ColorInt colorStart: Int,
        @ColorInt colorEnd: Int
    ) {
        if (DEBUG) {
            Log.d(
                TAG,
                "animateTextColor() called with: view = [$view], duration = [$duration], colorStart = [$colorStart], colorEnd = [$colorEnd]"
            )
        }
        val viewPropertyAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorStart, colorEnd)
        viewPropertyAnimator.interpolator = FastOutSlowInInterpolator()
        viewPropertyAnimator.duration = duration
        viewPropertyAnimator.addUpdateListener { animation -> view.setTextColor(animation.animatedValue as Int) }
        viewPropertyAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.setTextColor(colorEnd)
            }

            override fun onAnimationCancel(animation: Animator) {
                view.setTextColor(colorEnd)
            }
        })
        viewPropertyAnimator.start()
    }

    fun animateHeight(view: View, duration: Long, targetHeight: Int): ValueAnimator {
        val height = view.height
        if (DEBUG) {
            Log.d(
                TAG,
                "animateHeight: duration = [$duration], from $height to → $targetHeight in: $view"
            )
        }
        val animator = ValueAnimator.ofFloat(height.toFloat(), targetHeight.toFloat())
        animator.interpolator = FastOutSlowInInterpolator()
        animator.duration = duration
        animator.addUpdateListener { animation: ValueAnimator ->
            val value = animation.animatedValue as Float
            view.layoutParams.height = value.toInt()
            view.requestLayout()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.layoutParams.height = targetHeight
                view.requestLayout()
            }

            override fun onAnimationCancel(animation: Animator) {
                view.layoutParams.height = targetHeight
                view.requestLayout()
            }
        })
        animator.start()
        return animator
    }

    fun animateRotation(view: View, duration: Long, targetRotation: Int) {
        if (DEBUG) {
            Log.d(
                TAG,
                "animateRotation: duration = [" + duration + "], from " + view.rotation + " to → " + targetRotation + " in: " + view
            )
        }
        view.animate().setListener(null).cancel()
        view.animate().rotation(targetRotation.toFloat()).setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    view.rotation = targetRotation.toFloat()
                }

                override fun onAnimationEnd(animation: Animator) {
                    view.rotation = targetRotation.toFloat()
                }
            }).start()
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Internals
    ////////////////////////////////////////////////////////////////////////// */
    private fun animateAlpha(
        view: View,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long,
        execOnEnd: Runnable?
    ) {
        if (enterOrExit) {
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(1f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        execOnEnd?.run()
                    }
                }).start()
        } else {
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(0f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        execOnEnd?.run()
                    }
                }).start()
        }
    }

    private fun animateScaleAndAlpha(
        view: View,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long,
        execOnEnd: Runnable?
    ) {
        if (enterOrExit) {
            view.scaleX = .8f
            view.scaleY = .8f
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(1f).scaleX(1f)
                .scaleY(1f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        execOnEnd?.run()
                    }
                }).start()
        } else {
            view.scaleX = 1f
            view.scaleY = 1f
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(0f).scaleX(.8f)
                .scaleY(.8f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        execOnEnd?.run()
                    }
                }).start()
        }
    }

    private fun animateLightScaleAndAlpha(
        view: View,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long,
        execOnEnd: Runnable?
    ) {
        if (enterOrExit) {
            view.alpha = .5f
            view.scaleX = .95f
            view.scaleY = .95f
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(1f).scaleX(1f)
                .scaleY(1f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        execOnEnd?.run()
                    }
                }).start()
        } else {
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(0f).scaleX(.95f)
                .scaleY(.95f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        execOnEnd?.run()
                    }
                }).start()
        }
    }

    private fun animateSlideAndAlpha(
        view: View,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long,
        execOnEnd: Runnable?
    ) {
        if (enterOrExit) {
            view.translationY = -view.height.toFloat()
            view.alpha = 0f
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(1f).translationY(0f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        execOnEnd?.run()
                    }
                }).start()
        } else {
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(0f)
                .translationY(-view.height.toFloat())
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        execOnEnd?.run()
                    }
                }).start()
        }
    }

    private fun animateSlideRightAndAlpha(
        view: View,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long,
        execOnEnd: Runnable?
    ) {
        if (enterOrExit) {
            view.translationX = view.width.toFloat()
            view.alpha = 0f
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(1f).translationX(0f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        execOnEnd?.run()
                    }
                }).start()
        } else {
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(0f)
                .translationX(view.width.toFloat())
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        execOnEnd?.run()
                    }
                }).start()
        }
    }

    private fun animateLightSlideAndAlpha(
        view: View,
        enterOrExit: Boolean,
        duration: Long,
        delay: Long,
        execOnEnd: Runnable?
    ) {
        if (enterOrExit) {
            view.translationY = (-view.height / 2).toFloat()
            view.alpha = 0f
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(1f).translationY(0f)
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        execOnEnd?.run()
                    }
                }).start()
        } else {
            view.animate().setInterpolator(FastOutSlowInInterpolator()).alpha(0f)
                .translationY((-view.height / 2).toFloat())
                .setDuration(duration).setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        execOnEnd?.run()
                    }
                }).start()
        }
    }

    enum class Type {
        ALPHA, SCALE_AND_ALPHA, LIGHT_SCALE_AND_ALPHA, SLIDE_AND_ALPHA, LIGHT_SLIDE_AND_ALPHA, SLIDE_RIGHT_AND_ALPHA
    }
}