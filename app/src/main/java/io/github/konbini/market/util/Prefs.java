package io.github.konbini.market.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Prefs {
    private static final String P = "konbini_prefs";
    private static final String AUTH_SECRET = "fghjcbvnmbfdjkghhjdfnbvlkdfshgujirdgehty45uiy4t3y578347yr8ioue4roti7u340895784908itujoldrfikskjfl";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static String getServer(Context c) {
        return sp(c).getString("server", "apk.pyt.pp.ua");
    }

    public static void setServer(Context c, String host) {
        sp(c).edit().putString("server", host).commit();
    }

    public static int getPerPage(Context c) {
        return sp(c).getInt("per_page", 15);
    }

    public static void setPerPage(Context c, int v) {
        if (v < 5) v = 5;
        if (v > 200) v = 200;
        sp(c).edit().putInt("per_page", v).commit();
    }

    public static String getLang(Context c) {
        return sp(c).getString("lang", "en");
    }

    public static void setLang(Context c, String lang) {
        if (lang == null) lang = "ru";
        sp(c).edit().putString("lang", lang).commit();
    }

    // ---- AUTH ----
    public static int getUserId(Context c) {
        return sp(c).getInt("user_id", 0);
    }

    public static String getUsername(Context c) {
        return sp(c).getString("username", "");
    }

    public static boolean isLoggedIn(Context c) {
        return getUserId(c) > 0;
    }

    public static void setAuth(Context c, int userId, String username, String authKey) {
        String safeAuthKey = authKey == null ? "" : authKey;
        sp(c).edit()
                .putInt("user_id", userId)
                .putString("username", username == null ? "" : username)
                .putString("auth_key", safeAuthKey)
                .commit();
        // Populate the server-compatible auth token so API requests can authenticate.
        sp(c).edit()
                .putString("auth_token", buildAuthToken(userId, username == null ? "" : username,
                        "default_avatar.png", 0, safeAuthKey))
                .commit();
    }

    public static void logout(Context c) {
        sp(c).edit()
                .remove("user_id")
                .remove("username")
                .remove("auth_key")
                .remove("auth_token")
                .commit();
    }

    public static String getAuthKey(Context c) { return sp(c).getString("auth_key", ""); }

    public static String getAuthToken(Context c) {
        int userId = getUserId(c);
        if (userId <= 0) return "";
        String authKey = sp(c).getString("auth_key", "");
        if (authKey == null || authKey.length() == 0) return "";

        String token = buildAuthToken(userId, getUsername(c), "default_avatar.png", 0, authKey);
        if (token != null && token.length() > 0) {
            sp(c).edit().putString("auth_token", token).commit();
        }
        return token;
    }

    private static String buildAuthToken(int userId, String username, String avatar, int isPremium,
                                         String authKey) {
        try {
            String safeUsername = username == null ? "" : username;
            String safeAvatar = avatar == null ? "default_avatar.png" : avatar;
            String safeAuthKey = authKey == null ? "" : authKey;
            String payload = String.format(
                    Locale.US,
                    "{\"user_id\":%d,\"username\":%s,\"avatar\":%s,\"is_premium\":%d,\"auth_key\":%s,\"ts\":%d}",
                    userId,
                    quoteJson(safeUsername),
                    quoteJson(safeAvatar),
                    isPremium,
                    quoteJson(safeAuthKey),
                    System.currentTimeMillis() / 1000L
            );
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(AUTH_SECRET.getBytes("UTF-8"), "HmacSHA256");
            mac.init(key);
            byte[] sig = mac.doFinal(payload.getBytes("UTF-8"));
            String b64 = Base64.encodeToString(payload.getBytes("UTF-8"), Base64.NO_WRAP);
            return b64 + "." + toHex(sig);
        } catch (Exception e) {
            return "";
        }
    }

    private static String quoteJson(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format(Locale.US, "\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b & 0xff));
        }
        return sb.toString();
    }
    
    public static boolean isBannerHidden(Context c) {
        return c.getSharedPreferences("prefs", 0).getBoolean("hide_banner", false);
    }

    public static void setBannerHidden(Context c, boolean v) {
        c.getSharedPreferences("prefs", 0).edit().putBoolean("hide_banner", v).commit();
    }
    
    public static boolean isApi25WarningShown(Context c) {
        return sp(c).getBoolean("api25_warning_shown", false);
    }

    public static void setApi25WarningShown(Context c, boolean v) {
        sp(c).edit().putBoolean("api25_warning_shown", v).commit();
    }


    public static boolean isAutoInstallRoot(Context c) {
        return sp(c).getBoolean("auto_install_root", false);
    }

    public static void setAutoInstallRoot(Context c, boolean v) {
        sp(c).edit().putBoolean("auto_install_root", v).commit();
    }

    public static boolean isRootGranted(Context c) {
        return sp(c).getBoolean("root_granted", false);
    }

    public static void setRootGranted(Context c, boolean v) {
        sp(c).edit().putBoolean("root_granted", v).commit();
    }

    public static String getIconPack(Context c) {
        return sp(c).getString("icon_pack", "default");
    }

    public static void setIconPack(Context c, String v) {
        if (v == null || v.length() == 0) v = "default";
        sp(c).edit().putString("icon_pack", v).commit();
    }

    public static long getLastClientUpdateCheckAt(Context c) {
        return sp(c).getLong("last_client_update_check_at", 0L);
    }

    public static void setLastClientUpdateCheckAt(Context c, long v) {
        sp(c).edit().putLong("last_client_update_check_at", v).commit();
    }

    public static long getLastAnalyticsSentAt(Context c) {
        return sp(c).getLong("last_analytics_sent_at", 0L);
    }

    public static void setLastAnalyticsSentAt(Context c, long v) {
        sp(c).edit().putLong("last_analytics_sent_at", v).commit();
    }

}
