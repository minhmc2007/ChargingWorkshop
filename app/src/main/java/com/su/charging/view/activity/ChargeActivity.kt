package com.su.charging.view.activity

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.su.charging.receiver.BatteryBroadCastReceiver
import com.su.charging.ChargeAudioManager
import com.su.charging.Charging
import com.su.charging.R
import com.su.charging.view.fragment.SettingsFragmentCompat
import com.su.charging.view.preference.SoundPreference
import kotlin.math.ceil

class ChargeActivity : AppCompatActivity(), BatteryBroadCastReceiver.BatteryListener {

    private lateinit var video: VideoView
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charge)

        findViewById<ConstraintLayout>(R.id.charge_top).setOnClickListener {
            if (SettingsFragmentCompat.isClickClose) {
                onPowerDisconnected() //finish()

                //Required SDK 26 (Android 8 "Oreo")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    unlockScreen() //Call password UI
            } else {

                //Required SDK 26 (Android 8 "Oreo")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    unlockScreen() //Call password UI

                    val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

                    if (keyguardManager.isKeyguardLocked) {
                        // Register dismiss keyguard callback
                        keyguardManager.requestDismissKeyguard(
                            this,
                            object : KeyguardManager.KeyguardDismissCallback() {
                                // Dismissing Keyguard has succeeded and the device is now unlocked
                                override fun onDismissSucceeded() {
                                    onPowerDisconnected() //finish()
                                }
                            })
                    } else
                        onPowerDisconnected()
                }
            }
        }

        video = findViewById(R.id.video)
        info = findViewById(R.id.charge_info)

        chargeData()
        setDimension()
        chargeConfig()
    }

    private fun unlockScreen() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if (keyguardManager.isKeyguardLocked) {
            // If the screen is locked, ask the system to unlock it, need API 26
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            // If not locked, do nothing or do whatever you want
        }
    }

    private fun chargeData() {
        //充电信息
        val tf = Typeface.createFromAsset(assets, "fonts/bshark_bold.ttf")
        info.typeface = tf

        setBattery(BatteryBroadCastReceiver.firstBattery)
        //视频
        video.apply {
            val path =
                if (ChargeAudioManager.checkChargeState(BatteryBroadCastReceiver.firstBattery) == SoundPreference.AudioFlag.LOW)
                    Charging.normalChargingVideo
                else
                    Charging.quickChargingVideo
            if (!path.exists()) {
                Toast.makeText(this@ChargeActivity, "No charging animations available", Toast.LENGTH_SHORT).show()
                return
            }
            setVideoPath(path.absolutePath)
            setOnCompletionListener {
                start()
            }
            setOnErrorListener { _, what, extra ->
                Log.d("播放发生错误", "错误what=$what extra=$extra")
                Toast.makeText(
                    this@ChargeActivity,
                    "播放发生错误 what=$what extra=$extra",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
            start()
        }
        //音频
        ChargeAudioManager.INS.play(applicationContext, BatteryBroadCastReceiver.firstBattery)
    }

    private fun setDimension() {
        //视频比例
        val videoProportion = 1f
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val screenProportion = screenHeight.toFloat() / screenWidth.toFloat()
        val lp: ViewGroup.LayoutParams = video.layoutParams
        if (videoProportion < screenProportion) {
            lp.height = screenHeight
            lp.width = (screenHeight.toFloat() / videoProportion).toInt()
        } else {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() * videoProportion).toInt()
        }
        video.layoutParams = lp
        //电量位置
        val clp = info.layoutParams as ConstraintLayout.LayoutParams
        val size = info.textSize
        clp.apply {
            val fm = Paint().apply {
                textSize = size
            }.fontMetrics
            val textHeight = (ceil(fm.descent - fm.top) + size).toInt()
            //UP_TODO 2021/4/9 3:40 0 这里的算法可能还需要进一步优化
            verticalBias = 0.153f - textHeight * 1f / screenHeight / 4
        }
    }


    private fun chargeConfig() {
        BatteryBroadCastReceiver.addBatteryListener(this)

        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        window.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        (if (SettingsFragmentCompat.isKeepShow) WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON else 0)
            )
            decorView.also {
                it.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ChargeAudioManager.INS.stopMedia()
        BatteryBroadCastReceiver.removeBatteryListener()
        finish()
    }

    override fun onPowerConnected() {

    }

    override fun onPowerDisconnected() {
        finish()
    }

    override fun onPowerChange(battery: Int) {
        setBattery(battery)
    }

    private fun setBattery(battery: Int) {
        val res = "$battery%"
        val sp = SpannableString(res).apply {
            setSpan(
                AbsoluteSizeSpan(15, true),
                res.length - 1,
                res.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        info.text = sp
    }
}