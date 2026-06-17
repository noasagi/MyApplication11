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

// מחלקת מתאם (Adapter) מותאמת אישית לניהול והצגת משבצות הזמן הפנויות (Time Slots) לקביעת תור בתוך RecyclerView
public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.SlotViewHolder> {

    // רשימה דינמית המכילה את מחרוזות השעות הפנויות (לדוגמה: "10:30", "11:00")
    private List<String> timeSlots = new ArrayList<>();

    // משתנה השומר את מיקום (Position) משבצת הזמן שהמשתמש בחר כרגע. ערך 1- מציין שטרם בוצעה בחירה
    private int selectedPosition = -1;

    // אובייקט המפנה לממשק (Interface) כדי להעביר את אירוע הלחיצה והנתונים בחזרה לאקטיביטי/פרגמנט המחזיק
    private final OnSlotClickListener listener;

    /**
     * הגדרת ממשק פנימי (Interface) - משמש כצינור תקשורת להעברת נתוני השעה שנבחרה אל מחוץ לאדפטר.
     * למה זה נחוץ: אדפטר תפקידו רק להציג רשימות. הוא לא אמור לדעת מה לעשות עם השעה שנבחרה (למשל לשמור אותה ב-Firestore).
     * בעזרת ה-Interface, האדפטר "מדווח" החוצה והלוגיקה העסקית מתבצעת באקטיביטי.
     */
    public interface OnSlotClickListener {
        void onSlotClick(String time);
    }

    // פעולה בונה (Constructor) המקבלת את מאזין הלחיצות מהאקטיביטי ומאתחלת אותו
    public TimeSlotAdapter(OnSlotClickListener listener) {
        this.listener = listener;
    }

    /**
     * מה הפעולה עושה: מעדכנת דינמית את רשימת השעות (למשל, כאשר הלקוח מחליף תאריך בלוח השנה).
     * קלט: List<String> slots.
     */
    public void setTimeSlots(List<String> slots) {
        this.timeSlots = slots;
        this.selectedPosition = -1; // איפוס הבחירה חזרה ל-1- כדי שהשעה של היום הקודם לא תישאר מסומנת בטעות
        notifyDataSetChanged();     // פקודה קריטית המורה למערכת לרענן, לחשב ולצייר מחדש את כל הרשימה על המסך
    }

    @NonNull
    @Override
    /**
     * מה הפעולה עושה: מייצרת פיזית את ה-ViewHolder (מחזיק הרכיבים) ומנפחת את קובץ ה-XML הבודד של השורה.
     * מתי היא מופעלת: רק מספר פעמים מצומצם בהתחלה (לפי כמות הפריטים שנכנסים במסך של הטלפון בבת אחת).
     */
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    /**
     * מה הפעולה עושה: פונקציית הליבה שמחברת ומזריקה את הנתונים הגולמיים לרכיבים הגרפיים, וקובעת את עיצוב השורה בהתאם למצב הבחירה.
     * מתי היא מופעלת: בכל פעם שפריט נכנס לתצוגה תוך כדי גלילה (Scroll) או בעת קריאה ל-notifyDataSetChanged.
     */
    public void onBindViewHolder(@NonNull final SlotViewHolder holder, int position) {
        final String time = timeSlots.get(position);
        holder.tvTime.setText(time);

        // --- מנגנון ויזואלי דינמי: קביעת צבע הרכיב בהתאם למצב הבחירה של המשתמש ---
        if (selectedPosition == position) {
            // אם המשבצת הנוכחית היא זו שנבחרה על ידי הלקוח: נצבע את הרקע בסגול מותג והטקסט בלבן
            holder.cardView.setCardBackgroundColor(Color.parseColor("#6200EE"));
            holder.tvTime.setTextColor(Color.WHITE);
        } else {
            // אם זו משבצת רגילה שאינה נבחרה: נחזיר את הרקע לצבע לבן נקי והטקסט לצבע שחור
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvTime.setTextColor(Color.BLACK);
        }

        // הגדרת מאזין לחיצה אנונימי קלאסי ומפורט עבור הפריט ברשימה
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // עדכון משתנה המחלקה למיקום החדש שהלקוח לחץ עליו כעת
                selectedPosition = holder.getAdapterPosition();

                // רענון מיידי של הרשימה כדי להפעיל מחדש את ה-onBindViewHolder ולצבוע מחדש את הפריטים בצבעים הנכונים
                notifyDataSetChanged();

                // הפעלת פונקציית הממשק והעברת מחרוזת השעה שנבחרה ישירות אל האקטיביטי שמקשיב
                listener.onSlotClick(time);
            }
        });
    }

    @Override
    // מחזירה למערכת את גודל הרשימה (כמות משבצות הזמן הזמינות להצגה)
    public int getItemCount() {
        return timeSlots.size();
    }

    /**
     * תת-מחלקה פנימית וסטטית המייצגת את מחזיק הרכיבים הגרפיים (ViewHolder).
     * תפקידה: למנוע קריאות יקרות בביצועים לפקודת findViewById בכל גלילה של הרשימה על ידי שמירת הפניות (References) קבועות לרכיבים בזיכרון.
     */
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