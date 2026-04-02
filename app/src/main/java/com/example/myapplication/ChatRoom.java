package com.example.myapplication;

import com.google.firebase.Timestamp;

public class ChatRoom {

    private String id; // מזהה החדר (למשל שילוב של ID לקוח ו-ID עסק)
    private String clientId;
    private String businessId;
    private String lastMessage; // ההודעה האחרונה שנשלחה (לתצוגה מקדימה)
    private Timestamp lastUpdated; // מתי נשלחה ההודעה האחרונה

    // בנאי ריק - חובה עבור Firebase
    public ChatRoom() {
    }

    public ChatRoom(String id, String clientId, String businessId, String lastMessage, Timestamp lastUpdated) {
        this.id = id;
        this.clientId = clientId;
        this.businessId = businessId;
        this.lastMessage = lastMessage;
        this.lastUpdated = lastUpdated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}