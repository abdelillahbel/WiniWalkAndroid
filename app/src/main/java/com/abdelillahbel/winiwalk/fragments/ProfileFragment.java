package com.abdelillahbel.winiwalk.fragments;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.abdelillahbel.winiwalk.ui.ProfileActivity;
import com.abdelillahbel.winiwalk.R;
import com.abdelillahbel.winiwalk.ui.ReferralActivity;
import com.abdelillahbel.winiwalk.ui.auth.LoginActivity;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {


    TextView btn_get_reward;
    boolean redeemCodeUsed;
    private String currentUserEmail;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef, usersRef, referralCodeShareCountRef, redeemCodeUsedRef;
    private FirebaseUser currentUser;
    private TextView mFullNameTextView , logoutBtn;
    private CircleImageView mProfileImageView;


    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize rootRef for firebase database / mAuth
        RootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        referralCodeShareCountRef = RootRef.child("Data").child("Users").child(currentUser.getUid()).child("referralCodeShareCount");
        redeemCodeUsedRef = RootRef.child("Data").child("Users").child(currentUser.getUid()).child("redeemCodeUsed");
        currentUserEmail = currentUser.getEmail();

        usersRef = RootRef.child("Data").child("Users");


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        btn_get_reward =  view.findViewById(R.id.redeemReward);
        logoutBtn  = view.findViewById(R.id.logout_btn);

        redeemCodeUsedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                redeemCodeUsed = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (redeemCodeUsed) {
                    btn_get_reward.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mFullNameTextView = view.findViewById(R.id.textView_fullname);
        mProfileImageView = view.findViewById(R.id.profile_pic);
        ConstraintLayout account_item = view.findViewById(R.id.account_item_view);

        // intent to profile
        account_item.setOnClickListener(view12 -> {
            Intent i_to_profile = new Intent(getContext(), ProfileActivity.class);
            startActivity(i_to_profile);
        });


        btn_get_reward.setOnClickListener(view1 -> {
            Intent i_to_referral = new Intent(getContext(), ReferralActivity.class);
            startActivity(i_to_referral);
        });

// get full name from firebase db
        RootRef.child("Data").child("Users").child(Objects.requireNonNull(mAuth.getUid())).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    String fullName = dataSnapshot.child("displayName").getValue(String.class);
                    mFullNameTextView.setText(fullName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error retrieving data", databaseError.toException());
            }
        });
        // get profile pic from firebase db
        RootRef.child("Data").child("Users").child(mAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String profilePicUrl = dataSnapshot.child("photoUrl").getValue(String.class);
                    Picasso.get().load(profilePicUrl).into(mProfileImageView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error retrieving data", databaseError.toException());
            }
        });

        logoutBtn.setOnClickListener(view13 -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });


        return view;
    }


}