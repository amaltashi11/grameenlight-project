package com.grameenlight.app;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

final class FirebaseReporter {
    interface Callback {
        void onResult(boolean success);
    }

    private final DatabaseReference root;

    FirebaseReporter() {
        DatabaseReference databaseRoot = null;
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(
                    "https://grameen-light-0-default-rtdb.asia-southeast1.firebasedatabase.app");
            try {
                database.setPersistenceEnabled(true);
            } catch (RuntimeException ignored) {
            }
            databaseRoot = database.getReference("villages").child("demo-village");
        } catch (IllegalStateException ignored) {
        }
        root = databaseRoot;
    }

    boolean isConfigured() {
        return root != null;
    }

    void upload(PoleEntity pole, ReportEntity report, Callback callback) {
        if (root == null) {
            callback.onResult(false);
            return;
        }

        Map<String, Object> poleMap = new HashMap<>();
        poleMap.put("id", pole.id);
        poleMap.put("lane", pole.lane);
        poleMap.put("status", pole.status);
        poleMap.put("tracker", pole.tracker);
        poleMap.put("complaintId", pole.complaintId);
        poleMap.put("updatedAt", pole.updatedAt);
        poleMap.put("x", pole.x);
        poleMap.put("y", pole.y);

        Map<String, Object> reportMap = new HashMap<>();
        reportMap.put("complaintId", report.complaintId);
        reportMap.put("poleId", report.poleId);
        reportMap.put("lane", pole.lane);
        reportMap.put("status", report.status);
        reportMap.put("tracker", report.tracker);
        reportMap.put("createdAt", report.createdAt);
        reportMap.put("source", "android-citizen-report");

        Map<String, Object> update = new HashMap<>();
        update.put("poles/" + pole.id, poleMap);
        update.put("complaints/" + report.complaintId, reportMap);

        root.updateChildren(update)
                .addOnSuccessListener(unused -> callback.onResult(true))
                .addOnFailureListener(error -> callback.onResult(false));
    }

    void updateTracker(String complaintId, int poleId, String poleStatus, String tracker, long updatedAt, Callback callback) {
        if (root == null) {
            callback.onResult(false);
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("complaints/" + complaintId + "/tracker", tracker);
        update.put("complaints/" + complaintId + "/adminUpdatedAt", updatedAt);
        update.put("poles/" + poleId + "/status", poleStatus);
        update.put("poles/" + poleId + "/tracker", tracker);
        update.put("poles/" + poleId + "/updatedAt", updatedAt);

        root.updateChildren(update)
                .addOnSuccessListener(unused -> callback.onResult(true))
                .addOnFailureListener(error -> callback.onResult(false));
    }
}
