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

    public BusinessAdapter(Context context, List<BusinessModel> businessList) {
        this.context = context;
        this.businessList = businessList;
    }

    public void setBusinesses(List<BusinessModel> list) {
        this.businessList = list;
    }

    @NonNull
    @Override
    public BusinessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // כאן אנחנו מחברים את ה-XML של הכרטיסייה (item_business)
        View view = LayoutInflater.from(context).inflate(R.layout.item_business_card, parent, false);
        return new BusinessViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusinessViewHolder holder, int position) {
        BusinessModel business = businessList.get(position);

        // עדכון טקסטים בסיסיים
        holder.tvBusinessName.setText(business.getName());
        holder.tvBusinessType.setText(business.getBusinessType());
        holder.tvBusinessDescription.setText(business.getDescription());

        // --- עדכון הדירוג בזמן אמת ---
        float rating = business.getOverallRating();
        int totalReviews = business.getTotalReviews();

        if (totalReviews > 0) {
            // מעדכן לטקסט כמו: ⭐ 4.8 (12)
            holder.tvBusinessRating.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d)", rating, totalReviews));
        } else {
            holder.tvBusinessRating.setText("⭐ חדש!");
        }

        // טעינת תמונה (מה-Blob הראשון ברשימה)
        if (business.getImageBlobs() != null && !business.getImageBlobs().isEmpty()) {
            byte[] bytes = business.getImageBlobs().get(0).toBytes();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            holder.imgBusiness.setImageBitmap(bitmap);
        }

        // לחיצה על הכרטיס למעבר למסך פרטים
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BusinessDetailsActivity.class);
            intent.putExtra("BUSINESS_ID", business.getBusinessId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return businessList.size();
    }

    // ה-ViewHolder שמחזיק את הקישורים לרכיבי ה-XML
    public static class BusinessViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusinessName, tvBusinessType, tvBusinessDescription, tvBusinessRating;
        ImageView imgBusiness;

        public BusinessViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
            tvBusinessType = itemView.findViewById(R.id.tvBusinessType);
            tvBusinessDescription = itemView.findViewById(R.id.tvBusinessDescription);
            // זה ה-ID מה-XML ששלחת לי עכשיו
            tvBusinessRating = itemView.findViewById(R.id.tvBusinessRating);
            imgBusiness = itemView.findViewById(R.id.imgBusiness);
        }
    }
}