package io.github.konbini.market.api;

import org.json.JSONObject;

/**
 * Created by Paul on 9/24/2026.
 */

public class Category {
    private String id = "";
    private String name = "";

    public Category(JSONObject object) {
        this.id = object.optString("id", "other");
        this.name = object.optString("name", "Other");
    }

    public String getId() { return this.id; }
    public String getName() { return this.name; }
}
