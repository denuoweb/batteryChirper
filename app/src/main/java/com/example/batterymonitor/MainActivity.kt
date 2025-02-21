package com.example.batterymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tilGeneralThreshold: TextInputLayout
    private lateinit var tilCriticalThreshold: TextInputLayout
    private lateinit var tilCooldown: TextInputLayout
    private lateinit var tilCriticalLower: TextInputLayout
    private lateinit var tilCriticalUpper: TextInputLayout
    private lateinit var tilChirpDuration: TextInputLayout

    private lateinit var etGeneralThreshold: TextInputEditText
    private lateinit var etCriticalThreshold: TextInputEditText
    private lateinit var etCooldown: TextInputEditText
    private lateinit var etCriticalLower: TextInputEditText
    private lateinit var etCriticalUpper: TextInputEditText
    private lateinit var etChirpDuration: TextInputEditText

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnTestChirp: Button
    private lateinit var tvStatus: android.widget.TextView

    // Receiver to update UI with battery status from the service
    private val batteryStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val batteryPct = intent?.getFloatExtra("batteryPct", 0f) ?: 0f
            val dischargeRate = intent?.getDoubleExtra("dischargeRate", 0.0) ?: 0.0
            tvStatus.text = "Battery: $batteryPct%\nDischarge Rate: ${"%.2f".format(dischargeRate)}%/min"
        }
    }

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        // Initialize the status TextView
        tvStatus = findViewById(R.id.tvStatus)

        // Initialize TextInputLayouts and their EditTexts
        tilGeneralThreshold = findViewById(R.id.tilGeneralThreshold)
        tilCriticalThreshold = findViewById(R.id.tilCriticalThreshold)
        tilCooldown = findViewById(R.id.tilCooldown)
        tilCriticalLower = findViewById(R.id.tilCriticalLower)
        tilCriticalUpper = findViewById(R.id.tilCriticalUpper)
        tilChirpDuration = findViewById(R.id.tilChirpDuration)

        etGeneralThreshold = findViewById(R.id.etGeneralThreshold)
        etCriticalThreshold = findViewById(R.id.etCriticalThreshold)
        etCooldown = findViewById(R.id.etCooldown)
        etCriticalLower = findViewById(R.id.etCriticalLower)
        etCriticalUpper = findViewById(R.id.etCriticalUpper)
        etChirpDuration = findViewById(R.id.etChirpDuration)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnTestChirp = findViewById(R.id.btnTestChirp)

        // Optionally set default values if desired (or leave fields empty to show hints)
        etGeneralThreshold.setText("5.0")
        etCriticalThreshold.setText("7.0")
        etCooldown.setText("60000")
        etCriticalLower.setText("15.0")
        etCriticalUpper.setText("60.0")
        etChirpDuration.setText("10000") // 10 seconds

        btnStart.setOnClickListener {
            val generalThreshold = etGeneralThreshold.text.toString().toDoubleOrNull() ?: 5.0
            val criticalThreshold = etCriticalThreshold.text.toString().toDoubleOrNull() ?: 7.0
            val cooldown = etCooldown.text.toString().toLongOrNull() ?: 60000L
            val criticalLower = etCriticalLower.text.toString().toDoubleOrNull() ?: 15.0
            val criticalUpper = etCriticalUpper.text.toString().toDoubleOrNull() ?: 60.0
            val chirpDuration = etChirpDuration.text.toString().toLongOrNull() ?: 10000L

            // Start the BatteryMonitorService with these parameters
            val serviceIntent = Intent(this, BatteryMonitorService::class.java).apply {
                putExtra("generalThreshold", generalThreshold)
                putExtra("criticalThreshold", criticalThreshold)
                putExtra("cooldown", cooldown)
                putExtra("criticalRangeLower", criticalLower)
                putExtra("criticalRangeUpper", criticalUpper)
                putExtra("chirpDuration", chirpDuration)
            }
            startService(serviceIntent)
            Toast.makeText(this, "Battery Monitor Service started", Toast.LENGTH_SHORT).show()
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, BatteryMonitorService::class.java))
            Toast.makeText(this, "Battery Monitor Service stopped", Toast.LENGTH_SHORT).show()
        }

        btnTestChirp.setOnClickListener {
            // Send an intent with extra "test" to trigger the chirp sound immediately
            val testIntent = Intent(this, BatteryMonitorService::class.java).apply {
                putExtra("test", true)
            }
            startService(testIntent)
            Toast.makeText(this, "Test chirp triggered", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed normally
            } else {
                // Permission denied, show a message or disable notification features
                Toast.makeText(this, "Notification permission is required for proper functioning.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register receiver to update UI with battery status
        registerReceiver(batteryStatusReceiver, IntentFilter("com.example.batterymonitor.BATTERY_STATUS"))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryStatusReceiver)
    }
}

