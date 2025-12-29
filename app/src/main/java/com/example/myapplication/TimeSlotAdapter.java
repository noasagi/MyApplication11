package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.SlotViewHolder> {

    private List<String> timeSlots = new ArrayList<>();
    private int selectedPosition = -1; // אף שעה לא נבחרה בהתחלה
    private final OnSlotClickListener listener;

    // ממשק להעברת הלחיצה לאקטיביטי
    public interface OnSlotClickListener {
        void onSlotClick(String time);
    }

    public TimeSlotAdapter(OnSlotClickListener listener) {
        this.listener = listener;
    }

    public void setTimeSlots(List<String> slots) {
        this.timeSlots = slots;
        this.selectedPosition = -1; // איפוס בחירה כשמחליפים יום
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        String time = timeSlots.get(position);
        holder.tvTime.setText(time);

        // שינוי צבע אם נבחר
        if (selectedPosition == position) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#6200EE")); // סגול (נבחר)
            holder.tvTime.setTextColor(Color.WHITE);
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvTime.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged(); // רענון הרשימה כדי לעדכן צבעים
            listener.onSlotClick(time);
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        CardView cardView;

        public SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTimeSlot);
            cardView = itemView.findViewById(R.id.cardSlot);
        }
    }
}