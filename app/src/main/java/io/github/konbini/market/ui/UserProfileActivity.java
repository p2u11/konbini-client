package io.github.konbini.market.ui;

import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class UserProfileActivity extends Activity {

    private int userId;

    private ImageView imgAvatar;
    private TextView txtUser, txtCreated, txtDesc;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_user_profile);

        userId = getIntent().getIntExtra("user_id", 0);

        imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        txtUser = (TextView) findViewById(R.id.txtUser);
        txtCreated = (TextView) findViewById(R.id.txtCreated);
        txtDesc = (TextView) findViewById(R.id.txtDesc);

        loadProfile();
    }

    private void loadProfile() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... v) {
                try {
                    String s = Http.getString(Api.userProfileUrl(UserProfileActivity.this, userId));
                    if (s == null) return null;
                    return new JSONObject(s);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject o) {
                try { pd.dismiss(); } catch (Exception e) {}

                if (o == null) {
                    txtUser.setText("Network error");
                    return;
                }

                String username = o.optString("username", "User");
                String avatar = o.optString("avatar", "default_avatar.png");
                String desc = o.optString("description", "");
                String created = o.optString("created_at", "");

                txtUser.setText(username + " (ID: " + userId + ")");
                txtDesc.setText(desc != null && desc.length() > 0 ? desc : "-");
                txtCreated.setText(created != null && created.length() > 0 ? ("Created: " + created) : "Created: -");

                ImageLoader.load(UserProfileActivity.this, Api.avatarUrl(UserProfileActivity.this, avatar),
                        imgAvatar, R.drawable.icon_placeholder);
            }
        }.execute();
    }
}