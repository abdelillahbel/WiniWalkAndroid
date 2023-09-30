package com.abdelillahbel.winiwalk.ui.auth;
/*
 * walkerz app v 1.0
 * by ..
 */

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.abdelillahbel.winiwalk.MainActivity;
import com.abdelillahbel.winiwalk.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 123;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private Button btSignInGoogle, btSignInApple;
    private FirebaseAuth mAuth;
    private String photoUrl;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;

    private DatabaseReference RootRef;

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        // initialize rootRef for firebase database / mAuth
        RootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // set up the privacy description text
        TextView textView = findViewById(R.id.privacy_txt);
        String text = "By creating this account, you agree to our Privacy Policy.";
        SpannableString spannableString = new SpannableString(text);

        // Find the indices of "Privacy Policy" and "Cookie Policy"
        int privacyPolicyIndex = text.indexOf("Privacy Policy");

        // Set clickable spans for "Privacy Policy" and "Cookie Policy"
        ClickableSpan privacyPolicyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                // Handle privacy policy click
                Toast.makeText(LoginActivity.this, "privacyPolicy", Toast.LENGTH_SHORT).show();
            }
        };

        spannableString.setSpan(privacyPolicyClickableSpan, privacyPolicyIndex, privacyPolicyIndex + "Privacy Policy".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the text view text with clickable spans
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance()); // Make links clickable
        // End here

        createRequest();

        // Assign variable
        btSignInGoogle = findViewById(R.id.bt_sign_in_google);
        btSignInApple = findViewById(R.id.bt_sign_in_apple);

        btSignInApple.setOnClickListener(view -> {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Available soon", Snackbar.LENGTH_LONG);
            snackbar.show();
        });

        btSignInGoogle.setOnClickListener(view -> signIn());

        signInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            int resultCode = result.getResultCode();
            Intent data = result.getData();

            if (resultCode == RESULT_OK && data != null) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                if (task.isSuccessful()) {
                    // Handle the successful sign-in
                    GoogleSignInAccount account = task.getResult();
                    // Retrieve the Google profile picture URL
                    photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";
                    firebaseAuthWithGoogle(account);
                } else {
                    // Handle the failed sign-in
                    Exception exception = task.getException();
                    if (exception != null) {
                        // Handle the exception
                        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                // Handle the failed sign-in
                Toast.makeText(this, "sorry, failed to sign in", Toast.LENGTH_SHORT).show();
            }
        });
        //end

    }

    private void createRequest() {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // No need to handle the result here since it will be handled by the ActivityResultLauncher
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success");

                // Get the current user
                FirebaseUser currentUser = mAuth.getCurrentUser();
                // Retrieve the Google profile picture URL

                if (currentUser != null) {
                    String currentUserID = currentUser.getUid();

                    // Check if the user exists in the database
                    RootRef.child("Data").child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // User already exists, do nothing
                                Log.d(TAG, "User already exists in the database");
                            } else {
                                // User does not exist, create new data for them
                                String emailName = Objects.requireNonNull(currentUser.getEmail()).substring(0, currentUser.getEmail().indexOf("@"));
                                createNewUser(currentUserID, currentUser.getEmail(), currentUser.getDisplayName(), emailName, photoUrl, getFirebaseDeviceToken());
                            }

                            // Start the HomeActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle error
                            Log.e(TAG, "Database error: " + databaseError.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Current user is null");
                }
            } else {
                // If sign in fails, display a message to the user.
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewUser(String userID, String email, String displayName, String emailName, String photoUrl, String deviceToken) {
        // Create user data in the database
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("displayName", displayName);
        userData.put("deviceToken", deviceToken);
        userData.put("stepCount", "0");
        userData.put("referralCodeShareCount", "0");
        userData.put("lastDay", getCurrentDate());
        userData.put("dailySteps", "0");
        userData.put("balance", "0");
        userData.put("gender", "");
        userData.put("age", "");
        userData.put("redeemCodeUsed", false);
        userData.put("userLevel", "Regular");
        userData.put("tasksDone", "0");
        userData.put("coinsAmount", "0");
        userData.put("accountType", "USER");
        userData.put("referralCode", emailName);
        userData.put("photoUrl", photoUrl);

        String countryCode = getCountryCode();
        if (countryCode != null) {
            userData.put("countryCode", countryCode);
        }
        // Add other fields as needed
        RootRef.child("Data").child("Users").child(userID).setValue(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "New user created in the database");
                } else {
                    Log.e(TAG, "Failed to create a new user in the database: " + task.getException());
                }
            }
        });
    }


    /*
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // create child in database to store user info

                    String currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                    String currentUserEmail = mAuth.getCurrentUser().getEmail();
                    String currentUserName = mAuth.getCurrentUser().getDisplayName();
                    String email = acct.getEmail();
                    assert email != null;
                    String emailName = email.substring(0, email.indexOf("@"));

                    RootRef.child("Data").child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // User already exists, update their data as needed
                                Toast.makeText(LoginActivity.this, "welcome back " + currentUserName, Toast.LENGTH_LONG).show();
                            } else {
                                // User does not exist, create new data for them
                                RootRef.child("Data").child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            // User already exists, update their data as needed
                                            Toast.makeText(LoginActivity.this, "Welcome back " + currentUserName, Toast.LENGTH_LONG).show();
                                        } else {
                                            // User does not exist, create new data for them
                                            // get device token
                                            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
                                                @Override
                                                public void onSuccess(String deviceToken) {
                                                    // save device token to user data in database
                                                    Map<String, Object> userData = new HashMap<>();
                                                    userData.put("deviceToken", deviceToken);
                                                    userData.put("stepCount", "0");
                                                    userData.put("referralCodeShareCount", "0");
                                                    userData.put("lastDay", getCurrentDate());
                                                    userData.put("dailySteps", "0");
                                                    userData.put("email", currentUserEmail);
                                                    userData.put("gender", "");
                                                    userData.put("age", "");
                                                    userData.put("redeemCodeUsed", false);
                                                    userData.put("userLevel", "1");
                                                    userData.put("coinsAmount", "0");
                                                    userData.put("accountType", "USER");
                                                    userData.put("referralCode", emailName);
                                                    userData.put("displayName", currentUserName);
                                                    String countryCode = getCountryCode();
                                                    if (countryCode != null) {
                                                        userData.put("countryCode", countryCode);
                                                    }
                                                    if (acct.getPhotoUrl() != null) {
                                                        String photoUrl = acct.getPhotoUrl().toString();
                                                        userData.put("photoUrl", photoUrl);
                                                    }
                                                    RootRef.child("Data").child("Users").child(currentUserID).setValue(userData);
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // handle failure
                                                    Toast.makeText(LoginActivity.this, "Error :" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        // Handle error
                                        Toast.makeText(LoginActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });


                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle error
                            Toast.makeText(LoginActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, "Sorry auth failed.", Toast.LENGTH_SHORT).show();
                }
                // ...
            }
        });
    }
*/


    private String getFirebaseDeviceToken() {
        final String[] deviceToken = {""};

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    // Token retrieval is successful
                    deviceToken[0] = token;
                })
                .addOnFailureListener(e -> {
                    // Token retrieval failed
                    Log.e(TAG, "Error retrieving device token: " + e.getMessage());
                });

        return deviceToken[0];
    }

    private String getCountryCode() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode;

        // Check for SIM network info
        if (tm != null) {
            countryCode = tm.getNetworkCountryIso();
            if (!TextUtils.isEmpty(countryCode)) {
                return countryCode.toUpperCase(Locale.getDefault());
            }
        }


        // Check for WiFi network info
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            WifiInfo wifiInfo = wm.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                if (!TextUtils.isEmpty(ssid)) {
                    // Remove double quotes from SSID if present
                    ssid = ssid.replace("\"", "");
                    Locale locale = Resources.getSystem().getConfiguration().locale;
                    String localeCountry = locale.getCountry();
                    try {
                        JSONObject jsonObj = new JSONObject(readFromUrl("http://api.geonames.org/searchJSON?formatted=true&q=" + ssid + "&maxRows=1&lang=" + localeCountry + "&username=YOUR_USERNAME"));
                        JSONArray jsonArray = jsonObj.getJSONArray("geonames");
                        if (jsonArray.length() > 0) {
                            JSONObject jsonGeo = jsonArray.getJSONObject(0);
                            countryCode = jsonGeo.getString("countryCode");
                            return countryCode.toUpperCase(Locale.getDefault());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Check for GPS info
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
                }
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        countryCode = addresses.get(0).getCountryCode();
                        if (!TextUtils.isEmpty(countryCode)) {
                            return countryCode.toUpperCase(Locale.getDefault());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // If no location info is available, return null
        return null;
    }

    private String readFromUrl(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        assert response.body() != null;
        return response.body().string();
    }

    private String getCurrentDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

}