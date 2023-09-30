package com.abdelillahbel.winiwalk;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class StepCountService extends Service implements SensorEventListener {

    private static final String TAG = "StepCounterService";
    private static final float ALPHA = 0.8f;
    private static final int MIN_STEP_THRESHOLD = 10;
    private static final int MAX_STEP_THRESHOLD = 40;
    private static final long STEP_TIME_INTERVAL = 1000L; // 1 second
    private final float[] gravity = new float[3];
    private final float[] linearAcceleration = new float[3];
    private final BroadcastReceiver sensorChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("omsensorchange")) {
                int steps = intent.getIntExtra("steps", 0);
                updateNotification(steps);
            }
        }
    };
    private SharedPreferences stepCountCache;
    private long lastStepTime = 0L;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;

    private int dailySteps;


    private Sensor stepSensor;


    private int stepCount;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String stepsHistoryKey;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {// Do something when the day changes, for example:
                checkAndUpdateStepsCount(currentUser.getUid());
            }
        }
    };
    private final Thread mWorkerThread = new Thread(() -> {
        while (true) {
            try {
                // Sleep for 1 minute
                Thread.sleep(60000);

                // Get the current date
                Calendar now = Calendar.getInstance();
                int currentDay = now.get(Calendar.DAY_OF_YEAR);

                // Get the previously recorded day
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                int lastRecordedDay = sharedPreferences.getInt("lastRecordedDay", 0);

                // Check if the current day is different from the previous day
                if (currentDay != lastRecordedDay) {
                    // Update the last recorded day
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("lastRecordedDay", currentDay);
                    editor.apply();

                    // Send a message to the handler
                    mHandler.sendEmptyMessage(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });
    private DatabaseReference RootRef;


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.abdelillahbel.winiwalk";
            String channelName = "Background Proximity Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setSound(null, null); // Set the notification sound to null
            chan.enableVibration(false); // Disable vibration
            chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(chan);
            }
        }
    }

    private void startMyOwnForeground() {
        // Register the sensor change receiver
        IntentFilter filter = new IntentFilter("omsensorchange");
        if (getApplicationContext().getPackageManager().queryBroadcastReceivers(new Intent("omsensorchange"), 0).size() == 0) {
            registerReceiver(sensorChangeReceiver, filter);
        }

        createNotificationChannel();

        String NOTIFICATION_CHANNEL_ID = "com.abdelillahbel.winiwalk";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true).setSmallIcon(R.drawable.logo_white).setContentTitle("Steps count").setContentText("0 steps taken").setVisibility(NotificationCompat.VISIBILITY_SECRET).setCategory(Notification.CATEGORY_SERVICE).setPriority(NotificationCompat.PRIORITY_LOW).setAutoCancel(false).setSound(null).setVibrate(null).build();
        startForeground(1, notification);
    }

    private void updateNotification(int steps) {
        String channelId = "com.abdelillahbel.winiwalk";
        @SuppressLint("DefaultLocale") String contentText = String.format("%d steps taken", steps);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo_white)
                .setContentTitle("Steps Today")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, notification);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the SharedPreferences instance
        stepCountCache = getSharedPreferences("StepCountCache", Context.MODE_PRIVATE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        mWorkerThread.start();

        // firebase initialization
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        RootRef = FirebaseDatabase.getInstance().getReference();
        stepsHistoryKey = RootRef.child("Data").child("Users").child(currentUser.getUid()).child("stepsHistory").push().getKey();

        // get step count from Db
        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("stepCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    stepCount = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getValue(String.class)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });

        // daily steps from Db
        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("dailySteps").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    dailySteps = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getValue(String.class)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMyOwnForeground();
        return START_STICKY;
    }

    private void checkAndUpdateStepsCount(final String currentUserID) {
        final DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Data").child("Users").child(currentUserID).child("lastDay").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    String lastRecordedDate = dataSnapshot.getValue().toString();
                    if (!lastRecordedDate.equals(getCurrentDate())) {
                        // Current date is different from the previous date, reset the steps count
                        RootRef.child("Data").child("Users").child(currentUserID).child("stepsHistory").child(stepsHistoryKey).setValue(String.valueOf(dailySteps));
                        stepCount += dailySteps;
                        RootRef.child("Data").child("Users").child(currentUserID).child("stepCount").setValue(String.valueOf(stepCount));
                        RootRef.child("Data").child("Users").child(currentUserID).child("dailySteps").setValue("0");
                        RootRef.child("Data").child("Users").child(currentUserID).child("lastDay").setValue(getCurrentDate());
                    }
                } else {
                    // lastDay field does not exist, create it
                    RootRef.child("Data").child("Users").child(currentUserID).child("lastDay").setValue(getCurrentDate());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int steps = (int) event.values[0];
            //   Toast.makeText(this, "This device use Pedometer", Toast.LENGTH_SHORT).show();
            updateStepsInFirebase(steps);
            updateNotification(dailySteps);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //   Toast.makeText(this, "This device use Accelerometer", Toast.LENGTH_SHORT).show();
            detectStepFromAccelerometer(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void detectStepFromAccelerometer(SensorEvent event) {
        final float alpha = ALPHA;

        // Apply low-pass filter to remove gravity
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Subtract gravity from acceleration
        linearAcceleration[0] = event.values[0] - gravity[0];
        linearAcceleration[1] = event.values[1] - gravity[1];
        linearAcceleration[2] = event.values[2] - gravity[2];

        double magnitude = Math.sqrt(linearAcceleration[0] * linearAcceleration[0]
                + linearAcceleration[1] * linearAcceleration[1]
                + linearAcceleration[2] * linearAcceleration[2]);

        // Apply step detection logic
        if (magnitude > MIN_STEP_THRESHOLD && magnitude < MAX_STEP_THRESHOLD) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastStepTime > STEP_TIME_INTERVAL) {
                dailySteps++;
                lastStepTime = currentTime;
                // Update the step count in Firebase
                updateStepsInFirebase(dailySteps);
                // Update the notification with the new step count
                updateNotification(dailySteps);
                // Store the step count in the app cache
                //storeStepCountInCache(dailySteps);
            }
        }
    }

    private void storeStepCountInCache(int steps) {
        // Get the current cached step count
        int cachedSteps = stepCountCache.getInt("dailySteps", 0);
        // Add the new steps to the cached count
        cachedSteps += steps;
        // Store the updated step count in the cache
        SharedPreferences.Editor editor = stepCountCache.edit();
        editor.putInt("dailySteps", cachedSteps);
        editor.apply();
    }

    private void syncStepCountWithFirebase() {
        // Retrieve the step count from the cache
        int cachedSteps = stepCountCache.getInt("dailySteps", 0);
        // Update Firebase with the cached step count
        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("dailySteps").setValue(String.valueOf(cachedSteps))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Step count synced successfully, clear the cache
                        clearStepCountCache();
                    } else {
                        // Failed to sync step count with Firebase, handle the error
                        Log.e(TAG, "Failed to sync step count with Firebase", task.getException());
                    }
                });
    }

    private void clearStepCountCache() {
        // Clear the step count cache
        dailySteps = 0;
    }

    private void updateStepsInFirebase(int dailySteps) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            RootRef.child("Data").child("Users").child(currentUser.getUid()).child("dailySteps").setValue(String.valueOf(dailySteps));
        }
    }

    private String getCurrentDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }
}