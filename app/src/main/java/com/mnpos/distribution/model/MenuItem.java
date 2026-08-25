package com.mnpos.distribution.model;

public class MenuItem {
    public final String label;
    public final String specKey;      // key into Catalog.RECORDS, or null for special screens
    public final String activityTag;  // "stock_transfer" for the dedicated screen, else null
    public final int minTier;         // Session.TIER_*
    public final String[] anyPermission; // visible if user has ANY of these (empty = tier check only)

    public MenuItem(String label, String specKey, String activityTag, int minTier, String... anyPermission) {
        this.label = label;
        this.specKey = specKey;
        this.activityTag = activityTag;
        this.minTier = minTier;
        this.anyPermission = anyPermission;
    }
}
