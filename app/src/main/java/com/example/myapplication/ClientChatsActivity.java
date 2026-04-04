package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ClientChatsActivity extends BaseActivity {

    private RecyclerView rvClientChats;
    private ClientChatsAdapter adapter;
    private List<ChatRoomModel> chatRoomsList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_chats);

        rvClientChats = findViewById(R.id.rvClientChats);
        rvClientChats.setLayoutManager(new LinearLayoutManager(this));

        chatRoomsList = new ArrayList<>();
        adapter = new ClientChatsAdapter(chatRoomsList);
        rvClientChats.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadClientChats();
    }

    private void loadClientChats() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לראות הודעות", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentClientId = auth.getCurrentUser().getUid();

        // מאזינים לכל החדרים בהם ה-clientId שווה ללקוח המחובר
        db.collection("ChatRooms")
                .whereEqualTo("clientId", currentClientId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    chatRoomsList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        ChatRoomModel room = doc.toObject(ChatRoomModel.class);
                        chatRoomsList.add(room);
                    }

                    chatRoomsList.sort((r1, r2) -> {
                        if (r1.getLastUpdate() == null || r2.getLastUpdate() == null) return 0;
                        return r2.getLastUpdate().compareTo(r1.getLastUpdate());
                    });

                    adapter.notifyDataSetChanged();
                });
    }
}