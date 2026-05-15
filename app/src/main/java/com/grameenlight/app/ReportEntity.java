package com.grameenlight.app;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reports")
public class ReportEntity {
    @PrimaryKey
    @NonNull
    public String complaintId = "";  // unique per report — used as PK for upsert/sync
    public int poleId;
    public String status;
    public String tracker;
    public long createdAt;
}
