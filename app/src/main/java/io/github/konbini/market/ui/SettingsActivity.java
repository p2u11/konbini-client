package io.github.konbini.market.ui;

import io.github.konbini.market.R;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.DataOutputStream;

public class SettingsActivity extends Activity {

    private EditText edtServer;
    private Spinner spnLang;
    private Button btnSave;
    private CheckBox chkAutoInstallRoot;
    private boolean ignoreRootToggle = false;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_settings);

        edtServer = (EditText) findViewById(R.id.edtServer);
        spnLang = (Spinner) findViewById(R.id.spnLang);
        btnSave = (Button) findViewById(R.id.btnSave);
        chkAutoInstallRoot = (CheckBox) findViewById(R.id.chkAutoInstallRoot);

        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.lang_ru), getString(R.string.lang_en)});
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLang.setAdapter(a);

        edtServer.setText(Prefs.getServer(this));
        String lang = Prefs.getLang(this);
        spnLang.setSelection("en".equals(lang) ? 1 : 0);

        if (chkAutoInstallRoot != null) {
            chkAutoInstallRoot.setChecked(Prefs.isAutoInstallRoot(this));
            chkAutoInstallRoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (ignoreRootToggle) return;
                    if (isChecked) {
                        if (requestRootAccess()) {
                            Prefs.setRootGranted(SettingsActivity.this, true);
                            Prefs.setAutoInstallRoot(SettingsActivity.this, true);
                            Toast.makeText(SettingsActivity.this, getString(R.string.auto_install_root), Toast.LENGTH_SHORT).show();
                        } else {
                            Prefs.setRootGranted(SettingsActivity.this, false);
                            Prefs.setAutoInstallRoot(SettingsActivity.this, false);
                            ignoreRootToggle = true;
                            chkAutoInstallRoot.setChecked(false);
                            ignoreRootToggle = false;
                            Toast.makeText(SettingsActivity.this, "ROOT denied", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Prefs.setAutoInstallRoot(SettingsActivity.this, false);
                    }
                }
            });
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String host = edtServer.getText().toString().trim();
                if (host.length() == 0) host = "94.156.115.120";
                Prefs.setServer(SettingsActivity.this, host);

                String sel = (spnLang.getSelectedItemPosition() == 1) ? "en" : "ru";
                Prefs.setLang(SettingsActivity.this, sel);

                LocaleHelper.applySavedLocale(SettingsActivity.this);
                Toast.makeText(SettingsActivity.this, R.string.save, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private boolean requestRootAccess() {
        Process p = null;
        DataOutputStream os = null;
        try {
            p = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int rc = p.waitFor();
            return rc == 0;
        } catch (Exception e) {
            return false;
        } finally {
            try { if (os != null) os.close(); } catch (Exception e) { }
            try { if (p != null) p.destroy(); } catch (Exception e) { }
        }
    }
}
