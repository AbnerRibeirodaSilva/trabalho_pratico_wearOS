package com.example.trabalho2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.widget.BoxInsetLayout
import kotlinx.coroutines.*

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private lateinit var txtHeartRate: TextView
    private var simulate = true
    private var simulationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = BoxInsetLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            setPadding(16, 16, 16, 16)
        }

        txtHeartRate = TextView(this).apply {
            text = "-- bpm"
            textSize = 28f
            setTextColor(Color.parseColor("#FF4444"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        layout.addView(txtHeartRate)
        setContentView(layout)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1001)
        } else {
            initSensor()
        }
    }

    private fun initSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor == null) {
            Toast.makeText(this, "Sensor não disponível — modo simulado", Toast.LENGTH_LONG).show()
            startSimulation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!simulate) {
            heartRateSensor?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val bpm = event.values[0].toInt()
            updateHeartRate(bpm)
        }
    }

    private fun updateHeartRate(bpm: Int) {
        txtHeartRate.text = "$bpm bpm"
        if (bpm > 120) {
            alertHighHeartRate()
        }
    }

    private fun startSimulation() {
        simulate = true
        simulationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val fakeBpm = (70..140).random()
                updateHeartRate(fakeBpm)
                delay(2000)
            }
        }
    }

    private fun alertHighHeartRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }

        Toast.makeText(this, "⚠️ Batimentos altos detectados!", Toast.LENGTH_SHORT).show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
