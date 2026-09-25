package io.github.konbini.market.api;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by paul on 14/07/26.
 */

public class AppResourceList {
    public AppResource icon = null;
    public ArrayList<AppResource> screenshots;

    public AppResourceList(JSONArray obj) throws JSONException {
        this.screenshots = new ArrayList<>();
        for (int i = 0; i < obj.length(); i++) {
            AppResource appRes = new AppResource(obj.getJSONObject(i));
            if (appRes.type.equals("ICON")) {
                this.icon = appRes;
                continue;
            }
            this.screenshots.add(appRes);
        }
    }
}
