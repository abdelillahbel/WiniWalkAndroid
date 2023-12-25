package com.abdelillahbel.winiwalk.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.abdelillahbel.winiwalk.R
import com.abdelillahbel.winiwalk.ui.auth.LoginActivity

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                //ask for permission
                requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 0)
            }
        }
        val textView = findViewById<TextView>(R.id.get_started_txt)
        val txt_title_get_started = findViewById<TextView>(R.id.get_started_title)
        val get_started = findViewById<Button>(R.id.get_started_btn)

        // Animation
        YoYo.with(Techniques.SlideInLeft)
            .duration(1600)
            .playOn(txt_title_get_started)
        YoYo.with(Techniques.SlideInLeft)
            .duration(1700)
            .playOn(textView)
        YoYo.with(Techniques.SlideInLeft)
            .duration(1800)
            .playOn(get_started)
        get_started.setOnClickListener { view: View? -> checkAndRequestPermissions() }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Permissions for API level 29 and above
                    val permissions = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION,
                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.BODY_SENSORS_BACKGROUND,
                        Manifest.permission.BODY_SENSORS,
                        Manifest.permission.ACCESS_NOTIFICATION_POLICY
                    )
                    requestPermissions(permissions, PERMISSION_REQUEST_CODE)
                }
            } else {
                // Permissions for API level 28 and below
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.BODY_SENSORS
                )
                requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            }
        }
        startActivity(Intent(this@StartActivity, LoginActivity::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted
            var allPermissionsGranted = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                // All permissions granted, proceed with your logic
                Toast.makeText(this, "Thanks", Toast.LENGTH_SHORT).show()
            } else {
                // Some permissions were not granted, handle accordingly
                Toast.makeText(this, "Please review settings", Toast.LENGTH_SHORT).show()
            }
        }
    } /* private void checkGPSandPedometer() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGPSEnabled) {
            // GPS is not enabled, request user to enable it
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("GPS is not enabled").setMessage("Please enable GPS to use this app").setPositiveButton("Enable GPS", (dialog, id) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
            builder.create().show();
        } else {
            // GPS is enabled, check for pedometer
            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (stepSensor == null) {
                    // Sensor permission not granted
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Pedometer is not available").setMessage("Please enable the pedometer to use this app").setPositiveButton("Ok", (dialog, id) -> {
                        // handle "Ok" button
                        ActivityCompat.requestPermissions(StartActivity.this, new String[]{Manifest.permission.BODY_SENSORS}, REQUEST_SENSOR_PERMISSION);
                    }).setNegativeButton("Cancel", (dialog, id) -> {
                        // handle "cancel" button
                        dialog.dismiss();
                    });
                    builder.create().show();
                }
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
                // Sensor permission not granted
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Pedometer is not available").setMessage("Please enable the pedometer to use this app").setPositiveButton("Ok", (dialog, id) -> {
                    // handle "Ok" button
                    ActivityCompat.requestPermissions(StartActivity.this, new String[]{Manifest.permission.BODY_SENSORS}, REQUEST_SENSOR_PERMISSION);
                }).setNegativeButton("Cancel", (dialog, id) -> {
                    // handle "cancel" button
                    dialog.dismiss();
                });
                builder.create().show();
            } else {
                // GPS and pedometer are both enabled
                // Continue with app functionality
                startActivity(new Intent(StartActivity.this, LoginActivity.class));
                finish();
            }
        }
    }*/

    companion object {
        private const val REQUEST_SENSOR_PERMISSION = 1001
        private const val PERMISSION_REQUEST_CODE = 1
    }
}