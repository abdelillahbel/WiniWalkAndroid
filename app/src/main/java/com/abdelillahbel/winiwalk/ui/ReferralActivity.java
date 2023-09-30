package com.abdelillahbel.winiwalk.ui;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.razir.progressbutton.ButtonTextAnimatorExtensionsKt;
import com.github.razir.progressbutton.DrawableButtonExtensionsKt;
import com.github.razir.progressbutton.ProgressButtonHolderKt;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.abdelillahbel.winiwalk.R;
import com.shashank.sony.fancydialoglib.Animation;
import com.shashank.sony.fancydialoglib.FancyAlertDialog;

import java.util.Objects;

import kotlin.Unit;

public class ReferralActivity extends AppCompatActivity {
    private final int rewardAmount = 5;
    boolean redeemCodeUsed;
    Button rewardBtn;
    private FloatingActionButton fab_back;
    private int refereeCoinsAmount;
    // private Button rewardBtn;
    private EditText refCodeInput;
    private String referralCode, currentUserEmail;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef, usersRef, referralCodeShareCountRef, redeemCodeUsedRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral);

        rewardBtn = findViewById(R.id.claim_reward_btn);
        refCodeInput = findViewById(R.id.ref_code_input);
        fab_back = findViewById(R.id.fab_back_ref_activity);

        // button animation
        ProgressButtonHolderKt.bindProgressButton(this, rewardBtn);
        ButtonTextAnimatorExtensionsKt.attachTextChangeAnimator(rewardBtn);

        // initialize rootRef for firebase database / mAuth
        RootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        referralCodeShareCountRef = RootRef.child("Data").child("Users").child(currentUser.getUid()).child("referralCodeShareCount");
        redeemCodeUsedRef = RootRef.child("Data").child("Users").child(currentUser.getUid()).child("redeemCodeUsed");
        usersRef = RootRef.child("Data").child("Users");
        currentUserEmail = currentUser.getEmail();

        redeemCodeUsedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                redeemCodeUsed = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        // return back
        fab_back.setOnClickListener(view -> {
            super.onBackPressed();
        });

        // get reward button
        rewardBtn.setOnClickListener(view1 -> {

            // Check if the referral code has already been used
            if (refCodeInput.getText().toString().trim().isEmpty()) {
                hideKeyboard();
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Please enter referral code", Snackbar.LENGTH_LONG);
                snackbar.show();
            } else if (redeemCodeUsed) {
                Toast.makeText(this, "Referral code has already been used.", Toast.LENGTH_SHORT).show();
            } else {
                checkAndRedeemCode();
            }
        });


        // get coins amount from db
        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("coinsAmount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    refereeCoinsAmount = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getValue(String.class)));
                    refereeCoinsAmount += rewardAmount;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });

    }

    private void checkAndRedeemCode() {
        showProgressRight(rewardBtn);

        referralCode = refCodeInput.getText().toString().trim();
        // Search for the user with the given referral code
        Query query = FirebaseDatabase.getInstance().getReference().child("Data").child("Users").orderByChild("referralCode").equalTo(referralCode);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // add any other fields you want to retrieve
                        // Found a user with the specified refCode
                        // Do something with the user's data
                        // Get the user's data
                        String userEmail = snapshot.child("email").getValue(String.class);
                        int coinsAmount = Integer.parseInt(Objects.requireNonNull(snapshot.child("coinsAmount").getValue(String.class)));
                        int referralCodeShareCount = Integer.parseInt(Objects.requireNonNull(snapshot.child("referralCodeShareCount").getValue(String.class)));

                        // Check if the referral code belongs to someone else
                        assert userEmail != null;
                        if (userEmail.equals(currentUserEmail)) {
                            hideKeyboard();
                            hideProgressRight(rewardBtn);
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "You cannot use your own referral code.", Snackbar.LENGTH_LONG);
                            snackbar.show();
                            return;
                        }
                        // Update the referrer's coins amount and redeem code used flag
                        usersRef.child(Objects.requireNonNull(snapshot.getKey())).child("coinsAmount").setValue(String.valueOf(coinsAmount + rewardAmount));
                        usersRef.child(Objects.requireNonNull(snapshot.getKey())).child("referralCodeShareCount").setValue(String.valueOf(referralCodeShareCount + 1));

                        // Update the referee's coins amount
                        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("coinsAmount").setValue(String.valueOf(refereeCoinsAmount));
                        redeemCodeUsedRef.setValue(true).addOnSuccessListener(unused -> {
                            hideKeyboard();
                            hideProgressRight(rewardBtn);
                            refCodeInput.setText("");

                            FancyAlertDialog.Builder
                                    .with(ReferralActivity.this)
                                    .setTitle("Good")
                                    .setBackgroundColorRes(R.color.green) // for @ColorRes use setBackgroundColorRes(R.color.colorvalue)
                                    .setMessage("Referral code accepted. 5 coins added to your account.")
                                    .setPositiveBtnBackgroundRes(R.color.alert_dialog_button_color)  // for @ColorRes use setPositiveBtnBackgroundRes(R.color.colorvalue)
                                    .setPositiveBtnText("Okay")
                                    .setNegativeBtnText("Cancel")
                                    .setNegativeBtnBackgroundRes(R.color.btn_disabled)
                                    .setAnimation(Animation.SLIDE)
                                    .isCancellable(false)
                                    .onPositiveClicked(dialog -> {
                                        dialog.cancel();
                                        onBackPressed();
                                    })
                                    .build()
                                    .show();
                        });

                        return;
                    }
                } else {
                    // referral code not found
                    hideKeyboard();
                    hideProgressRight(rewardBtn);

                    FancyAlertDialog.Builder
                            .with(ReferralActivity.this)
                            .setTitle("Oops..")
                            .setBackgroundColorRes(R.color.red) // for @ColorRes use setBackgroundColorRes(R.color.colorvalue)
                            .setMessage("Invalid referral code.")
                            .setPositiveBtnBackgroundRes(R.color.green)  // for @ColorRes use setPositiveBtnBackgroundRes(R.color.colorvalue)
                            .setPositiveBtnText("Okay")
                            .setNegativeBtnText("Cancel")
                            .setNegativeBtnBackgroundRes(R.color.btn_disabled)
                            .setAnimation(Animation.SLIDE)
                            .isCancellable(false)
                            .onPositiveClicked(Dialog::cancel)
                            .build()
                            .show();


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // handle errors
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focusedView = getCurrentFocus();
        if (focusedView != null) {
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    private void showProgressRight(final Button button) {
        DrawableButtonExtensionsKt.showProgress(button, progressParams -> {
            progressParams.setButtonTextRes(R.string.loading);
            progressParams.setProgressColor(Color.BLACK);
            return Unit.INSTANCE;
        });
        button.setEnabled(false);
    }

    private void hideProgressRight(final Button button) {
        button.setEnabled(true);
        DrawableButtonExtensionsKt.hideProgress(button, R.string.progressRight);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}