package io.github.konbini.market.ui;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class ProfileActivity extends Activity {

    private ImageView imgAvatar;
    private Spinner spAvatar;
    private EditText edtDesc;
    private TextView txtUser, txtCreated;
    private Button btnSave, btnLogout;

    private int userId;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_profile);

        userId = Prefs.getUserId(this);
        if (userId <= 0) {
            msg("Login required");
            finish();
            return;
        }

        imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        spAvatar = (Spinner) findViewById(R.id.spAvatar);
        edtDesc = (EditText) findViewById(R.id.edtDesc);
        txtUser = (TextView) findViewById(R.id.txtUser);
        txtCreated = (TextView) findViewById(R.id.txtCreated);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        txtUser.setText(Prefs.getUsername(this));

        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveProfile(); }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.logout(ProfileActivity.this);
                msg("Logged out");
                finish();
            }
        });

        loadAvatarsThenProfile();
    }

    private void loadAvatarsThenProfile() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, Object[]>() {
            protected Object[] doInBackground(Void... v) {
                try {
                    String sA = Http.getString(Api.avatarsUrl(ProfileActivity.this));
                    if (sA == null) return null;
                    JSONArray aArr = new JSONArray(sA);

                    String[] avatars = new String[aArr.length()];
                    for (int i = 0; i < aArr.length(); i++) {
                        avatars[i] = aArr.getString(i);
                    }

                    String sP = Http.getString(Api.userProfileUrl(ProfileActivity.this, userId));
                    if (sP == null) return null;
                    JSONObject prof = new JSONObject(sP);

                    return new Object[] { avatars, prof };
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(Object[] out) {
                try { pd.dismiss(); } catch (Exception e) {}

                if (out == null) {
                    msg(getString(R.string.error_network));
                    return;
                }

                final String[] avatars = (String[]) out[0];
                final JSONObject prof = (JSONObject) out[1];

                ArrayAdapter<String> ad = new ArrayAdapter<String>(ProfileActivity.this,
                        android.R.layout.simple_spinner_item, avatars);
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spAvatar.setAdapter(ad);

                spAvatar.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                        String file = avatars[pos];
                        ImageLoader.load(ProfileActivity.this, Api.avatarUrl(ProfileActivity.this, file),
                                imgAvatar, R.drawable.icon_placeholder);
                    }
                    public void onNothingSelected(android.widget.AdapterView<?> parent) { }
                });

                String username = prof.optString("username", Prefs.getUsername(ProfileActivity.this));
                String avatar = prof.optString("avatar", "default_avatar.png");
                String desc = prof.optString("description", "");
                String created = prof.optString("created_at", "");

                txtUser.setText(username);
                edtDesc.setText(desc);

                if (created != null && created.length() > 0) {
                	txtCreated.setText("Created: " + created);
                } else {
                	txtCreated.setText("Created: " + created);
                }

                int idx = 0;
                for (int i = 0; i < avatars.length; i++) {
                    if (avatars[i].equalsIgnoreCase(avatar)) { idx = i; break; }
                }
                spAvatar.setSelection(idx);
                ImageLoader.load(ProfileActivity.this, Api.avatarUrl(ProfileActivity.this, avatars[idx]),
                        imgAvatar, R.drawable.icon_placeholder);
            }
        }.execute();
    }

    private void saveProfile() {
        final String avatar = (String) spAvatar.getSelectedItem();
        final String desc = edtDesc.getText().toString();

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Saving...");
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("avatar", avatar);
                    o.put("description", desc);
                    return Http.putJson(Api.userProfileUrl(ProfileActivity.this, userId), o.toString());
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(String s) {
                try { pd.dismiss(); } catch (Exception e) {}

                if (s == null) {
                    msg(getString(R.string.error_network));
                    return;
                }

                // ������ ������ ������ {"success":true,...} ��� �������
                msg("Saved");
            }
        }.execute();
    }

    private void msg(String s) {
        try {
            new AlertDialog.Builder(this)
                    .setMessage(s)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {}
    }
}