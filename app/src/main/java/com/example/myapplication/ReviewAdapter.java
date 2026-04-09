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

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<ReviewModel> reviewsList;

    public ReviewAdapter(List<ReviewModel> reviewsList) {
        this.reviewsList = reviewsList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewModel review = reviewsList.get(position);

        holder.tvName.setText(review.getUserName());
        holder.tvComment.setText(review.getComment());

        // לוגיקת הצגת הדירוג:
        float finalRating;

        // בדיקה: האם זו ביקורת חדשה עם פירוט?
        if (review.getRatingProfessionalism() > 0 || review.getRatingReliability() > 0 || review.getRatingPrice() > 0) {
            // אם כן, נחשב ממוצע של שלושתן
            finalRating = review.calculateAverage();
        } else {
            // אם לא (ביקורת ישנה), נשתמש בשדה הדירוג הישן אם קיים
            // (אם קראת לשדה הישן 'rating', ודאי שיש לו Getter במודל)
            finalRating = review.getRatingProfessionalism(); // או השדה שהיה בשימוש קודם
        }

        holder.rbRating.setRating(finalRating);

        // טיפול בתאריך (כדי למנוע קריסה אם ה-Timestamp ריק)
        if (review.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(review.getTimestamp().toDate()));
        } else {
            holder.tvDate.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return reviewsList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvComment;
        RatingBar rbRating;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvReviewerName);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            rbRating = itemView.findViewById(R.id.rbReviewRating);
        }
    }
}