package com.grameenlight.app;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {PoleEntity.class, ReportEntity.class}, version = 2, exportSchema = false)
public abstract class GrameenDb extends RoomDatabase {
    public abstract PoleDao poleDao();

    /**
     * Migrates from v1 → v2:
     * Re-creates the reports table with complaintId as the STRING primary key
     * (replacing the old auto-generated integer id).
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS reports");
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `reports` (" +
                "`complaintId` TEXT NOT NULL, " +
                "`poleId` INTEGER NOT NULL, " +
                "`status` TEXT, " +
                "`tracker` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`complaintId`))"
            );
        }
    };
}
