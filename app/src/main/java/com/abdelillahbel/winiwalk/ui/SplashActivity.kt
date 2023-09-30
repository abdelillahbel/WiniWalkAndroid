package com.abdelillahbel.winiwalk.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.abdelillahbel.winiwalk.R
import com.abdelillahbel.winiwalk.ui.auth.LoginActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val SPLASH_DISPLAY_LENGTH = 5000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen
        val w = window
        w.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash)
        Handler().postDelayed({
            val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
            val firstStart = prefs.getBoolean("firstStart", true)
            if (firstStart) {
                showStartScreen()
            } else {
                /* Create an Intent that will start the Start-Activity. */
                val signInIntent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(signInIntent)
                finish()
            }
        }, SPLASH_DISPLAY_LENGTH.toLong())
    }

    private fun showStartScreen() {
        val signinIntent = Intent(this@SplashActivity, StartActivity::class.java)
        startActivity(signinIntent)
        finish()
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("firstStart", false)
        editor.apply()
    }
}