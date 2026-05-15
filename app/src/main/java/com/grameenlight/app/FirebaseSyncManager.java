package com.grameenlight.app;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Listens to the Firebase Realtime Database for changes to poles and complaints
 * made by ANY device and syncs them into the local Room database.
 * This enables real-time cross-device updates.
 */
public class FirebaseSyncManager {

    /** Called on the main thread whenever Firebase data changes. */
    public interface OnSyncListener {
        void onDataChanged();
    }

    private final PoleDao dao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DatabaseReference polesRef;
    private DatabaseReference complaintsRef;
    private ValueEventListener polesListener;
    private ValueEventListener complaintsListener;
    private OnSyncListener listener;

    private boolean configured = false;

    public FirebaseSyncManager(PoleDao dao) {
        this.dao = dao;
        try {
            FirebaseDatabase db = FirebaseDatabase.getInstance(
                    "https://grameen-light-0-default-rtdb.asia-southeast1.firebasedatabase.app");
            DatabaseReference villageRef = db.getReference("villages").child("demo-village");
            polesRef      = villageRef.child("poles");
            complaintsRef = villageRef.child("complaints");
            configured = true;
        } catch (IllegalStateException ignored) {
            // Firebase not initialized — sync will be silently disabled
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    /** Attach Firebase listeners. Call from Activity.onResume(). */
    public void startSync(OnSyncListener listener) {
        if (!configured) return;
        this.listener = listener;

        polesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Run DB writes on a background thread to avoid blocking the main thread
                new Thread(() -> {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PoleEntity pole = parsePole(child);
                        if (pole != null) {
                            dao.upsertPole(pole);
                        }
                    }
                    // Notify UI on main thread
                    mainHandler.post(() -> {
                        if (FirebaseSyncManager.this.listener != null) {
                            FirebaseSyncManager.this.listener.onDataChanged();
                        }
                    });
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { /* ignored */ }
        };

        complaintsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                new Thread(() -> {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ReportEntity report = parseReport(child);
                        if (report != null) {
                            dao.upsertReport(report);
                        }
                    }
                    mainHandler.post(() -> {
                        if (FirebaseSyncManager.this.listener != null) {
                            FirebaseSyncManager.this.listener.onDataChanged();
                        }
                    });
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { /* ignored */ }
        };

        polesRef.addValueEventListener(polesListener);
        complaintsRef.addValueEventListener(complaintsListener);
    }

    /** Detach Firebase listeners. Call from Activity.onPause(). */
    public void stopSync() {
        if (!configured) return;
        if (polesListener != null) {
            polesRef.removeEventListener(polesListener);
            polesListener = null;
        }
        if (complaintsListener != null) {
            complaintsRef.removeEventListener(complaintsListener);
            complaintsListener = null;
        }
        listener = null;
    }

    // ── Parsers ──────────────────────────────────────────────────────────────

    private PoleEntity parsePole(DataSnapshot s) {
        try {
            PoleEntity p = new PoleEntity();
            Long idLong = s.child("id").getValue(Long.class);
            if (idLong == null) return null;
            p.id        = idLong.intValue();
            p.lane      = s.child("lane").getValue(String.class);
            p.status    = s.child("status").getValue(String.class);
            p.tracker   = s.child("tracker").getValue(String.class);
            p.complaintId = valueOrEmpty(s.child("complaintId").getValue(String.class));
            Long updatedAt = s.child("updatedAt").getValue(Long.class);
            p.updatedAt = updatedAt != null ? updatedAt : 0L;
            Double x = s.child("x").getValue(Double.class);
            Double y = s.child("y").getValue(Double.class);
            p.x = x != null ? x.floatValue() : 0f;
            p.y = y != null ? y.floatValue() : 0f;
            if (p.status == null || p.lane == null) return null;
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private ReportEntity parseReport(DataSnapshot s) {
        try {
            ReportEntity r = new ReportEntity();
            r.complaintId = s.child("complaintId").getValue(String.class);
            if (r.complaintId == null || r.complaintId.isEmpty()) return null;
            Long poleId = s.child("poleId").getValue(Long.class);
            if (poleId == null) return null;
            r.poleId  = poleId.intValue();
            r.status  = s.child("status").getValue(String.class);
            r.tracker = s.child("tracker").getValue(String.class);
            Long createdAt = s.child("createdAt").getValue(Long.class);
            r.createdAt = createdAt != null ? createdAt : 0L;
            if (r.status == null) return null;
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private String valueOrEmpty(String v) {
        return v != null ? v : "";
    }
}
