package com.example.myapplication;

public class BlockedSlot {
    private String blockId;
    private String date;      // "dd-MM-yyyy"
    private String startTime; // "12:00"
    private String endTime;   // "14:00"
    private String reason;    // "הפסקת צהריים"

    public BlockedSlot() {}

    public BlockedSlot(String blockId, String date, String startTime, String endTime, String reason) {
        this.blockId = blockId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    // Getters...
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
}