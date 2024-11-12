package com.shailendra.smartplayer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.shailendra.smartplayer.R

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
    }

    fun playVideo(view: View) {
        startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
    }
}