package com.grameenlight.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PoleDao {
    @Query("SELECT * FROM poles ORDER BY id")
    List<PoleEntity> getPoles();

    @Query("SELECT * FROM reports ORDER BY createdAt DESC LIMIT 8")
    List<ReportEntity> getRecentReports();

    @Query("SELECT * FROM poles WHERE id = :id LIMIT 1")
    PoleEntity getPoleById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertPole(PoleEntity pole);

    @Insert
    void insertReport(ReportEntity report);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertReport(ReportEntity report);

    @Query("SELECT COUNT(*) FROM poles")
    int poleCount();

    @Query("SELECT COUNT(*) FROM reports WHERE status = :status")
    int countReportsByStatus(String status);

    @Query("UPDATE reports SET tracker = :tracker WHERE complaintId = :complaintId")
    void updateReportTracker(String complaintId, String tracker);
}
