package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

// מחלקת אדפטר (Adapter) המקשרת בין רשימת הנתונים של בתי העסק לבין רכיב התצוגה הממוחזרת (RecyclerView)
public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder> {

    // משתנה המחזיק את הקשר המסך (Context) לצורך ניפוח XML ופתיחת מסכים חדשים
    private Context context;
    // רשימה דינמית המכילה את אובייקטי המודל של בתי העסק השונים
    private List<BusinessModel> businessList;

    // פעולה בונה (Constructor) המקבלת את הקשר האקטיביטי ואת רשימת בתי העסק המקורית
    public BusinessAdapter(Context context, List<BusinessModel> businessList) {
        this.context = context;
        this.businessList = businessList;
    }

    // פונקציה המאפשרת לעדכן ולהחליף את רשימת בתי העסק ברשימה חדשה (למשל לאחר סינון או חיפוש)
    public void setBusinesses(List<BusinessModel> list) {
        this.businessList = list;
    }

    @NonNull
    @Override
    // פונקציה המופעלת על ידי המערכת כדי לייצר מבנה גרפי חדש (ViewHolder) עבור פריט ברשימה
    public BusinessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינה ואינפלציה (Inflation) של קובץ ה-XML המייצג כרטיסיית עסק בודדת
        View view = LayoutInflater.from(context).inflate(R.layout.item_business_card, parent, false);
        // החזרת מופע ViewHolder חדש המקושר לעיצוב שנטען
        return new BusinessViewHolder(view);
    }

    @Override
    // פונקציה המופעלת על ידי המערכת כדי לצקת נתונים מתוך אובייקט במיקום ספציפי אל תוך רכיבי הממשק
    public void onBindViewHolder(@NonNull BusinessViewHolder holder, int position) {
        // שליפת אובייקט בית העסק הנוכחי מתוך הרשימה על פי המיקום (Position)
        BusinessModel business = businessList.get(position);

        // השמת שם בית העסק בתוך רכיב הטקסט התואם
        holder.tvBusinessName.setText(business.getName());
        // השמת סוג או קטגוריית העסק בתוך רכיב הטקסט התואם
        holder.tvBusinessType.setText(business.getBusinessType());
        // השמת הפירוט או התיאור של העסק בתוך רכיב הטקסט התואם
        holder.tvBusinessDescription.setText(business.getDescription());

        // --- עדכון מדדי הדירוג והביקורות בממשק ---
        // שליפת הציון המשוקלל של העסק מתוך האובייקט
        float rating = business.getOverallRating();
        // שליפת כמות המדרגים הכוללת שנתנו ביקורת לעסק
        int totalReviews = business.getTotalReviews();

        // תנאי: אם יש לפחות ביקורת אחת, נציג את הציון המשוקלל לצד כמות המדרגים בסוגריים
        if (totalReviews > 0) {
            // עיצוב מחרוזת טקסט מוגדרת (למשל: ⭐ 4.8 (12)) עם דיוק של ספרה אחת לאחר הנקודה
            holder.tvBusinessRating.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d)", rating, totalReviews));
        } else {
            // במידה ואין עדיין דירוגים, נציג חיווי גרפי המציין שמדובר בעסק חדש במערכת
            holder.tvBusinessRating.setText("⭐ חדש!");
        }

        // --- טעינה ופיענוח של תמונת בית העסק ---
        // תנאי בטיחות: מוודאים שקיימות תמונות שמורות בפורמט Blobs עבור בית העסק
        if (business.getImageBlobs() != null && !business.getImageBlobs().isEmpty()) {
            // שליפת מערך הבייטים (byte[]) מתוך אובייקט ה-Blob הראשון ברשימה
            byte[] bytes = business.getImageBlobs().get(0).toBytes();
            // המרת מערך הבייטים לאובייקט תמונה של אנדרואיד מסוג Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            // הצבת ה-Bitmap בתוך רכיב ה-ImageView בכרטיסייה
            holder.imgBusiness.setImageBitmap(bitmap);
        }

        // --- הגדרת האזנה ללחיצה על הכרטיסייה כולה (מעבר למסך פרטי העסק) ---
        // שימוש במופע אנונימי קלאסי של View.OnClickListener במקום למדא
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת כוונת (Intent) למעבר מהמסך הנוכחי אל מסך פירוט העסק המורחב
                Intent intent = new Intent(context, BusinessDetailsActivity.class);
                // העברת מזהה העסק הייחודי כפרמטר (Extra) כדי שהמסך הבא ידע איזה מידע לשלוף
                intent.putExtra("BUSINESS_ID", business.getBusinessId());
                // התחלת האקטיביטי ופתיחת המסך החדש בפועל
                context.startActivity(intent);
            }
        });
    }

    @Override
    // פונקציה המחזירה למערכת את כמות הפריטים הכוללת הקיימת ברשימת בתי העסק
    public int getItemCount() {
        return businessList.size();
    }

    // תת-מחלקה פנימית וסטטית המייצגת את מחזיק הרכיבים (ViewHolder) של הכרטיסייה
    public static class BusinessViewHolder extends RecyclerView.ViewHolder {
        // הצהרה על רכיבי הטקסט והתמונה המרכיבים את עיצוב ה-XML של הכרטיסייה
        TextView tvBusinessName, tvBusinessType, tvBusinessDescription, tvBusinessRating;
        ImageView imgBusiness;

        // פעולה בונה המקבלת את תצוגת הכרטיסייה ומקשרת בין המשתנים לרכיבי ה-XML בפועל
        public BusinessViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור משתנה שם העסק לרכיב ה-XML המתאים על פי מזהה
            tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
            // קישור משתנה סוג העסק לרכיב ה-XML המתאים על פי מזהה
            tvBusinessType = itemView.findViewById(R.id.tvBusinessType);
            // קישור משתנה תיאור העסק לרכיב ה-XML המתאים על פי מזהה
            tvBusinessDescription = itemView.findViewById(R.id.tvBusinessDescription);
            // קישור משתנה הדירוג והכוכב לרכיב ה-XML המתאים על פי מזהה
            tvBusinessRating = itemView.findViewById(R.id.tvBusinessRating);
            // קישור משתנה תמונת העסק לרכיב ה-XML המתאים על פי מזהה
            imgBusiness = itemView.findViewById(R.id.imgBusiness);
        }
    }
}