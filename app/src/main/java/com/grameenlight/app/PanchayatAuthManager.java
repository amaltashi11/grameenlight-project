package com.grameenlight.app;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class PanchayatAuthManager {
    interface AuthCallback {
        void onSuccess(String panchayatId, String panchayatName);
        void onFailure(String error);
    }

    interface RegisterCallback {
        void onSuccess();
        void onFailure(String error);
    }

    private final DatabaseReference panchayatsRef;

    public PanchayatAuthManager() {
        DatabaseReference ref = null;
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(
                    "https://grameen-light-0-default-rtdb.asia-southeast1.firebasedatabase.app");
            ref = database.getReference("panchayats");
        } catch (IllegalStateException ignored) {
        }
        this.panchayatsRef = ref;
    }

    public boolean isConfigured() {
        return panchayatsRef != null;
    }

    public void authenticate(String panchayatId, String password, AuthCallback callback) {
        if (panchayatsRef == null) {
            callback.onFailure("Firebase not configured");
            return;
        }

        panchayatsRef.child(panchayatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onFailure("Panchayat ID not found");
                    return;
                }

                String storedPasswordHash = snapshot.child("passwordHash").getValue(String.class);
                String panchayatName = snapshot.child("name").getValue(String.class);

                if (storedPasswordHash == null || panchayatName == null) {
                    callback.onFailure("Invalid panchayat data");
                    return;
                }

                if (hashPassword(password).equals(storedPasswordHash)) {
                    callback.onSuccess(panchayatId, panchayatName);
                } else {
                    callback.onFailure("Invalid password");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onFailure("Database error: " + error.getMessage());
            }
        });
    }

    public void registerPanchayat(String panchayatId, String panchayatName, String password, String villageId, RegisterCallback callback) {
        if (panchayatsRef == null) {
            callback.onFailure("Firebase not configured");
            return;
        }

        String passwordHash = hashPassword(password);
        Map<String, Object> panchayatData = new HashMap<>();
        panchayatData.put("id", panchayatId);
        panchayatData.put("name", panchayatName);
        panchayatData.put("villageId", villageId);
        panchayatData.put("passwordHash", passwordHash);
        panchayatData.put("createdAt", System.currentTimeMillis());

        panchayatsRef.child(panchayatId).setValue(panchayatData)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onFailure(error.getMessage()));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
