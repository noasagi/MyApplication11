package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder> {

    private final Context context;
    private List<BusinessModel> businessesList;

    public BusinessAdapter(Context context, List<BusinessModel> businessesList) {
        this.context = context;
        this.businessesList = businessesList;
    }

    // פונקציה לעדכון הרשימה (בשימוש ע"י ClientMainActivity לסינון)
    public void setBusinesses(List<BusinessModel> newBusinessesList) {
        this.businessesList = newBusinessesList;
    }

    @NonNull
    @Override
    public BusinessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // מנפח את העיצוב של item_business_card.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_business_card, parent, false);
        return new BusinessViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusinessViewHolder holder, int position) {
        BusinessModel currentBusiness = businessesList.get(position);

        // עדכון הטקסטים בכרטיס
        holder.tvBusinessName.setText(currentBusiness.getName());
        holder.tvBusinessType.setText("סוג: " + currentBusiness.getBusinessType());

        // TODO: אם בעתיד תטעיני תמונות, תצטרכי להשתמש כאן בספרייה כמו Glide או Picasso
        //holder.imgBusiness.setImageResource(R.drawable.default_business_icon);

        // פתיחת פרטי העסק בלחיצה
        holder.itemView.setOnClickListener(v -> {
            // כרגע רק מדפיסים הודעה, בהמשך נעבור למסך פרטי עסק
            // Intent intent = new Intent(context, BusinessDetailsActivity.class);
            // context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return businessesList.size();
    }

    // מחלקה פנימית שמחזיקה את האלמנטים של העיצוב
    public static class BusinessViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusinessName;
        TextView tvBusinessType;
        ImageView imgBusiness;

        public BusinessViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
            tvBusinessType = itemView.findViewById(R.id.tvBusinessType);
            imgBusiness = itemView.findViewById(R.id.imgBusiness);
        }
    }
}