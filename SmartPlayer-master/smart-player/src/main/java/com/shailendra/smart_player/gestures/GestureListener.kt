package com.shailendra.smart_player.gestures

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

abstract class GestureListener(ctx: Context?, rootView: View) : View.OnTouchListener,
    IGestureListener {

    private val gestureDetector: GestureDetector
    private val isVolumeGestureEnabled = true
    private val isBrightnessGestureEnabled = true
    private var isMoving = false
    var rootView: View

    init {
        gestureDetector = GestureDetector(ctx, MyGestureListener())
        this.rootView = rootView
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(motionEvent)
        if (motionEvent.action == MotionEvent.ACTION_UP && isMoving) {
            isMoving = false
            onScrollEnd()
        }
        return true
    }

    private inner class MyGestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(
            initialEvent: MotionEvent,
            movingEvent: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val deltaY = movingEvent.y - initialEvent.y
            val deltaX = movingEvent.x - initialEvent.x
            if (abs(deltaX) > abs(deltaY)) {
                if (abs(deltaX) > SWIPE_THRESHOLD) {
                    onHorizontalScroll(movingEvent, deltaX)
                }
            }
            if (!isVolumeGestureEnabled && !isBrightnessGestureEnabled) return false

            val insideThreshold = abs(movingEvent.y - initialEvent.y) <= MOVEMENT_THRESHOLD
            /*  if (!isMoving && (insideThreshold || Math.abs(distanceX) > Math.abs(distanceY))
                    || playerImpl.getCurrentState() == BasePlayer.STATE_COMPLETED) {
                return false;
            }*/

            if (!isMoving && (insideThreshold || abs(distanceX) > abs(distanceY))) {
                return false
            }
            isMoving = true
            val acceptAnyArea = isVolumeGestureEnabled != isBrightnessGestureEnabled
            val acceptVolumeArea = acceptAnyArea || initialEvent.x > rootView.width / 2
            val acceptBrightnessArea = acceptAnyArea || !acceptVolumeArea
            if (isVolumeGestureEnabled && acceptVolumeArea) {
                /**
                 * Go for volume control on left drag of the player
                 */
                volume(distanceY.toInt())
            } else if (isBrightnessGestureEnabled && acceptBrightnessArea) {
                /**
                 * Go for brightness control on left drag of the player
                 */
                brightness(distanceY.toInt())
                return false
            }
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            /**on tap of screen */
            onTap()
            return false
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Fling event occurred.  Notification of this one happens after an "up" event.
            if (e2.action == MotionEvent.ACTION_DOWN) {
                val diffY = e2.y - e1.y
            }
            var result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                    result = true
                } else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                }
                result = true
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }
    }

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        private const val MOVEMENT_THRESHOLD = 40
    }
}