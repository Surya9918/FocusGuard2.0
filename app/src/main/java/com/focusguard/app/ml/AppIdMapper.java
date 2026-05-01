package com.focusguard.app.ml;

import java.util.HashMap;
import java.util.Map;

/**
 * AppIdMapper — Maps package names to ML category IDs.
 */
public class AppIdMapper {
    private static final Map<String, Integer> PACKAGE_TO_ID = new HashMap<>();

    static {
        // Example mappings
        PACKAGE_TO_ID.put("com.instagram.android", 0);
        PACKAGE_TO_ID.put("com.facebook.katana", 1);
        PACKAGE_TO_ID.put("com.twitter.android", 2);
        PACKAGE_TO_ID.put("com.zhiliaoapp.musically", 3); // TikTok
        // ... more mappings
    }

    public static int categorize(String packageName) {
        Integer id = PACKAGE_TO_ID.get(packageName);
        if (id != null) return id;
        
        // Fallback categories based on package name heuristics
        if (packageName.contains("social") || packageName.contains("chat")) return 0;
        if (packageName.contains("video") || packageName.contains("player")) return 3;
        if (packageName.contains("news")) return 4;
        
        return 4; // Default: news_feeds/general
    }
}
