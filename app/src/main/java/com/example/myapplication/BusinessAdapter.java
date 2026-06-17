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

public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder> {

    private Context context;
    private List<BusinessModel> businessList;

    /**
     * מה הפעולה עושה: פעולה בונה (Constructor) המאותחלת עם ה-Context של המסך ורשימת בתי העסק.
     * קלט: Context context (הקשר האקטיביטי), List<BusinessModel> businessList (רשימת נתוני העסקים).
     * פלט: אין.
     */
    public BusinessAdapter(Context context, List<BusinessModel> businessList) {
        this.context = context;
        this.businessList = businessList;
    }

    /**
     * מה הפעולה עושה: מאפשרת להחליף מחוץ למחלקה את רשימת העסקים הישנה ברשימה מעודכנת.
     * קלט: List<BusinessModel> list (רשימת בתי העסק החדשה/המסוננת).
     * פלט: אין (void).
     */
    public void setBusinesses(List<BusinessModel> list) {
        this.businessList = list;
    }

    /**
     * מה הפעולה עושה: מייצרת אובייקט ViewHolder חדש ומנפחת עבורו את קובץ ה-XML של העיצוב לכל כרטיסייה ברשימה.
     * קלט: ViewGroup parent (הרכיב שמכיל את הרשימה), int viewType (סוג התצוגה).
     * פלט: BusinessViewHolder (מחזיק הרכיבים של הכרטיסייה המנופחת).
     */
    @NonNull
    @Override
    public BusinessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // שימוש ב-LayoutInflater כדי לקחת קובץ עיצוב XML סטטי ולהפוך אותו לאובייקט View דינמי בזיכרון
        View view = LayoutInflater.from(context).inflate(R.layout.item_business_card, parent, false);
        return new BusinessViewHolder(view);
    }

    /**
     * מה הפעולה עושה: יוצקת (מקשרת) את הנתונים של עסק ספציפי מתוך הרשימה אל תוך רכיבי ה-UI של ה-ViewHolder לפי מיקומו.
     * קלט: BusinessViewHolder holder (מחזיק הרכיבים של הכרטיסייה הנוכחית), int position (האינדקס של הפריט ברשימה).
     * פלט: אין (void).
     */
    @Override
    public void onBindViewHolder(@NonNull BusinessViewHolder holder, int position) {
        // שליפת אובייקט הנתונים של בית העסק שנמצא במיקום הנוכחי שרוצים להציג
        BusinessModel business = businessList.get(position);

        // הזרקת הנתונים הבסיסיים (טקסטים) מרכיב המודל אל רכיבי התצוגה הגרפיים
        holder.tvBusinessName.setText(business.getName());
        holder.tvBusinessType.setText(business.getBusinessType());
        holder.tvBusinessDescription.setText(business.getDescription());

        // בדיקה לוגית של מצב הדירוגים: אם לעסק יש דירוגים, נציג ציון ממוצע.
        // אם אין (totalReviews הוא 0), נציג חיווי מיוחד של "חדש!" כדי למנוע הצגה של דירוג 0 לא מוצדק.
        float rating = business.getOverallRating();
        int totalReviews = business.getTotalReviews();

        if (totalReviews > 0) {
            // String.format מעגל את הציון (rating) לספרה אחת בלבד אחרי הנקודה העשרונית (%.1f)
            holder.tvBusinessRating.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d)", rating, totalReviews));
        } else {
            holder.tvBusinessRating.setText("⭐ חדש!");
        }

        // לוגיקה לעיבוד תמונה: הנתונים ב-Firestore שמורים כ-Blobs (מערך בייטים גולמי).
        // המערכת צריכה לקחת את הבייטים האלה ולהפוך אותם בחזרה לאובייקט Bitmap שאנדרואיד מסוגל להציג בתוך ImageView.
        if (business.getImageBlobs() != null && !business.getImageBlobs().isEmpty()) {
            byte[] bytes = business.getImageBlobs().get(0).toBytes();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            holder.imgBusiness.setImageBitmap(bitmap);
        }

        // הגדרת מאזין לחיצה על כל שטח הכרטיסייה הנוכחית (itemView מייצג את ה-View הראשי של השורה).
        // בעת לחיצה, המערכת תפתח את מסך פרטי העסק ותעביר אליו את ה-ID שלו כדי שהמסך הבא ידע מה לשלוף.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, BusinessDetailsActivity.class);
                intent.putExtra("BUSINESS_ID", business.getBusinessId());
                context.startActivity(intent);
            }
        });
    }

    /**
     * מה הפעולה עושה: מחזירה את מספר הפריטים הכולל הקיים ברשימת העסקים, כדי שרכיב הרשימה ידע כמה כרטיסיות לייצר.
     * קלט: אין.
     * פלט: int (גודל הרשימה).
     */
    @Override
    public int getItemCount() {
        return businessList.size();
    }

    // --- תת-מחלקה פנימית: מחזיק הרכיבים של הרשימה (ViewHolder) ---
    // תפקידה למנוע קריאות חוזרות ונשנות לפקודת findViewById היקרה במשאבים, על ידי שמירת הקישורים לרכיבים פעם אחת בזיכרון.
    public static class BusinessViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusinessName, tvBusinessType, tvBusinessDescription, tvBusinessRating;
        ImageView imgBusiness;

        public BusinessViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
            tvBusinessType = itemView.findViewById(R.id.tvBusinessType);
            tvBusinessDescription = itemView.findViewById(R.id.tvBusinessDescription);
            tvBusinessRating = itemView.findViewById(R.id.tvBusinessRating);
            imgBusiness = itemView.findViewById(R.id.imgBusiness);
        }
    }
}