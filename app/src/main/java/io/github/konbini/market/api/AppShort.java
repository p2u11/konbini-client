package io.github.konbini.market.api;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by paul on 14/07/26.
 */

public class AppShort {
    public int id = 0;
    public String name;
    public int api = 0;
    public String categoryCode = "";
    public String categoryLabel = "";
    public String icon = "";
    public String author = "";
    public String description = "";
    public boolean is_game = false;
    public ArrayList<String> abis = new ArrayList<>();
    public int downloads = 0;
    public double rating = 0;
    public String packageName = "";

    public AppShort(JSONObject obj) throws JSONException {
        this.id = obj.getInt("id");
        Log.d("AppShort@36", String.valueOf(this.id));
        this.name = obj.getString("name");
        this.api = obj.getInt("api");
        this.categoryCode = obj.optString("categoryCode", "other_apps");
        this.categoryLabel = obj.optString("categoryLabel", "Other apps");
        this.icon = obj.optString("icon", "");
        JSONArray abis_json = obj.getJSONArray("abis");
        for (int i = 0; i < abis_json.length(); i++) {
            abis.add(abis_json.getString(i));
        }
        this.description = obj.optString("description", "No description provided.");
        this.is_game = obj.optBoolean("isGame", false);
        this.author = obj.optString("author", "Unknown");
        this.downloads = obj.optInt("downloads", 0);
        this.rating = obj.optDouble("rating", 0.0);
        this.packageName = obj.getString("packageName");
    }

    boolean isSupported() {
        if (this.api > Build.VERSION.SDK_INT) {
            Log.e("AppVersion", "REJECTED on minSdk -> App requires API: " + this.api + ", Device is API: " + Build.VERSION.SDK_INT);
            return false;
        }

        if (this.abis.isEmpty()) return true; // noarch apks that don't have any libraries

        List<String> abis = Arrays.asList((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                ? Build.SUPPORTED_ABIS : new String[]{Build.CPU_ABI, Build.CPU_ABI2});

        boolean hasMatchingAbi = !Collections.disjoint(this.abis, abis);

        if (!hasMatchingAbi) {
            Log.e("AppVersion", "REJECTED on ABI mismatch -> App ABIs: " + this.abis + " | Device ABIs: " + abis);
        }

        return hasMatchingAbi;
    }
}
