package io.github.konbini.market.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.ui.AppDetailActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;

public class UpdateCheckService extends Service {
    public IBinder onBind(Intent intent) { return null; }

    public int onStartCommand(Intent intent, int flags, int startId) {
        LocaleHelper.applySavedLocale(this);
        new Thread(new Runnable() {
            public void run() {
                checkUpdates();
                stopSelf();
            }
        }).start();
        return START_NOT_STICKY;
    }

    private void checkUpdates() {
        try {
            HashMap<String, PackageInfo> installed = new HashMap<String, PackageInfo>();
            PackageManager pm = getPackageManager();
            List<PackageInfo> pkgs = pm.getInstalledPackages(0);
            for (int i = 0; i < pkgs.size(); i++) {
                PackageInfo pi = pkgs.get(i);
                if (pi != null && pi.packageName != null) installed.put(pi.packageName, pi);
            }
            scanEndpoint("/api/apps?is_game=false", installed);
            scanEndpoint("/api/apps?is_game=true", installed);
        } catch (Exception e) { }
    }

    private void scanEndpoint(String endpoint, java.util.Map<String, PackageInfo> installed) {
        try {
            String s = Http.getString(Api.baseUrl(this) + endpoint);
            if (s == null) return;
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String pkg = o.optString("package", o.optString("package_name", ""));
                if (pkg == null || pkg.length() == 0) continue;
                PackageInfo pi = installed.get(pkg);
                if (pi == null) continue;
                int serverVersionCode = o.optInt("versionCode", o.optInt("version_code", 0));
                if (serverVersionCode <= 0) continue;
                if (serverVersionCode > pi.versionCode) {
                    int appId = o.optInt("id", 0);
                    String appName = o.optString("name", pkg);
                    int lastNotified = notifiedPrefs().getInt("upd_" + pkg, 0);
                    if (serverVersionCode > lastNotified) {
                        showUpdateNotification(appId, appName);
                        notifiedPrefs().edit().putInt("upd_" + pkg, serverVersionCode).commit();
                    }
                }
            }
        } catch (Exception e) { }
    }

    private SharedPreferences notifiedPrefs() {
        return getSharedPreferences("update_notify_state", MODE_PRIVATE);
    }

    @SuppressWarnings("deprecation")
    private void showUpdateNotification(int appId, String appName) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent open = new Intent(this, AppDetailActivity.class);
        open.putExtra("app_id", appId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 20000 + appId, open, PendingIntent.FLAG_UPDATE_CURRENT);

        int iconRes = getResources().getIdentifier("stat_notify_marketplace_update", "drawable", getPackageName());
        if (iconRes == 0) iconRes = android.R.drawable.stat_notify_more;
        Notification n = new Notification(iconRes, null, System.currentTimeMillis());
        n.flags = Notification.FLAG_AUTO_CANCEL;
        try {
            Method m = n.getClass().getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
            m.invoke(n, this, getString(R.string.updates_available), getString(R.string.update_available_for_named, appName), pi);
        } catch (Exception e) { }
        nm.notify(20000 + appId, n);
    }
}
