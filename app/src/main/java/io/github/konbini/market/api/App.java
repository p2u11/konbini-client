package io.github.konbini.market.api;

import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by paul on 14/07/26.
 */

public class App {
    public int id;
    public String name;
    public String author = "";
    public String packageId;
    public String icon;
    public ArrayList<String> screenshots = new ArrayList<>();
    public String description = "";
    public SparseArray<AppVersion> versions = new SparseArray<AppVersion>();
    public String categoryId = "";
    public AppResourceList resources;
//    public ArrayList<Review> reviews;

    public App(JSONObject obj) throws JSONException {
        this.id = obj.getInt("id");
        this.name = obj.getString("name");
        this.author = obj.getString("author");
        this.packageId = obj.getString("packageId");
        this.description = obj.getString("description");

        JSONArray versions = obj.getJSONArray("versions");
        Log.d("App", "App versions: "+versions.toString());
        for (int i = 0; i < versions.length(); i++) {
            Log.d("App", "App version: "+versions.getJSONObject(i));
            AppVersion version = new AppVersion(versions.getJSONObject(i));
            this.versions.append(version.id, version);
        }

        this.icon = obj.getString("icon");
        JSONArray screenshots = obj.getJSONArray("screenshots");
        for (int i = 0; i < screenshots.length(); i++) {
            this.screenshots.add(screenshots.getString(i));
        }

        this.categoryId = obj.getString("categoryId");
        this.resources = new AppResourceList(obj.getJSONArray("resources"));
    }

    public AppVersion getFirstVersion() {
        if (versions.size() == 0) return null;
        AppVersion firstVersion = null;
        for (int j = 0; j < versions.size(); j++) {
            AppVersion version = versions.valueAt(j);
            if (version == null) continue;
            firstVersion = (firstVersion == null || firstVersion.versionCode > version.versionCode)
                    ? version : firstVersion;
        }
        return firstVersion;
    }

    public AppVersion getLastVersion() {
        if (versions.size() == 0) return null;
        AppVersion lastVersion = null;
        for (int j = 0; j < versions.size(); j++) {
            AppVersion version = versions.valueAt(j);
            if (version == null) continue;
            lastVersion = (lastVersion == null || lastVersion.versionCode < version.versionCode)
                    ? version : lastVersion;
        }
        return lastVersion;
    }

    public SparseArray<AppVersion> supportedVersions() {
        Log.d("App", "Versions found:"+String.valueOf(versions.size()));
        SparseArray<AppVersion> result = new SparseArray<>();

        for (int j = 0; j < versions.size(); j++) {
            AppVersion version = versions.valueAt(j);
            if (version == null) continue;
            if (version.isSupported()) result.append(version.id, version);
        }

        return result;
    }

    public boolean isSupported() {
        SparseArray<AppVersion> supportedVersions = this.supportedVersions();
        Log.d("App", String.valueOf(supportedVersions.size()));
        return supportedVersions.size() > 0;
        /*for (int j = 0; j < versions.size(); j++) {
            AppVersion version = versions.valueAt(j);
            if (version == null) continue;
            if (version.isSupported()) return true;
        }

        return false;*/
    }
}

