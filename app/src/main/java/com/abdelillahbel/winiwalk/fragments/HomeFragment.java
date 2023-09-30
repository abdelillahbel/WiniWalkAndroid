package com.abdelillahbel.winiwalk.fragments;

import static android.content.ContentValues.TAG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.abdelillahbel.winiwalk.ClickableTextView;
import com.abdelillahbel.winiwalk.R;

import java.text.DecimalFormat;
import java.util.Objects;

public class HomeFragment extends Fragment {
    private float startX;
    private float startY;
    private boolean isSwiping = false;
    private TextView dailyStepCountTextView;
    private TextView stepCountTextView;
    private TextView coinsAmountTextView;
    private TextView lastDayStepsCountTextView;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference RootRef, coinsAmountRef;
    private int coinsAmount;
    private int stepCount;
    private GestureDetector gestureDetector;
    private ClickableTextView item_alert;
    private float xDelta;
    private float yDelta;
    private int dailySteps;
    private int lastDayStepsCount;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // add rotate anim to imageView
        // add it to coin view
        ImageView coinView = view.findViewById(R.id.coin_view);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(coinView, "rotation", 0f, -180f);
        rotation.setDuration(5000);
        rotation.setRepeatCount(ObjectAnimator.INFINITE);
        rotation.setRepeatMode(ObjectAnimator.REVERSE);
        rotation.start();

        // firebase initialization
        mAuth = FirebaseAuth.getInstance();

        currentUser = mAuth.getCurrentUser();

        RootRef = FirebaseDatabase.getInstance().getReference();
        coinsAmountRef = RootRef.child("Data").child("Users").child(currentUser.getUid()).child("coinsAmount");
        coinsAmountRef.keepSynced(true);
        // steps counter add/get/update in firebase database real-time
        dailyStepCountTextView = view.findViewById(R.id.daily_step_count_text_view);
        stepCountTextView = view.findViewById(R.id.step_count_text_view);
        coinsAmountTextView = view.findViewById(R.id.coins_amount_textview);
        lastDayStepsCountTextView = view.findViewById(R.id.last_day_steps_count_text_view);
        item_alert = view.findViewById(R.id.textView_item_alert);

        gestureDetector = new GestureDetector(getContext(), new MyGestureListener());

// swipe right or left item
        item_alert.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP && !gestureDetector.onTouchEvent(event)) {
                v.performClick();
            }
            return true;
        });


        // get coins amount from Db
        coinsAmountRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    coinsAmount = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getValue(String.class)));
                    coinsAmountTextView.setText(String.valueOf(coinsAmount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });

        // get lastDay steps from Db
        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("stepsHistory").orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        lastDayStepsCount = Integer.parseInt(Objects.requireNonNull(snapshot.getValue(String.class)));
                        lastDayStepsCountTextView.setText(String.valueOf(lastDayStepsCount));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });

        // get daily steps from Db
        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("dailySteps").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    dailySteps = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getValue(String.class)));

                    DecimalFormat formatter = new DecimalFormat("#,###");
                    String formattedStepCount = formatter.format(dailySteps);

                    dailyStepCountTextView.setText(formattedStepCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
        // get steps count from Db
        RootRef.child("Data").child("Users").child(currentUser.getUid()).child("stepCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    stepCount = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getValue(String.class)));

                    DecimalFormat formatter = new DecimalFormat("#,###");
                    String formattedStepCount = formatter.format(stepCount);

                    stepCountTextView.setText(formattedStepCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });

        return view;
    }

    private void animateTextViewHide(final TextView textView, float translationX) {
        textView.animate()
                .translationX(translationX)
                .setDuration(5000)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        textView.setVisibility(View.GONE);
                        textView.setTranslationX(0);
                    }
                })
                .start();
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 5000;

        @Override
        public boolean onDown(MotionEvent event) {
            startX = event.getRawX();
            startY = event.getRawY();
            isSwiping = false;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
            if (!isSwiping) {
                float deltaX = event2.getRawX() - startX;
                float deltaY = event2.getRawY() - startY;

                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    isSwiping = true;
                }
            }

            if (isSwiping) {
                float newX = event2.getRawX() - distanceX;
                item_alert.setX(newX);
            }

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isSwiping) {
                float deltaX = e2.getRawX() - startX;

                if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                    if (deltaX > 0 && velocityX > 0) {
                        // Swipe right, hide the TextView to the right
                        animateTextViewHide(item_alert, item_alert.getWidth());
                    } else if (deltaX < 0 && velocityX < 0) {
                        // Swipe left, hide the TextView to the left
                        animateTextViewHide(item_alert, -item_alert.getWidth());
                    }
                }
            }

            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            // Handle long press if needed
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            // Handle single tap if needed
            return true;
        }
    }

}