package com.grameenlight.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "GrameenLightSession";
    private static final String KEY_PANCHAYAT_ID = "panchayat_id";
    private static final String KEY_PANCHAYAT_NAME = "panchayat_name";
    private static final String KEY_VILLAGE_ID = "village_id";
    private static final String KEY_IS_AUTHENTICATED = "is_authenticated";
    private static final String KEY_USER_TYPE = "user_type";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setAuthenticated(String panchayatId, String panchayatName, String villageId, String userType) {
        prefs.edit()
                .putBoolean(KEY_IS_AUTHENTICATED, true)
                .putString(KEY_PANCHAYAT_ID, panchayatId)
                .putString(KEY_PANCHAYAT_NAME, panchayatName)
                .putString(KEY_VILLAGE_ID, villageId)
                .putString(KEY_USER_TYPE, userType)
                .apply();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public boolean isAuthenticated() {
        return prefs.getBoolean(KEY_IS_AUTHENTICATED, false);
    }

    public String getPanchayatId() {
        return prefs.getString(KEY_PANCHAYAT_ID, "");
    }

    public String getPanchayatName() {
        return prefs.getString(KEY_PANCHAYAT_NAME, "");
    }

    public String getVillageId() {
        return prefs.getString(KEY_VILLAGE_ID, "demo-village");
    }

    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, "citizen");
    }

    public boolean isPanchayat() {
        return "panchayat".equals(getUserType());
    }
}
