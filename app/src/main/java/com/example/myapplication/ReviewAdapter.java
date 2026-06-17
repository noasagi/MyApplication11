package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// מחלקת אדפטר (Adapter) מותאמת אישית המקשרת ומציגה את רשימת הביקורות בתוך רכיב ה-RecyclerView במסך פרטי העסק
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    // רשימה דינמית המכילה את כל אובייקטי הביקורות (`ReviewModel`) שנשלפו עבור בית העסק הנוכחי
    private List<ReviewModel> reviewsList;

    // פעולה בונה (Constructor) המקבלת את רשימת הביקורות מתוך האקטיביטי ומאתחלת את המתאם
    public ReviewAdapter(List<ReviewModel> reviewsList) {
        this.reviewsList = reviewsList;
    }

    /**
     * מה הפעולה עושה: מייצרת ומנפחת (Inflate) את תצוגת ה-XML עבור שורת ביקורת בודדת, ועוטפת אותה ב-ViewHolder חדש. הפעולה רצה רק עבור השורות הראשונות שנכנסות למסך.
     * קלט: ViewGroup parent, int viewType.
     * פלט: ReviewViewHolder (מחזיק הרכיבים של השורה).
     */
    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינה ואינפלציה של קובץ ה-XML המעצב פריט ביקורת בודד (`item_review`)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    /**
     * מה הפעולה עושה: פונקציית הליבה המזריקה ומחברת את הנתונים הגולמיים מתוך אובייקט ה-Model אל רכיבי הממשק הגרפיים בכל פעם ששורה ממוחזרת או נחשפת על המסך.
     * קלט: ReviewViewHolder holder, int position (מיקום השורה הנוכחית ברשימה).
     * פלט: אין (void).
     */
    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        // שליפת אובייקט הביקורת הספציפי מתוך הרשימה על פי מיקומו ברצף ה-RecyclerView
        ReviewModel review = reviewsList.get(position);

        holder.tvName.setText(review.getUserName());
        holder.tvComment.setText(review.getComment());

        // --- לוגיקת עיבוד והצגת הציון הממוצע של הביקורת הנוכחית ---
        float finalRating;

        // בדיקה מבנית (פולימורפיזם של נתונים): האם קיימים נתוני דירוג מפורטים עבור שלוש הקטגוריות בביקורת זו?
        if (review.getRatingProfessionalism() > 0 || review.getRatingReliability() > 0 || review.getRatingPrice() > 0) {
            // במידה וכן (ביקורת חדשה ומפורטת) - הפעלת פונקציה פנימית במודל המחשבת את ממוצע שלושת המדדים
            finalRating = review.calculateAverage();
        } else {
            // במידה ולא (תמיכה בביקורות ישנות/היסטוריות) - שימוש במדד הקיים כשדה ברירת מחדל
            finalRating = review.getRatingProfessionalism();
        }

        // הזנת הציון הסופי שחושב לתוך רכיב כוכבי הדירוג הגרפי (RatingBar) של השורה
        holder.rbRating.setRating(finalRating);

        // --- טיפול והמרה של חותמת הזמן (הגנה מפני קריסות במידה והתאריך ריק בענן) ---
        if (review.getTimestamp() != null) {
            // הגדרת תבנית תצוגה אירופאית לתאריך (יום/חודש/שנה) בהתאם לשעון המקומי של המכשיר (Locale.getDefault)
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            // המרת ה-Timestamp של פיירבייס לאובייקט Date של ג'אווה והצגתו ב-TextView
            holder.tvDate.setText(sdf.format(review.getTimestamp().toDate()));
        } else {
            // במידה ואין חותמת זמן תקינה - ניקוי שדה הטקסט של התאריך למניעת הצגת נתוני סרק (Null Guard)
            holder.tvDate.setText("");
        }
    }

    /**
     * מה הפעולה עושה: מחזירה למערכת את כמות הפריטים הכוללת הקיימת ברשימה, ובכך קובעת מתי ה-RecyclerView יפסיק לגלול.
     * קלט: אין.
     * פלט: int (גודל הרשימה).
     */
    @Override
    public int getItemCount() {
        return reviewsList.size();
    }

    // תת-מחלקה פנימית וסטטית המייצגת את מחזיק הרכיבים הגרפיים (ViewHolder).
    // נועדה למנוע קריאות חוזרות ונשנות לפקודת findViewById היקרה ביצועים, ובכך מאפשרת מיחזור יעיל וחלק של השורות בגלילה.
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvComment;
        RatingBar rbRating;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            // ביצוע שידוך וקישור חד-פעמי בין משתני הג'אווה לרכיבי ה-XML בפועל על פי המזהים שלהם
            tvName = itemView.findViewById(R.id.tvReviewerName);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            rbRating = itemView.findViewById(R.id.rbReviewRating);
        }
    }
}