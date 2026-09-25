package io.github.konbini.market.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by paul on 14/07/26.
 */

public class ClientUpdate {
    public int latestCode;
    public String latestName;
    public String updateUrl;
    public String notes;

    public ClientUpdate(JSONObject obj) throws JSONException {
        this.latestCode = obj.getInt("latestCode");
        this.latestName = obj.getString("latestName");
        this.updateUrl = obj.getString("updateUrl");
        this.notes = obj.getString("notes");
    }
}
