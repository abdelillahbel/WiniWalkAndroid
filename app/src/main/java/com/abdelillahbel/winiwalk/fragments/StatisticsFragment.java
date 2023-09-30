package com.abdelillahbel.winiwalk.fragments;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.abdelillahbel.winiwalk.R;
import com.abdelillahbel.winiwalk.StepsListCustomAdapter;
import com.abdelillahbel.winiwalk.StepsListDataModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.wasabeef.recyclerview.animators.FadeInDownAnimator;


public class StatisticsFragment extends Fragment {

    private final ArrayList<StepsListDataModel> stepsList = new ArrayList<>();
    TextView emptyView;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference RootRef, stepsHistoryRef;
    private StepsListCustomAdapter listAdapter;
    private RecyclerView recycler;
    private Button clear_list;

    public StatisticsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // firebase initialization

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        RootRef = FirebaseDatabase.getInstance().getReference();
        stepsHistoryRef = RootRef.child("Data").child("Users").child(currentUser.getUid()).child("stepsHistory");
        stepsHistoryRef.keepSynced(true);


    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        recycler = view.findViewById(R.id.steps_history_list);
        emptyView = view.findViewById(R.id.txt_nothing_here);
        clear_list = view.findViewById(R.id.btn_clear_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler.setLayoutManager(layoutManager);
        listAdapter = new StepsListCustomAdapter(stepsList, this);
        recycler.setAdapter(listAdapter);
        // Add the item animator
        recycler.setItemAnimator(new FadeInDownAnimator());

        stepsHistoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear the list to remove previous data
                stepsList.clear();

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    // Get the step count value from the snapshot and keep it as string
                    String stepsWalked = childSnapshot.getValue(String.class);

                    // Create a StepsListDataModel object with the step count value
                    StepsListDataModel dataModel = new StepsListDataModel(stepsWalked);
                    // Add the StepsListDataModel object to the list
                    stepsList.add(dataModel);
                }

                // Reverse the order of the ArrayList
                Collections.reverse(stepsList);


                // Notify the adapter that the data has changed
                listAdapter.notifyDataSetChanged();

                // Check if the RecyclerView is empty
                if (recycler.getAdapter() == null || recycler.getAdapter().getItemCount() == 0) {
                    // If it's empty, make the TextView visible
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    // If it's not empty, make the TextView gone
                    emptyView.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read value.", databaseError.toException());
            }
        });
        // end

        // clear recycler view items and delete childs from db
        clear_list.setOnClickListener(view1 -> {
            clearData();
            // Notify the adapter that the dataset has changed
            listAdapter.notifyDataSetChanged();

        });

        return view;
    }

    private void clearData() {

        // Attach a ValueEventListener to the reference
        stepsHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Create a list to store the child nodes
                List<DataSnapshot> childNodes = new ArrayList<>();

                // Iterate through the dataSnapshot to retrieve child nodes
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    childNodes.add(childSnapshot);
                }
                // Remove each child node from the database
                for (DataSnapshot childNode : childNodes) {
                    childNode.getRef().removeValue();
                }
                // Clear the list used by the RecyclerView adapter
                stepsList.clear();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error if needed
            }
        });
    }

}