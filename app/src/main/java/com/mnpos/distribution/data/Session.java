package com.mnpos.distribution.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Single in-memory + persisted source of truth for the logged-in user.
 * Replaces the old static-field ApiConfig with something that can also
 * classify the user into a role tier so the UI can hide screens that
 * don't apply to them (sales rep vs branch manager vs admin).
 */
public final class Session {

    public static final int TIER_SALES_REP = 1;
    public static final int TIER_MANAGER = 2;
    public static final int TIER_ADMIN = 3;

    private static final String PREFS = "mnpos_distribution_session";

    private static Session instance;

    public String baseUrl = "";
    public String authToken = "";
    public String businessName = "";
    public String userName = "";
    public String userMobile = "";
    public String roleName = "";
    public int locationId = 0;
    public String locationName = "";
    public final Set<String> permissions = new HashSet<>();

    private Session() {}

    public static synchronized Session get() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public boolean isLoggedIn() {
        return !authToken.isEmpty() && !baseUrl.isEmpty();
    }

    public boolean hasPermission(String permission) {
        if (permissions.contains("*") || permissions.contains(permission)) return true;
        if (permission != null && permission.startsWith("distribution.")) {
            return permissions.contains("sell.create")
                || permissions.contains("stock_report.view")
                || permissions.contains("purchase_n_sell_report.view")
                || permissions.contains("access_all_locations");
        }
        return false;
    }

    public boolean hasAnyPermission(String... perms) {
        if (perms == null || perms.length == 0) return true;
        for (String p : perms) if (hasPermission(p)) return true;
        return false;
    }

    /** Coarse role tier used to decide which menu sections a user sees. */
    public int roleTier() {
        String role = roleName == null ? "" : roleName.toLowerCase();
        if (role.contains("admin") || permissions.contains("user.view") || permissions.contains("roles.view")) {
            return TIER_ADMIN;
        }
        if (role.contains("manager") || permissions.contains("access_all_locations")) {
            return TIER_MANAGER;
        }
        return TIER_SALES_REP;
    }

    public void save(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putString("baseUrl", baseUrl);
        editor.putString("authToken", authToken);
        editor.putString("businessName", businessName);
        editor.putString("userName", userName);
        editor.putString("userMobile", userMobile);
        editor.putString("roleName", roleName);
        editor.putInt("locationId", locationId);
        editor.putString("locationName", locationName);
        editor.putString("permissions", String.join(";", permissions));
        editor.apply();
    }

    public void load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        baseUrl = prefs.getString("baseUrl", "");
        authToken = prefs.getString("authToken", "");
        businessName = prefs.getString("businessName", "");
        userName = prefs.getString("userName", "");
        userMobile = prefs.getString("userMobile", "");
        roleName = prefs.getString("roleName", "");
        locationId = prefs.getInt("locationId", 0);
        locationName = prefs.getString("locationName", "");
        permissions.clear();
        String saved = prefs.getString("permissions", "");
        if (!saved.isEmpty()) {
            for (String p : saved.split(";")) if (!p.trim().isEmpty()) permissions.add(p.trim());
        }
    }

    public void clear(Context context) {
        authToken = "";
        permissions.clear();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public String url(String path) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalized = path.startsWith("/") ? path : "/" + path;
        return base + normalized;
    }
}
