package com.shailendra.smart_player.gestures

import android.view.MotionEvent

interface IGestureListener {
    fun onTap()
    fun onHorizontalScroll(event: MotionEvent?, delta: Float) { }
    fun onSwipeRight() { }
    fun onSwipeLeft() { }
    fun onSwipeBottom() { }
    fun onSwipeTop() { }
    fun brightness(value: Int)
    fun volume(value: Int)
    fun onScrollEnd()
}