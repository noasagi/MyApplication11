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

// מחלקת מתאם (Adapter) מותאמת אישית לניהול והצגת משבצות הזמן הפנויות (Time Slots) לקביעת תור
public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.SlotViewHolder> {

    // רשימה דינמית המכילה את מחרוזות השעות הפנויות (לדוגמה: "10:30", "11:00")
    private List<String> timeSlots = new ArrayList<>();
    // משתנה השומר את מיקום (Position) משבצת הזמן שהמשתמש בחר כרגע. ערך 1- מציין שטרם בוצעה בחירה
    private int selectedPosition = -1;
    // משתנה המפנה לממשק (Interface) כדי להעביר את אירוע הלחיצה בחזרה לאקטיביטי המחזיק
    private final OnSlotClickListener listener;

    // הגדרת ממשק פנימי (Interface) המשמש כצינור תקשורת להעברת נתוני השעה שנבחרה אל מחוץ לאדפטר
    public interface OnSlotClickListener {
        void onSlotClick(String time);
    }

    // פעולה בונה (Constructor) המקבלת את מאזין הלחיצות ומאתחלת אותו
    public TimeSlotAdapter(OnSlotClickListener listener) {
        this.listener = listener;
    }

    // פונקציה לעדכון דינמי של רשימת השעות (למשל, כאשר הלקוח מחליף תאריך בלוח השנה)
    public void setTimeSlots(List<String> slots) {
        this.timeSlots = slots;
        this.selectedPosition = -1; // איפוס הבחירה חזרה ל-1- כדי שהשעה של היום הקודם לא תישאר מסומנת
        notifyDataSetChanged();     // פקודה המורה למערכת לרענן ולצייר מחדש את כל הרשימה על המסך
    }

    @NonNull
    @Override
    // פונקציה המופעלת על ידי המערכת כדי לייצר מחזיק רכיבים (ViewHolder) חדש עבור משבצת זמן בודדת
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינה ואינפלציה של קובץ ה-XML המעצב את משבצת השעה הפנויה (`item_time_slot`)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    // פונקציית הליבה המזריקה את הנתונים הגולמיים וקובעת את עיצוב השורה בהתאם למצב הבחירה
    public void onBindViewHolder(@NonNull final SlotViewHolder holder, int position) {
        // שליפת מחרוזת השעה הנוכחית מתוך הרשימה על פי המיקום (Position) שלה ברצף
        final String time = timeSlots.get(position);
        // הצבת מחרוזת השעה בתוך רכיב ה-TextView
        holder.tvTime.setText(time);

        // --- מנגנון ויזואלי דינמי: קביעת צבע הרכיב בהתאם למצב הבחירה של המשתמש ---
        if (selectedPosition == position) {
            // במידה והמשבצת הנוכחית היא זו שנבחרה על ידי הלקוח: נצבע את הרקע בסגול והטקסט בלבן
            holder.cardView.setCardBackgroundColor(Color.parseColor("#6200EE"));
            holder.tvTime.setTextColor(Color.WHITE);
        } else {
            // במידה וזו משבצת רגילה שאינה נבחרה: נחזיר את הרקע לצבע לבן והטקסט לצבע שחור
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvTime.setTextColor(Color.BLACK);
        }

        // --- הפיכת קוד הלמדא למבנה אנונימי מפורט וקלאסי עבור אירוע הלחיצה על פריט ברשימה ---
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // עדכון משתנה המחלקה הגלובלי למיקום החדש שהלקוח לחץ עליו כעת
                selectedPosition = holder.getAdapterPosition();
                // רענון מיידי של כל הרשימה כדי להפעיל מחדש את ה-onBindViewHolder ולעדכן את צבעי הרקע
                notifyDataSetChanged();
                // הפעלת פונקציית הממשק והעברת השעה שנבחרה ישירות אל האקטיביטי
                listener.onSlotClick(time);
            }
        });
    }

    @Override
    // פונקציה המחזירה למערכת את כמות משבצות הזמן הקיימות ברשימה הנוכחית
    public int getItemCount() {
        return timeSlots.size();
    }

    // תת-מחלקה פנימית וסטטית המייצגת את מחזיק הרכיבים הגרפיים (ViewHolder) של משבצת זמן בודדת
    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        CardView cardView;

        // פעולה בונה המקשרת בין משתני הג'אווה לרכיבי ה-XML של משבצת הזמן
        public SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTimeSlot);
            cardView = itemView.findViewById(R.id.cardSlot);
        }
    }
}