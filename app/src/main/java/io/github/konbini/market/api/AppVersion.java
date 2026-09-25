package io.github.konbini.market.api;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by paul on 14/07/26.
 */

public class AppVersion {
    public int id;
    public int versionCode;
    public String versionName;
    public int minSdk;
    public ArrayList<String> abis = new ArrayList<>();
    public String downloadUrl;

    public AppVersion(JSONObject obj) throws JSONException {
        this.id = obj.getInt("id");
        this.versionCode = obj.getInt("versionCode");
        this.versionName = obj.getString("versionName");
        this.minSdk = obj.getInt("minSdk");

        JSONArray abis_json = obj.getJSONArray("abis");
        for (int i = 0; i < abis_json.length(); i++) {
            abis.add(abis_json.getString(i));
        }

        this.downloadUrl = obj.getString("downloadUrl");
    }

    public boolean isSupported() {
        if (minSdk > Build.VERSION.SDK_INT) {
            Log.e("AppVersion", "REJECTED on minSdk -> App requires API: " + minSdk + ", Device is API: " + Build.VERSION.SDK_INT);
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
