package com.grameenlight.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "poles")
public class PoleEntity {
    @PrimaryKey
    public int id;
    public float x;
    public float y;
    public String lane;
    public String status;
    public String tracker;
    public String complaintId;
    public long updatedAt;
}
