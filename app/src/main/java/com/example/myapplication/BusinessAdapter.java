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

import com.google.firebase.firestore.Blob;

import java.util.List;

public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder> {

    private final Context context;
    private List<BusinessModel> businessesList;

    public BusinessAdapter(Context context, List<BusinessModel> businessesList) {
        this.context = context;
        this.businessesList = businessesList;
    }

    public void setBusinesses(List<BusinessModel> newBusinessesList) {
        this.businessesList = newBusinessesList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BusinessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_business_card, parent, false);
        return new BusinessViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusinessViewHolder holder, int position) {
        if (businessesList == null || businessesList.isEmpty()) return;

        BusinessModel currentBusiness = businessesList.get(position);

        // הצגת טקסטים
        holder.tvBusinessName.setText(currentBusiness.getName() != null ? currentBusiness.getName() : "");
        holder.tvBusinessType.setText("סוג: " + (currentBusiness.getBusinessType() != null ? currentBusiness.getBusinessType() : ""));
        holder.tvBusinessDescription.setText(currentBusiness.getDescription() != null ? currentBusiness.getDescription() : "");

        // הצגת תמונה ראשית בלבד בכרטיס
        boolean imageSet = false;
        if (currentBusiness.getImageBlobs() != null && !currentBusiness.getImageBlobs().isEmpty()) {
            Blob firstBlob = currentBusiness.getImageBlobs().get(0);
            if (firstBlob != null) {
                byte[] bytes = firstBlob.toBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    holder.imgBusiness.setImageBitmap(bitmap);
                    imageSet = true;
                }
            }
        }

        if (!imageSet) {
            holder.imgBusiness.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // --- כאן השינוי: לחיצה על הכרטיס מעבירה לדף העסק המלא ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BusinessDetailsActivity.class);
            intent.putExtra("BUSINESS_ID", currentBusiness.getBusinessId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return (businessesList == null) ? 0 : businessesList.size();
    }

    public static class BusinessViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBusiness;
        TextView tvBusinessName;
        TextView tvBusinessType;
        TextView tvBusinessDescription;

        public BusinessViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBusiness = itemView.findViewById(R.id.imgBusiness);
            tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
            tvBusinessType = itemView.findViewById(R.id.tvBusinessType);
            tvBusinessDescription = itemView.findViewById(R.id.tvBusinessDescription);
        }
    }
}