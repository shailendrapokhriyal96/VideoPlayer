package com.shailendra.smart_player.core

/**
 * Created by Shailendra on 29-07-2021
 * */

/*
To notify the app of the player's full screen button presses/toggle for full screen toggle request
by the user.
*/
interface SmartPlayerFullScreenToggleNotifier {
    fun togglePlayerFullScreen(makeFullScreen: Boolean)
}