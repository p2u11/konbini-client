package io.github.konbini.market.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.model.AppItem;
import io.github.konbini.market.net.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.service.DownloadService;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadsActivity extends Activity {
    private static final int TYPE_SECTION = 0;
    private static final int TYPE_APP = 1;

    private ListView list;
    private View loadingOverlay;
    private TextView titleView;
    private DownloadsAdapter adapter;
    private ArrayList<RowItem> rows = new ArrayList<RowItem>();
    private LinearLayout currentDownloadsContainer;
    private TextView txtCurrentSection;
    private View currentDownloadsHeader;

    private final BroadcastReceiver dlReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!DownloadService.ACTION_PROGRESS.equals(intent.getAction())) return;
            refreshCurrentDownloads();
            boolean done = intent.getBooleanExtra("done", false);
            boolean error = intent.getBooleanExtra("error", false);
            boolean cancelled = intent.getBooleanExtra("cancelled", false);
            if (done || error || cancelled) loadInstalledMarketApps();
        }
    };

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_downloads);

        titleView = (TextView) findViewById(R.id.txtTitle);
        list = (ListView) findViewById(R.id.list);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        LinearLayout headerRoot = new LinearLayout(this);
        headerRoot.setOrientation(LinearLayout.VERTICAL);
        txtCurrentSection = new TextView(this);
        txtCurrentSection.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        txtCurrentSection.setBackgroundResource(R.drawable.market_header);
        txtCurrentSection.setPadding(7, 7, 7, 7);
        txtCurrentSection.setText(getString(R.string.currently_downloading));
        txtCurrentSection.setTextColor(0xffffffff);
        currentDownloadsContainer = new LinearLayout(this);
        currentDownloadsContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        currentDownloadsContainer.setOrientation(LinearLayout.VERTICAL);
        headerRoot.addView(txtCurrentSection);
        headerRoot.addView(currentDownloadsContainer);
        currentDownloadsHeader = headerRoot;
        list.addHeaderView(currentDownloadsHeader, null, false);
        try {
                        ((ImageButton)findViewById(R.id.btnHome)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(DownloadsActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                }
            });
            ((ImageButton)findViewById(R.id.btnSearch)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startActivity(new Intent(DownloadsActivity.this, SearchActivity.class));
                }
            });
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/storopia.ttf");
            titleView.setTypeface(tf);
        } catch (Exception e) {}
        if (currentDownloadsHeader != null) currentDownloadsHeader.setVisibility(View.GONE);

        adapter = new DownloadsAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                int headers = list.getHeaderViewsCount();
                int index = position - headers;

                if (index < 0 || index >= rows.size()) return;

                RowItem row = rows.get(index);

                if (row.type != TYPE_APP || row.app == null) return;

                Intent i = new Intent(DownloadsActivity.this, AppDetailActivity.class);
                i.putExtra("app_id", row.app.id);
                startActivity(i);
            }
        });
        refreshCurrentDownloads();
        loadInstalledMarketApps();
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(dlReceiver, new IntentFilter(DownloadService.ACTION_PROGRESS));
        refreshCurrentDownloads();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (rows.isEmpty()) loadInstalledMarketApps();
    }

    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(dlReceiver); } catch (Exception e) { }
    }

    private void refreshCurrentDownloads() {
        currentDownloadsContainer.removeAllViews();
        SharedPreferences p = getSharedPreferences("download_state", MODE_PRIVATE);
        String json = p.getString("tasks_json", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            LayoutInflater inf = LayoutInflater.from(this);
            for (int i = 0; i < arr.length(); i++) {
                final JSONObject o = arr.getJSONObject(i);
                final int appId = o.optInt("app_id", -1);
                String appName = o.optString("app_name", "");
                String statusText = o.optString("status_text", "");
                int percent = o.optInt("percent", 0);
                boolean installing = o.optBoolean("installing", false);
                String icon = o.optString("icon", "");

                View row = inf.inflate(R.layout.list_item_current_download, currentDownloadsContainer, false);
                ImageView img = (ImageView) row.findViewById(R.id.imgCurrent);
                TextView title = (TextView) row.findViewById(R.id.txtCurrentTitle);
                TextView status = (TextView) row.findViewById(R.id.txtCurrentStatus);
                ProgressBar progress = (ProgressBar) row.findViewById(R.id.progressCurrent);

                title.setText(appName);
                status.setText(statusText);
                if (installing) {
                    progress.setIndeterminate(true);
                } else {
                    progress.setIndeterminate(false);
                    progress.setProgress(percent);
                }
                if (icon != null && icon.length() > 0) ImageLoader.load(this, Api.iconUrl(this, icon), img, R.drawable.icon_placeholder);
                else img.setImageResource(R.drawable.icon_placeholder);

                row.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (appId > 0) {
                            Intent i = new Intent(DownloadsActivity.this, AppDetailActivity.class);
                            i.putExtra("app_id", appId);
                            startActivity(i);
                        }
                    }
                });
                currentDownloadsContainer.addView(row);
            }
            int vis = arr.length() > 0 ? View.VISIBLE : View.GONE;
            if (currentDownloadsHeader != null) currentDownloadsHeader.setVisibility(vis);
        } catch (Exception e) {
            if (currentDownloadsHeader != null) currentDownloadsHeader.setVisibility(View.GONE);
        }
    }

    private void loadInstalledMarketApps() {
        showLoading(true);
        new AsyncTask<Void, Void, ArrayList<RowItem>>() {
            protected ArrayList<RowItem> doInBackground(Void... params) {
                try {
                    HashMap<String, Integer> installed = new HashMap<String, Integer>();
                    PackageManager pm = getPackageManager();
                    List<PackageInfo> pkgs = pm.getInstalledPackages(0);
                    for (int i = 0; i < pkgs.size(); i++) {
                        PackageInfo pi = pkgs.get(i);
                        if (pi != null && pi.packageName != null) installed.put(pi.packageName, pi.versionCode);
                    }

                    ArrayList<AppItem> all = new ArrayList<AppItem>();
                    loadEndpoint("/api/apps?is_game=0", Build.VERSION.SDK_INT, installed, all);
                    loadEndpoint("/api/apps?is_game=1", Build.VERSION.SDK_INT, installed, all);

                    ArrayList<AppItem> updates = new ArrayList<AppItem>();
                    ArrayList<AppItem> installedOnly = new ArrayList<AppItem>();

                    for (int i = 0; i < all.size(); i++) {
                        AppItem a = all.get(i);
                        if (a.versionCode > 0 && a.installedVersionCode > 0 && a.versionCode > a.installedVersionCode) updates.add(a);
                        else installedOnly.add(a);
                    }

                    Comparator<AppItem> cmp = new Comparator<AppItem>() {
                        public int compare(AppItem a1, AppItem a2) {
                            return AppItem.safe(a1.name).compareToIgnoreCase(AppItem.safe(a2.name));
                        }
                    };
                    Collections.sort(updates, cmp);
                    Collections.sort(installedOnly, cmp);

                    ArrayList<RowItem> out = new ArrayList<RowItem>();
                    if (!updates.isEmpty()) {
                        RowItem sec = new RowItem();
                        sec.type = TYPE_SECTION;
                        sec.title = getString(R.string.available_updates);
                        out.add(sec);
                        for (int i = 0; i < updates.size(); i++) {
                            RowItem ri = new RowItem();
                            ri.type = TYPE_APP;
                            ri.app = updates.get(i);
                            out.add(ri);
                        }
                    }
                    RowItem sec2 = new RowItem();
                    sec2.type = TYPE_SECTION;
                    sec2.title = getString(R.string.downloads1);
                    out.add(sec2);
                    for (int i = 0; i < installedOnly.size(); i++) {
                        RowItem ri = new RowItem();
                        ri.type = TYPE_APP;
                        ri.app = installedOnly.get(i);
                        out.add(ri);
                    }
                    return out;
                } catch (Exception e) { return null; }
            }
            protected void onPostExecute(ArrayList<RowItem> out) {
                showLoading(false);
                if (out == null) {
                    Toast.makeText(DownloadsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    return;
                }
                rows.clear();
                rows.addAll(out);
                adapter.notifyDataSetChanged();
                if (rows.size() == 0) Toast.makeText(DownloadsActivity.this, R.string.no_downloads, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void loadEndpoint(String endpoint, int deviceApi, java.util.Map<String, Integer> installed, ArrayList<AppItem> out) {
        try {
            String s = Http.getString(Api.baseUrl(this) + endpoint);
            if (s == null) return;
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                AppItem a = new AppItem();
                a.id = o.optInt("id", 0);
                a.name = o.optString("name", "");
                a.developer = o.optString("developer", o.optString("author", ""));
                a.icon = o.optString("icon", "");
                a.api = o.optInt("api", 1);
                a.packageName = o.optString("package", o.optString("package_name", ""));
                a.isGame = o.optBoolean("is_game", false);
                a.categoryCode = o.optString("category_code", o.optString("category", ""));
                a.categoryLabel = o.optString("category_label", a.categoryCode);
                a.rating = (float) o.optDouble("rating", 0.0);
                a.downloads = o.optInt("downloads", 0);
                a.versionCode = o.optInt("versionCode", o.optInt("version_code", 0));
                Integer iv = installed.get(a.packageName);
                a.installedVersionCode = iv == null ? 0 : iv.intValue();
                a.description = o.optString("description", "");
                if (a.api <= deviceApi && a.packageName != null && installed.containsKey(a.packageName)) out.add(a);
            }
        } catch (Exception e) { }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private static class RowItem {
        int type;
        String title;
        AppItem app;
    }

    private class DownloadsAdapter extends BaseAdapter {
        public int getCount() { return rows.size(); }
        public Object getItem(int position) { return rows.get(position); }
        public long getItemId(int position) { return position; }
        public int getViewTypeCount() { return 2; }
        public int getItemViewType(int position) { return rows.get(position).type; }

        public View getView(int position, View convertView, ViewGroup parent) {
            RowItem row = rows.get(position);
            if (row.type == TYPE_SECTION) {
                View v = convertView;
                if (v == null || v.findViewById(R.id.sectionTitle) == null) v = LayoutInflater.from(DownloadsActivity.this).inflate(R.layout.list_item_section, parent, false);
                TextView tv = (TextView) v.findViewById(R.id.sectionTitle);
                tv.setText(row.title);
                return v;
            }

            View v = convertView;
            if (v == null || v.findViewById(R.id.title) == null) v = LayoutInflater.from(DownloadsActivity.this).inflate(R.layout.list_item_app, parent, false);

            ImageView img = (ImageView) v.findViewById(R.id.img);
            TextView title = (TextView) v.findViewById(R.id.title);
            TextView subtitle = (TextView) v.findViewById(R.id.subtitle);
            TextView status = (TextView) v.findViewById(R.id.txtStatus);
            RatingBar ratingBar = (RatingBar) v.findViewById(R.id.ratingBar);

            AppItem a = row.app;
            title.setText(AppItem.safe(a.name));
            subtitle.setText(AppItem.safe(a.developer));
            ratingBar.setRating(a.rating);

            if (a.versionCode > 0 && a.installedVersionCode > 0 && a.versionCode > a.installedVersionCode) {
                status.setText(getString(R.string.updates_available));
                status.setTextColor(0xfff28c18);
            } else {
                status.setText(getString(R.string.installed));
                status.setTextColor(0xff303030);
            }

            String iconUrl = (a.icon == null) ? "" : Api.iconUrl(DownloadsActivity.this, a.icon);
            ImageLoader.load(DownloadsActivity.this, iconUrl, img, R.drawable.icon_placeholder);
            return v;
        }
    }
}
