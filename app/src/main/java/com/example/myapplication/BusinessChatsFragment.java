package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BusinessChatsFragment extends Fragment {

    private RecyclerView rvBusinessChats;
    private BusinessChatsAdapter adapter;
    private List<ChatRoomModel> chatRoomsList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // מנפחים את ה-XML של הפרגמנט
        View view = inflater.inflate(R.layout.fragment_business_chats, container, false);

        rvBusinessChats = view.findViewById(R.id.rvBusinessChats);
        rvBusinessChats.setLayoutManager(new LinearLayoutManager(getContext()));

        chatRoomsList = new ArrayList<>();
        adapter = new BusinessChatsAdapter(chatRoomsList);
        rvBusinessChats.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadChats();

        return view;
    }

    private void loadChats() {
        if (auth.getCurrentUser() == null) return;

        // ה-ID של בעל העסק המחובר
        String currentBusinessId = auth.getCurrentUser().getUid();
        // מאזינים לכל חדרי הצ'אט שבהם ה-businessId שווה ל-ID של בעל העסק
        db.collection("ChatRooms")
                .whereEqualTo("businessId", currentBusinessId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return; // התעלמות משגיאות (אפשר להוסיף Toast במקרה הצורך)
                    }

                    chatRoomsList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        ChatRoomModel room = doc.toObject(ChatRoomModel.class);
                        chatRoomsList.add(room);
                    }

                    // בגלל שפיירסטור לפעמים עושה בעיות עם OrderBy בלי אינדקס,
                    // נסדר את הרשימה בזכרון המקומי לפי התאריך
                    chatRoomsList.sort((r1, r2) -> {
                        if (r1.getLastUpdate() == null || r2.getLastUpdate() == null) return 0;
                        return r2.getLastUpdate().compareTo(r1.getLastUpdate()); // מהחדש לישן
                    });

                    adapter.notifyDataSetChanged();
                });
    }
}