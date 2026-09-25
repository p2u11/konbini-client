package io.github.konbini.market.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.model.AppItem;
//import io.github.konbini.market.net.Api;
import io.github.konbini.market.api.Api;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CategoryAppsActivity extends Activity {
    private ListView list;
    private TextView titleView, subtitleView;
    private View loadingOverlay;
    private AppListAdapter adapter;
    private ArrayList<AppShort> items = new ArrayList<>();
    private ArrayList<AppShort> originalItems = new ArrayList<>();
    private View promoRoot;
    private ImageView promoIcon;
    private TextView promoText;
    private TextView appName;
    private Button btnTopFree, btnTopDownloads;
    private AppShort promoApp;
    private boolean sortDownloads = false;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_category_apps);

        final String title = getIntent().getStringExtra("title");
        final String query = getIntent().getStringExtra("query");
        final String type = getIntent().getStringExtra("type");
        final boolean isGame = getIntent().getBooleanExtra("is_game", false);

        titleView = (TextView) findViewById(R.id.txtTitle);
        subtitleView = (TextView) findViewById(R.id.txtSubtitle);
        list = (ListView) findViewById(R.id.list);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        btnTopFree = (Button) findViewById(R.id.btnTopFree);
        btnTopDownloads = (Button) findViewById(R.id.btnTopDownloads);

        if (list == null) {
            Toast.makeText(this, "list not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        View promoHeader = LayoutInflater.from(this).inflate(R.layout.view_promotion_app, list, false);
        promoRoot = promoHeader.findViewById(R.id.promoRoot);
        promoIcon = (ImageView) promoHeader.findViewById(R.id.promoIcon);
        promoText = (TextView) promoHeader.findViewById(R.id.promoText);
        appName = (TextView) promoHeader.findViewById(R.id.appName);
        list.addHeaderView(promoHeader, null, false);

        try {
            ImageButton btnHome = (ImageButton) findViewById(R.id.btnHome);
            if (btnHome != null) {
                                btnHome.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent i = new Intent(CategoryAppsActivity.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        finish();
                    }
                });
            }
            ((ImageButton)findViewById(R.id.btnSearch)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { startActivity(new Intent(CategoryAppsActivity.this, SearchActivity.class)); }
            });
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/storopia.ttf");
            if (titleView != null) titleView.setTypeface(tf);
        } catch (Exception e) { }

        if (titleView != null) titleView.setText(getString(isGame ? R.string.games1 : R.string.apps1));
        if (subtitleView != null) subtitleView.setText(title == null || title.length() == 0 ? getString(isGame ? R.string.all_games : R.string.all_apps) : title);

        adapter = new AppListAdapter(this, items);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int idx = position - list.getHeaderViewsCount();
                if (idx < 0 || idx >= items.size()) return;
                AppShort it = items.get(idx);
                Intent i = new Intent(CategoryAppsActivity.this, AppDetailActivity.class);
                i.putExtra("app_id", it.id);
                startActivity(i);
            }
        });

        if (btnTopFree != null) btnTopFree.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { sortDownloads = false; applySort(); updateTabButtons(); }
        });
        if (btnTopDownloads != null) btnTopDownloads.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { sortDownloads = true; applySort(); updateTabButtons(); }
        });
        if (promoRoot != null) promoRoot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (promoApp == null) return;
                Intent i = new Intent(CategoryAppsActivity.this, AppDetailActivity.class);
                i.putExtra("app_id", promoApp.id);
                startActivity(i);
            }
        });

        updateTabButtons();
        loadApps(type, query, isGame);
    }

    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.refreshInstalledPackages();
            adapter.notifyDataSetChanged();
        }
    }

    private void loadApps(final String type, final String query, final boolean isGame) {
        showLoading(true);
        final Api api = Api.getInstance(CategoryAppsActivity.this);
        new AsyncTask<Void, Void, ArrayList<AppShort>>() {
            protected ArrayList<AppShort> doInBackground(Void... v) {
                try {
//                    String url = Api.baseUrl(CategoryAppsActivity.this) + "/api/apps?is_game=" + (isGame ? "true" : "false");
                    ArrayList<AppShort> apps;
                    switch (type) {
                        case "author":
                            apps = api.getAuthorApps(query);
                            break;
                        case "category":
                            apps = api.getCategoryApps(query);
                            break;
                        default:
                            return null;
                    }
                    return apps;
//                    String s = Http.getString(url);
//                    if (s == null) return null;
//                    JSONArray arr = new JSONArray(s);
//                    ArrayList<AppShort> out = new ArrayList<>();
//                    int deviceApi = Build.VERSION.SDK_INT;
//                    for (int i = 0; i < arr.length(); i++) {
//                        JSONObject o = arr.getJSONObject(i);
//                        out.add(new AppShort(o));
//                    }
//                    return out;
                } catch (Exception e) { return null; }
            }
            protected void onPostExecute(ArrayList<AppShort> out) {
                showLoading(false);
                if (out == null) {
                    Toast.makeText(CategoryAppsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    return;
                }
                originalItems.clear();
                originalItems.addAll(out);
                bindPromotion();
                applySort();
                updateTabButtons();
            }
        }.execute();
    }

    private void applySort() {
        items.clear();
        items.addAll(originalItems);
        if (sortDownloads) {
            Collections.sort(items, new Comparator<AppShort>() {
                public int compare(AppShort a, AppShort b) { return b.downloads - a.downloads; }
            });
        } else {
            Collections.sort(items, new Comparator<AppShort>() {
                public int compare(AppShort a, AppShort b) {
                    int r = Float.compare((float)b.rating, (float)a.rating);
                    if (r != 0) return r;
                    return a.name.compareToIgnoreCase(b.name);
                }
            });
        }
        adapter.refreshInstalledPackages();
        adapter.notifyDataSetChanged();
    }

    private void bindPromotion() {
        if (promoRoot == null || promoIcon == null || promoText == null) return;
        if (originalItems.isEmpty()) {
            promoRoot.setVisibility(View.GONE);
            return;
        }
        promoRoot.setVisibility(View.VISIBLE);
        promoApp = originalItems.get(new Random().nextInt(originalItems.size()));
        ImageLoader.load(this, promoApp.icon, promoIcon, R.drawable.icon_placeholder);
        String text = promoApp.description;
        if (text.length() == 0) text = promoApp.name;
        if (text.length() > 90) text = text.substring(0, 90) + "...";
        promoText.setText(text);
        appName.setText(promoApp.name);
    }

    private void updateTabButtons() {
        if (btnTopFree != null) {
            btnTopFree.setCompoundDrawablePadding(6);
            btnTopFree.setCompoundDrawablesWithIntrinsicBounds(sortDownloads ? R.drawable.btn_strip_mark_off : R.drawable.btn_strip_mark_on, 0, 0, 0);
        }
        if (btnTopDownloads != null) {
            btnTopDownloads.setCompoundDrawablePadding(6);
            btnTopDownloads.setCompoundDrawablesWithIntrinsicBounds(sortDownloads ? R.drawable.btn_strip_mark_on : R.drawable.btn_strip_mark_off, 0, 0, 0);
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
