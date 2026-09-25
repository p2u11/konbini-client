package io.github.konbini.market.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by paul on 14/07/26.
 */

public class AppResource {
    /*
      id Int @id @default(autoincrement())
      type AppResourceType
      downloadUrl String
      createdAt   DateTime @default(now())
      appId       Int
      app         App      @relation(fields: [appId], references: [id])
      uploaderId  Int
      uploader    User     @relation(fields: [uploaderId], references: [id])
      moderatedObjectId  Int  @unique
      moderatedObject    ModeratedObject?
     */
    public int id;
    public String type;
    public String downloadUrl;

    public AppResource(JSONObject obj) throws JSONException {
        this.id = obj.getInt("id");
        this.type = obj.getString("type");
        this.downloadUrl = obj.getString("downloadUrl");
    }

    public AppResource(int id, String type, String downloadUrl) throws JSONException {
        this.id = id;
        this.type = type;
        this.downloadUrl = downloadUrl;
    }
}
