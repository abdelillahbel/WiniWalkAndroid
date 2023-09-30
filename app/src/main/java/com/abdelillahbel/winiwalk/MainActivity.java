package com.abdelillahbel.winiwalk;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.abdelillahbel.winiwalk.fragments.ProfileFragment;
import com.abdelillahbel.winiwalk.fragments.HomeFragment;
import com.abdelillahbel.winiwalk.fragments.StatisticsFragment;
import com.abdelillahbel.winiwalk.fragments.WalletFragment;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

public class MainActivity extends AppCompatActivity {
    static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;
    private static final int REQUEST_SENSOR_PERMISSION = 1001;
    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                // Do nothing if network is connected
            } else {
                // Show alert dialog if network is disconnected
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("No internet connection");
                builder.setCancelable(false);
                builder.setMessage("Please enable network connection to continue");
                builder.setPositiveButton("OK", (dialog, which) -> {
                    Intent intent1 = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    startActivity(intent1);
                });
                builder.create().show();
            }
        }
    };
    ChipNavigationBar chipNavigationBar;
    private StepCountService stepCountService;
    private DatabaseReference RootRef;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private int stepsHistoryCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isServiceRunning()) {
            Intent serviceIntent = new Intent(this, StepCountService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
        setContentView(R.layout.activity_main);


        // requestFitDataAccess();

        // initialize rootRef for firebase database / mAuth
        RootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("stepsHistory").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                stepsHistoryCount++;
                chipNavigationBar.showBadge(R.id.bottom_nav_Statistics, stepsHistoryCount);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // This method is triggered when a child is updated.
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // This method is triggered when a child is removed.
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // This method is triggered when a child's position is changed.
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // This method is triggered when there is an error in the database operation.
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });


        chipNavigationBar = findViewById(R.id.bottomNav);
        chipNavigationBar.setItemSelected(R.id.bottom_nav_dashboard, true);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view, new HomeFragment()).commit();
        bottomMenu();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // check ACTIVITY_RECOGNITION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
                //ask for permission
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
            }
        }

        // check gps and pedometer
        checkGPSandPedometer();

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // internet check
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, intentFilter);
        // check if service is running or no
        if (!isServiceRunning()) {
            Intent serviceIntent = new Intent(this, StepCountService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }


    @Override
    public void onBackPressed() {
        // Get the current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_view);

        // If the current fragment is not the HomeFragment, navigate back to it
        if (!(currentFragment instanceof HomeFragment)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view, new HomeFragment())
                    .commit();
            chipNavigationBar.setItemSelected(R.id.bottom_nav_dashboard, true);
        } else {
            super.onBackPressed();
        }
    }

    private void bottomMenu() {


        chipNavigationBar.setOnItemSelectedListener(i -> {
            Fragment fragment = null;
            switch (i) {
                case R.id.bottom_nav_dashboard:
                    fragment = new HomeFragment();
                    break;
                case R.id.bottom_nav_Statistics:
                    fragment = new StatisticsFragment();
                    chipNavigationBar.dismissBadge(R.id.bottom_nav_Statistics);
                    break;
                case R.id.bottom_nav_wallet:
                    fragment = new WalletFragment();
                    break;
                case R.id.bottom_nav_account:
                    fragment = new ProfileFragment();
                    break;
            }
            //getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view, fragment).commit();
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container_view, fragment)
                        .commit();
            }
        });
    }


    //


    private void requestFitDataAccess() {
        // Build the FitnessOptions specifying the data types you want to access
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                // Add more data types as needed
                .build();

        // Check if the user has granted permission to access Fit data
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            // The user has not granted permission, so request it
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            // The user has already granted permission, proceed with accessing Fit data
            accessFitData();
        }
    }


    private void accessFitData() {
        // You can access Fit data here
        // Subscribe to data types, read data, update Firebase, etc.
        // ...
    }


    //


    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (StepCountService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void requestPermissions() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            // accessGoogleFit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // User granted permission, proceed with accessing Fit data
                accessFitData();
            } else {
                // User denied permission or an error occurred, handle accordingly
                // ...
            }
        }
    }


    private void checkGPSandPedometer() {
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
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BODY_SENSORS}, REQUEST_SENSOR_PERMISSION);
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
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BODY_SENSORS}, REQUEST_SENSOR_PERMISSION);
                }).setNegativeButton("Cancel", (dialog, id) -> {
                    // handle "cancel" button
                    dialog.dismiss();
                });
                builder.create().show();
            } else {
                // GPS and pedometer are both enabled
                // Continue with app functionality
            }
        }
    }
}