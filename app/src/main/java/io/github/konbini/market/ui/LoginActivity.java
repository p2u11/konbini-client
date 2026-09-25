package io.github.konbini.market.ui;

import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText edtUser, edtPass;
    private Button btnLogin, btnLogout;
    private TextView txtStatus;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_login);

        Toast.makeText(this, "Social features not implemented yet!", Toast.LENGTH_LONG).show();
        this.finish();

        edtUser = (EditText) findViewById(R.id.edtUser);
        edtPass = (EditText) findViewById(R.id.edtPass);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        txtStatus = (TextView) findViewById(R.id.txtStatus);

        refreshUi();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doLogin(); }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.logout(LoginActivity.this);
                refreshUi();
                msg("OK");
            }
        });
    }

    private void refreshUi() {
        boolean logged = Prefs.isLoggedIn(this);

        btnLogout.setEnabled(logged);

        if (logged) {
            String u = Prefs.getUsername(this);
            txtStatus.setText("Logged in as " + (u.length() > 0 ? u : ("ID " + Prefs.getUserId(this))));
            edtUser.setText(u);
            edtPass.setText("");
        } else {
        	txtStatus.setText("Not logged in");
        }
    }

    private void doLogin() {
        final String u = edtUser.getText().toString().trim();
        final String p = edtPass.getText().toString();

        if (u.length() == 0 || p.length() == 0) {
        	msg("Enter username and password");
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setCancelable(false);
        pd.show();

        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("username", u);
                    o.put("password", p);
                    return Http.postJson(Api.loginUrl(LoginActivity.this), o.toString());
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

                try {
                    JSONObject o = new JSONObject(s);

                    if (!o.optBoolean("success", false)) {
                        msg(o.optString("error", "Login failed"));
                        return;
                    }

                    int id = o.optInt("user_id", 0);
                    String name = o.optString("username", "");
                    String authKey = o.optString("token", "");

                    if (id <= 0) {
                        msg("Bad response");
                        return;
                    }

                    Prefs.setAuth(LoginActivity.this, id, name, authKey);
                    refreshUi();
                    msg("OK");
                    finish();
                } catch (Exception e) {
                    msg("Bad response: " + s);
                }
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
