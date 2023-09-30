package com.abdelillahbel.winiwalk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abdelillahbel.winiwalk.fragments.StatisticsFragment;

import java.util.ArrayList;

public class StepsListCustomAdapter extends RecyclerView.Adapter<StepsListCustomAdapter.ViewHolder> {

    // List to store all the contact details
    private final ArrayList<StepsListDataModel> stepsList;
    // Method to add animation to the view
    private int lastPosition = -1;

    // Constructor for the Class
    public StepsListCustomAdapter(ArrayList<StepsListDataModel> stepsList, StatisticsFragment statisticsFragment) {
        this.stepsList = stepsList;
    }

    // This method creates views for the RecyclerView by inflating the layout
    // Into the viewHolders which helps to display the items in the RecyclerView
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        // Inflate the layout view you have created for the list rows here
        View view = layoutInflater.inflate(R.layout.steps_list_row_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return stepsList == null ? 0 : stepsList.size();
    }

    // This method is called when binding the data to the views being created in RecyclerView
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final StepsListDataModel dataModel = stepsList.get(position);

        // Set the data to the views here
        holder.setWalked_steps(dataModel.getWalked_steps());

        // Set animation to the view here
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.up_from_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    // This is your ViewHolder class that helps to populate data to the view
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtWalkedSteps;

        public ViewHolder(View itemView) {
            super(itemView);
            txtWalkedSteps = itemView.findViewById(R.id.txt_steps_walked);
        }

        public void setWalked_steps(String walked_steps) {
            txtWalkedSteps.setText(walked_steps);
        }
    }
}
