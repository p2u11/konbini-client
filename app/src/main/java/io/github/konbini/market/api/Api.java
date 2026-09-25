package io.github.konbini.market.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.*;
import org.apache.http.Header;

import io.github.konbini.market.net.Http;
import io.github.konbini.market.util.Prefs;

import org.apache.http.client.HttpClient;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by paul on 14/07/26.
 */

public class Api {
    private static String default_base_url = "http://apk.pyt.pp.ua";
    private static final String CACHE_PREFS = "api_response_cache";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private String base_url = default_base_url;
    private Context context;
    private ArrayList<AppShort> memoryApps;
    private long memoryAppsAt;

    private int sdk = Build.VERSION.SDK_INT;
    private String supportedAbis = "";
    private String platformQueries = "";

    private static Api instance;

    private SyncHttpClient client = new SyncHttpClient();

    private Api(String base_url) {
        if (base_url != null)
            this.base_url = base_url;

        if (!this.base_url.startsWith("http://") && !this.base_url.startsWith("https://")) {
            this.base_url = "http://"+this.base_url;
        }

        if (this.sdk >= Build.VERSION_CODES.LOLLIPOP) {
            for (int i = 0; i < Build.SUPPORTED_ABIS.length; i++) {
                supportedAbis += (Build.SUPPORTED_ABIS[i]) + ((i < Build.SUPPORTED_ABIS.length - 1) ? "," : "");
            }
        } else {
            supportedAbis = (Build.CPU_ABI+","+Build.CPU_ABI2);
        }

        Log.d("Supported ABIs", supportedAbis);

        platformQueries = String.format(Locale.ENGLISH, "api=%d&abis=%s", sdk, supportedAbis);
    }

    public static synchronized Api getInstance(String base_url) {
        if (instance == null) {
            instance = new Api(base_url);
        }
        return instance;
    }

    public static synchronized Api getInstance(Context context) {
        String url = Prefs.getServer(context);
        Api api = getInstance(url.equals("") ? default_base_url : url);
        api.context = context.getApplicationContext();
        return api;
    }

    public String getBaseUrl(Context c) {
        return this.base_url;
    }

    public String getSupportedAbis() { return this.supportedAbis; }

    private SharedPreferences cache() {
        return context == null ? null : context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
    }

    private String cacheKey(String suffix) {
        return "response_" + base_url.replaceAll("[^A-Za-z0-9]", "_") + "_" + suffix;
    }

    private String readCached(String suffix, boolean freshOnly) {
        SharedPreferences preferences = cache();
        if (preferences == null) return null;

        String key = cacheKey(suffix);
        String value = preferences.getString(key, null);
        if (value == null) return null;

        long savedAt = preferences.getLong(key + "_at", 0L);
        if (freshOnly && System.currentTimeMillis() - savedAt > CACHE_TTL_MS) return null;
        return value;
    }

    private void writeCached(String suffix, String value) {
        SharedPreferences preferences = cache();
        if (preferences == null || value == null) return;

        String key = cacheKey(suffix);
        preferences.edit().putString(key, value).putLong(key + "_at", System.currentTimeMillis()).commit();
    }

    // Get top apps
    public ArrayList<AppShort> getTopApps() {
        String url = base_url + "/api/apps.json";
        final ArrayList<AppShort> apps = new ArrayList<>();
        final boolean[] success = {false};
        if (memoryApps != null && System.currentTimeMillis() - memoryAppsAt <= CACHE_TTL_MS) {
            return new ArrayList<>(memoryApps);
        }

        String cached = readCached("top_apps", true);
        if (cached != null && parseApps(cached, apps)) {
            rememberApps(apps);
            return apps;
        }

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                Log.i("Api", String.format(Locale.ENGLISH, "Got %d status code, yay!", statusCode));
                String result;
                JSONArray array;
                try {
                    result = new String(responseBody, "UTF-8");
                    Log.d("Api", "onSuccess: "+result);
                    writeCached("top_apps", result);
                    success[0] = parseApps(result, apps);
                    if (success[0]) rememberApps(apps);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("Api", String.format(Locale.ENGLISH, "Got %d status code... :(", statusCode));
            }
        });

        if (!success[0]) {
            String stale = readCached("top_apps", false);
            if (stale != null && parseApps(stale, apps)) {
                rememberApps(apps);
                return apps;
            }
        }
        return success[0] ? apps : null;
    }

    public ArrayList<AppShort> searchApps(String query) {
        ArrayList<AppShort> source = getTopApps();
        if (source == null) return null;

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        ArrayList<AppShort> matches = new ArrayList<>();
        for (AppShort app : source) {
            String name = app.name == null ? "" : app.name.toLowerCase(Locale.ROOT);
            String packageName = app.packageName == null ? "" : app.packageName.toLowerCase(Locale.ROOT);
            if (name.contains(normalizedQuery) || packageName.contains(normalizedQuery)) {
                matches.add(app);
            }
        }
        return matches;
    }

    private void rememberApps(ArrayList<AppShort> apps) {
        memoryApps = new ArrayList<>(apps);
        memoryAppsAt = System.currentTimeMillis();
    }

    private boolean parseApps(String result, ArrayList<AppShort> apps) {
        try {
            JSONArray array = new JSONArray(result);
            for (int i = 0; i < array.length(); i++) {
                AppShort app = new AppShort(array.getJSONObject(i));
                if (app.isSupported()) apps.add(app);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

//    public ArrayList<> getAppCategories() {
//
//    }

    public ArrayList<AppShort> getAuthorApps(String author) {
        if (author == null || author.length() == 0) return this.getTopApps();
        ArrayList<AppShort> apps = filterApps(getTopApps(), author, true);
        return apps;
    }

    public ArrayList<AppShort> getCategoryApps(String category) {
        if (category == null || category.length() == 0) return this.getTopApps();
        return filterApps(getTopApps(), category, false);
    }

    private ArrayList<AppShort> filterApps(ArrayList<AppShort> source, String value, boolean byAuthor) {
        if (source == null) return null;
        ArrayList<AppShort> filtered = new ArrayList<>();
        for (AppShort app : source) {
            String field = byAuthor ? app.author : app.categoryCode;
            if (value.equals(field)) filtered.add(app);
        }
        return filtered;
    }

    public App getApp(final int app_id) {
        String url = String.format(Locale.ENGLISH, "%s/api/apps/%d.json", base_url, app_id);
        Log.d("Api", "line 178");
        Log.d("Api", url);
        AsyncHttpClient client = new SyncHttpClient();
        final App[] app = new App[1];
        final boolean[] success = {false};
        String cached = readCached("app_" + app_id, true);
        if (cached != null) {
            try {
                return new App(new JSONObject(cached));
            } catch (Exception e) {
                Log.w("Api", "Ignoring invalid cached app response", e);
            }
        }

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                Log.i("Api", String.format(Locale.ENGLISH, "Got %d status code, yay!", statusCode));
                String result;
                JSONArray array;
                try {
                    result = new String(responseBody, "UTF-8");
                    Log.d("Api", "onSuccess: "+result);
                    writeCached("app_" + app_id, result);
                    app[0] = new App(new JSONObject(result));
                    success[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("Api", String.format(Locale.ENGLISH, "Got %d status code... :( (line 199)", statusCode));
            }
        });
        if (!success[0] || app[0] == null) {
            String stale = readCached("app_" + app_id, false);
            if (stale != null) {
                try {
                    app[0] = new App(new JSONObject(stale));
                    success[0] = true;
                } catch (Exception e) {
                    Log.w("Api", "Ignoring invalid cached app response", e);
                }
            }
        }
        Log.d("Api", "getApp: " + (app[0] == null ? "null" : app[0].versions.size()));
        return success[0] ? app[0] : null;
    }

    public String clientUpdateAvailable() {
        String url = this.base_url + "/api/client/update";
        return null;
    }
}
