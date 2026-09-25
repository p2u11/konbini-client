package io.github.konbini.market.model;

public class AppItem {
    public int id;
    public String name;
    public String developer;
    public String icon;
    public int api;
    public boolean isGame;

    public String packageName;
    public String version;
    public String description;
    public String categoryCode;
    public String categoryLabel;
    public float rating;
    public int downloads;
    public int versionCode;
    public int installedVersionCode;

    public static String safe(String s) { return s == null ? "" : s; }
}
