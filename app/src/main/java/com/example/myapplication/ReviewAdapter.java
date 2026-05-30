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

    @NonNull
    @Override
    // פונקציה המופעלת על ידי המערכת כדי לייצר מחזיק רכיבים (ViewHolder) חדש עבור שורת ביקורת בודדת
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינה ואינפלציה של קובץ ה-XML המעצב פריט ביקורת בודד (`item_review`)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        // החזרת מופע ViewHolder חדש המקושר לקובץ העיצוב שנטען
        return new ReviewViewHolder(view);
    }

    @Override
    // פונקציית הליבה המזריקה את הנתונים הגולמיים מתוך אובייקט הביקורת אל רכיבי הממשק הגרפיים בכל שורה
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        // שליפת אובייקט הביקורת הספציפי מתוך הרשימה על פי מיקומו (Position) ברצף ה-RecyclerView
        ReviewModel review = reviewsList.get(position);

        // הצבת שם הלקוח שכתב את הביקורת ברכיב ה-TextView המתאים
        holder.tvName.setText(review.getUserName());
        // הצבת תוכן חוות הדעת המילולית שכתב הלקוח בתיבת הטקסט
        holder.tvComment.setText(review.getComment());

        // --- לוגיקת עיבוד והצגת הציון הממוצע של הביקורת הנוכחית ---
        float finalRating;

        // בדיקה מבנית: האם קיימים נתוני דירוג מפורטים עבור שלוש הקטגוריות בביקורת זו?
        if (review.getRatingProfessionalism() > 0 || review.getRatingReliability() > 0 || review.getRatingPrice() > 0) {
            // במידה וכן (ביקורת חדשה ומפורטת) - המערכת מפעילה פונקציה פנימית במודל המחשבת את ממוצע שלושת המדדים
            finalRating = review.calculateAverage();
        } else {
            // במידה ולא (תמיכה בביקורות היסטוריות/ישנות במערכת) - שימוש במדד הקיים כשדה ברירת מחדל
            finalRating = review.getRatingProfessionalism();
        }

        // הזנת הציון הסופי שחושב לתוך רכיב כוכבי הדירוג הגרפי (`RatingBar`) של השורה
        holder.rbRating.setRating(finalRating);

        // --- טיפול והמרה של חותמת הזמן (הגנה מפני קריסות במידה והתאריך ריק בענן) ---
        if (review.getTimestamp() != null) {
            // הגדרת תבנית תצוגה אירופאית לתאריך (יום/חודש/שנה) בהתאם לשעון המקומי של המכשיר
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            // המרת ה-Timestamp של פיירבייס לאובייקט Date של ג'אווה, פירמוטו למחרוזת והצגתו ב-TextView
            holder.tvDate.setText(sdf.format(review.getTimestamp().toDate()));
        } else {
            // במידה ואין חותמת זמן תקינה - ניקוי שדה הטקסט של התאריך למניעת הצגת נתוני סרק
            holder.tvDate.setText("");
        }
    }

    @Override
    // פונקציה המחזירה למערכת את כמות הפריטים הכוללת הקיימת ברשימת הביקורות (קובע את גודל הרשימה על המסך)
    public int getItemCount() {
        return reviewsList.size();
    }

    // תת-מחלקה פנימית וסטטית המייצגת את מחזיק הרכיבים הגרפיים (ViewHolder) של שורת ביקורת בודדת
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        // הצהרה על רכיבי הטקסט (שם, תאריך ותוכן) ורכיב הדירוג הגרפי הנמצאים בתוך השורה
        TextView tvName, tvDate, tvComment;
        RatingBar rbRating;

        // פעולה בונה המקבלת את תצוגת השורה ומקשרת בין משתני הג'אווה לרכיבי ה-XML בפועל
        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            // ביצוע שידוך וקישור לרכיבים על פי המזהים הייחודיים שלהם (IDs) המוגדרים בקובץ ה-XML של הפריט
            tvName = itemView.findViewById(R.id.tvReviewerName);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            rbRating = itemView.findViewById(R.id.rbReviewRating);
        }
    }
}