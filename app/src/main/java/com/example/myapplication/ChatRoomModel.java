package com.example.myapplication;

import com.google.firebase.Timestamp;

public class ChatRoomModel {
    private String chatRoomId;
    private String clientId;
    private String clientName;
    private String businessId;
    private String lastMessage;
    private Timestamp lastUpdate;

    // בנאי ריק שחובה עבור פיירבייס
    public ChatRoomModel() {}

    public ChatRoomModel(String chatRoomId, String clientId, String clientName, String businessId, String lastMessage, Timestamp lastUpdate) {
        this.chatRoomId = chatRoomId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.businessId = businessId;
        this.lastMessage = lastMessage;
        this.lastUpdate = lastUpdate;
    }

    public String getChatRoomId() { return chatRoomId; }
    public String getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getBusinessId() { return businessId; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getLastUpdate() { return lastUpdate; }
}