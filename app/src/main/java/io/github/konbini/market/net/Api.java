package io.github.konbini.market.net;

import io.github.konbini.market.util.Prefs;
import android.content.Context;

public class Api {

	public static String baseUrl(Context c) {
	    String host = Prefs.getServer(c);
	    if (host == null) host = "";
	    host = host.trim();

	    if (host.length() == 0) host = "pyt.pp.ua";

	    while (host.endsWith("/")) host = host.substring(0, host.length() - 1);

	    if (host.endsWith("/api")) host = host.substring(0, host.length() - 4);
	    while (host.endsWith("/")) host = host.substring(0, host.length() - 1);

	    if (host.startsWith("http://") || host.startsWith("https://")) {
	        return host;
	    }

	    return "http://" + host;
	}

    public static String iconUrl(Context c, String iconFile) {
        return iconFile;
    }

    public static String bannerUrl(Context c, String bannerFile) {
        return bannerFile;
    }

    public static String screenshotUrl(Context c, String file) {
        return file;
    }

    public static String avatarUrl(Context c, String avatarFile) {
        return avatarFile;
    }

    // ---- server_updated.py API ----
    public static String loginUrl(Context c) {
        return baseUrl(c) + "/api/auth/login";
    }

    public static String avatarsUrl(Context c) {
        return baseUrl(c) + "/api/user/avatars";
    }

    public static String userProfileUrl(Context c, int userId) {
        return baseUrl(c) + "/api/user/" + userId;
    }
    
    public static String appVersionsUrl(android.content.Context c, int appId) {
        return baseUrl(c) + "/api/apps/" + appId;
    }

    public static String downloadUrl(Context c, int appId, String version, int userId) {
        String url;

        if (version != null && version.length() > 0) {
            String enc = version;
            try { enc = java.net.URLEncoder.encode(version, "UTF-8"); } catch (Exception e) {}
            url = baseUrl(c) + "/api/download/" + appId + "/" + enc;
        } else {
            url = baseUrl(c) + "/api/download/" + appId;
        }

        if (userId > 0) url += "?user_id=" + userId;
        return url;
    }

    public static String appReviewsUrl(android.content.Context c, int appId, int viewerId) {
        String url = baseUrl(c) + "/api/app/" + appId + "/reviews";
        if (viewerId > 0) url += "?viewer_id=" + viewerId;
        return url;
    }

    public static String reviewReactionUrl(android.content.Context c, int reviewId) {
        return baseUrl(c) + "/api/review/" + reviewId + "/reaction";
    }

    public static String reviewCommentsUrl(android.content.Context c, int reviewId) {
        return baseUrl(c) + "/api/review/" + reviewId + "/comments";
    }

    public static String reviewAddCommentUrl(android.content.Context c, int reviewId) {
        return baseUrl(c) + "/api/review/" + reviewId + "/comment";
    }

    public static String reviewReportUrl(android.content.Context c, int reviewId) {
        return baseUrl(c) + "/api/review/" + reviewId + "/report";
    }
    
    public static String logoUrl(Context c) {
        return baseUrl(c) + "/logo.png";
    }

    public static String clientLatestUrl(Context c) {
        return baseUrl(c) + "/api/client/latest";
    }

    public static String clientAnalyticsUrl(Context c) {
        return baseUrl(c) + "/api/client/analytics";
    }
}