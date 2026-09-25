package io.github.konbini.market.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.api.Api;

import io.github.konbini.market.net.Http;
import io.github.konbini.market.service.UpdateCheckService;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private ListView list;
    private AppListAdapter adapter;
    private ArrayList<AppShort> items = new ArrayList<>();

    private ImageButton btnSearch;
    private Button btnApps, btnGames, btnDownloads;
    private TextView txtSection, txtMarket, txtPromoType, txtBrowseCategory;
    private View loadingOverlay, promoMainRoot;
    private ImageButton logo;
    private ImageView promoIcon1, promoIcon2, promoIcon3, bannerImage;
    private ImageView promoMirror1, promoMirror2, promoMirror3;
    private PromoCategory currentPromoCategory;

    private Api api;

    private String supportedAbis;

    private static class PromoCategory {
        String code;
        String label;
        boolean isGame;
        ArrayList<AppShort> apps = new ArrayList<>();
    }

    private static ArrayList<AppShort> CACHE_ITEMS = new ArrayList<>();
    private static ArrayList<AppShort> CACHE_PROMO_SOURCE = new ArrayList<>();
    private static long CACHE_TIME = 0L;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        logo = (ImageButton) findViewById(R.id.logo);
        list = (ListView) findViewById(R.id.list);
        btnApps = (Button) findViewById(R.id.btnApps);
        btnGames = (Button) findViewById(R.id.btnGames);
        btnDownloads = (Button) findViewById(R.id.btnDownloads);
        btnSearch = (ImageButton) findViewById(R.id.btnSearch);
        txtMarket = (TextView) findViewById(R.id.txtMarket);

        View header = getLayoutInflater().inflate(R.layout.main_list_header, list, false);
        promoMainRoot = header.findViewById(R.id.promoMainRoot);
//        bannerImage = (ImageView) header.findViewById(R.id.bannerImage);
        promoIcon1 = (ImageView) header.findViewById(R.id.promoIcon1);
        promoIcon2 = (ImageView) header.findViewById(R.id.promoIcon2);
        promoIcon3 = (ImageView) header.findViewById(R.id.promoIcon3);
        promoMirror1 = (ImageView) header.findViewById(R.id.promoMirror1);
        promoMirror2 = (ImageView) header.findViewById(R.id.promoMirror2);
        promoMirror3 = (ImageView) header.findViewById(R.id.promoMirror3);
        txtPromoType = (TextView) header.findViewById(R.id.txtPromoType);
        txtBrowseCategory = (TextView) header.findViewById(R.id.txtBrowseCategory);
        txtSection = (TextView) header.findViewById(R.id.txtSection);
        list.addHeaderView(header, null, false);

        this.api = Api.getInstance(this);

        this.supportedAbis = this.api.getSupportedAbis();

        try {
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/storopia.ttf");
            txtMarket.setTypeface(tf);
            txtPromoType.setTypeface(tf);
        } catch (Exception e) { }

        adapter = new AppListAdapter(this, items);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int idx = position - list.getHeaderViewsCount();
                if (idx < 0 || idx >= items.size()) return;
                AppShort it = items.get(idx);
                Intent i = new Intent(MainActivity.this, AppDetailActivity.class);
                Log.d("MainActivity@134", String.valueOf(it.id));
                i.putExtra("app_id", it.id);
                startActivity(i);
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, SearchActivity.class)); }
        });
        btnApps.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openCategories(false); }
        });
        btnGames.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openCategories(true); }
        });
        btnDownloads.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openDownloads(); }
        });
        logo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try { openOptionsMenu(); } catch (Exception e) { }
            }
        });
        promoMainRoot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentPromoCategory == null) return;
                Intent i = new Intent(MainActivity.this, CategoryAppsActivity.class);
                i.putExtra("category", currentPromoCategory.code);
                i.putExtra("title", currentPromoCategory.label);
                i.putExtra("is_game", currentPromoCategory.isGame);
                startActivity(i);
            }
        });

        txtMarket.setText(isRu() ? "маркет" : "market");
        try {
            int androidLogoRes = getResources().getIdentifier("market_android_logo", "drawable", getPackageName());
            ImageView iw = (ImageView) findViewById(R.id.imgAndroidWord);
            if (iw != null && androidLogoRes != 0) iw.setImageResource(androidLogoRes);
        } catch (Exception e) { }
        if (!restoreFromCache()) {
            loadTopContent();
        }
        checkClientUpdateIfNeeded();
        sendAnalyticsIfNeeded();
        scheduleUpdateChecks();
    }

//    private boolean checkPermissions() {
//        String permission1 = Manifest.permission.WRITE_EXTERNAL_STORAGE;
//        String permission2 = Manifest.permission.READ_EXTERNAL_STORAGE;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
//        int status1 = ContextCompat.checkSelfPermission(this, permission1);
//        int status2 = ContextCompat.checkSelfPermission(this, permission2);
//        if (status1 == PackageManager.PERMISSION_DENIED ||
//            status2 == PackageManager.PERMISSION_DENIED) {
//            ActivityCompat.requestPermissions(this, new String[]{permission1, permission2}, 100);
//        }
//        return false;
//    }

    public String baseUrl() {
        String host = Prefs.getServer(this).trim();

        if (host.length() == 0) host = "pyt.pp.ua";

        while (host.endsWith("/")) host = host.substring(0, host.length() - 1);

        if (host.endsWith("/api")) host = host.substring(0, host.length() - 4);
        while (host.endsWith("/")) host = host.substring(0, host.length() - 1);

        if (host.startsWith("http://") || host.startsWith("https://")) {
            return host;
        }

        return "http://" + host;
    }

    public String getSupportedAbis() {
        return this.supportedAbis;
    }

    private boolean isRu() {
//        String lang = java.util.Locale.getDefault().getLanguage();
        return false; // lang != null && lang.toLowerCase().startsWith("ru");
    }

    private void scheduleUpdateChecks() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(this, UpdateCheckService.class);
            PendingIntent pi = PendingIntent.getService(this, 30001, i, PendingIntent.FLAG_UPDATE_CURRENT);
            long interval = 5L * 60L * 1000L;
            long first = System.currentTimeMillis() + 15000L;
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, interval, pi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onResume() {
        super.onResume();
        adapter.refreshInstalledPackages();
        adapter.notifyDataSetChanged();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem downloads = menu.add(0, 3, 0, getString(R.string.downloads));
        downloads.setIcon(R.drawable.ic_menu_downloads);
        MenuItem profile = menu.add(0, 1, 1, Prefs.isLoggedIn(this) ? getString(R.string.profile) : getString(R.string.login));
        profile.setIcon(R.drawable.ic_menu_account);
        MenuItem settings = menu.add(0, 2, 2, getString(R.string.settings));
        settings.setIcon(R.drawable.ic_menu_settings_custom);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new Intent(this, Prefs.isLoggedIn(this) ? ProfileActivity.class : LoginActivity.class));
            return true;
        }
        if (item.getItemId() == 2) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == 3) {
            openDownloads();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openCategories(boolean isGame) {
        Intent i = new Intent(this, CategoryListActivity.class);
        i.putExtra("is_game", isGame);
        startActivity(i);
    }

    private void openDownloads() { startActivity(new Intent(this, DownloadsActivity.class)); }

    private boolean restoreFromCache() {
        if (CACHE_ITEMS == null || CACHE_ITEMS.isEmpty()) return false;
        if (System.currentTimeMillis() - CACHE_TIME > 120000L) return false;
        items.clear();
        items.addAll(CACHE_ITEMS);
        adapter.refreshInstalledPackages();
        adapter.notifyDataSetChanged();
        if (CACHE_PROMO_SOURCE != null && !CACHE_PROMO_SOURCE.isEmpty()) bindPromo(CACHE_PROMO_SOURCE);
        showLoading(false);
        return true;
    }

    private void loadTopContent() {
        showLoading(true);
        final int deviceApi = Build.VERSION.SDK_INT;
        new AsyncTask<Void, Void, ArrayList<AppShort>>() {
            ArrayList<AppShort> promoSource = new ArrayList<>();
            protected ArrayList<AppShort> doInBackground(Void... v) {
                ArrayList<AppShort> out = new ArrayList<AppShort>();
//                loadEndpoint("/api/top-apps", false, deviceApi, out);
//                loadEndpoint("/api/top-games", true, deviceApi, out);
//                final ArrayList<String> banners = loadBanner();
//                if (!banners.isEmpty()) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            (new DownloadImageTask(bannerImage)).execute(
//                              Api.baseUrl(MainActivity.this) + "/html/banners/" + banners.get(0));
//                        }
//                    });
//                }

                ArrayList<AppShort> apps = MainActivity.this.api.getTopApps();
                if (apps != null)
                    Log.i("MainActivity", "App size::: "+String.valueOf(apps.size()));

                return (apps != null) ? apps : new ArrayList<AppShort>();
            }
            protected void onPostExecute(ArrayList<AppShort> out) {
                showLoading(false);
                if (out == null) {
                    Toast.makeText(MainActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    return;
                }
                items.clear();
                items.addAll(out);
                CACHE_ITEMS.clear();
                CACHE_ITEMS.addAll(out);
                CACHE_PROMO_SOURCE.clear();
                CACHE_PROMO_SOURCE.addAll(promoSource);
                CACHE_TIME = System.currentTimeMillis();
                adapter.refreshInstalledPackages();
                adapter.notifyDataSetChanged();
                bindPromo(promoSource);
            }
        }.execute();
    }

    private ArrayList<String> loadBanner() {
        ArrayList<String> banners = new ArrayList<String>();
        try {
            String s = Http.getString(this.api.getBaseUrl(this) + "/api/banners");
            if (s == null) return banners;
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject k = arr.getJSONObject(i);
                String bannerUrl = (String) k.get("image");
                banners.add(bannerUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return banners;
    }

    private void bindPromo(ArrayList<AppShort> source) {
        if (source == null || source.size() == 0) return;
        HashMap<String, PromoCategory> map = new HashMap<String, PromoCategory>();
        for (int i = 0; i < source.size(); i++) {
            AppShort a = source.get(i);
            if (a.categoryCode == null || a.categoryCode.length() == 0) continue;
            String key = a.categoryCode;
            PromoCategory pc = map.get(key);
            if (pc == null) {
                pc = new PromoCategory();
                pc.code = a.categoryCode;
                pc.label = a.categoryLabel;
                pc.isGame = false;
                map.put(key, pc);
            }
            if (pc.apps.size() < 6) pc.apps.add(a);
        }
        if (map.size() == 0) return;
        ArrayList<PromoCategory> cats = new ArrayList<PromoCategory>(map.values());
        currentPromoCategory = cats.get(new Random().nextInt(cats.size()));
        txtPromoType.setText(currentPromoCategory.isGame ? getString(R.string.games).toLowerCase() : getString(R.string.apps).toLowerCase());
        txtBrowseCategory.setText((isRu() ? "����� " : "Browse ") + currentPromoCategory.label);

        bindPromoIcon(promoIcon1, promoMirror1, currentPromoCategory.apps, 0);
        bindPromoIcon(promoIcon2, promoMirror2, currentPromoCategory.apps, 1);
        bindPromoIcon(promoIcon3, promoMirror3, currentPromoCategory.apps, 2);
    }

    private void bindPromoIcon(final ImageView iv, final ImageView mirror, ArrayList<AppShort> apps, int idx) {
        if (apps.size() <= idx) {
            iv.setImageResource(R.drawable.icon_placeholder);
            mirror.setImageResource(R.drawable.icon_placeholder);
            return;
        }
        AppShort a = apps.get(idx);
        final String url = a.icon;
        ImageLoader.load(this, url, iv, R.drawable.icon_placeholder);
        iv.postDelayed(new Runnable() {
            public void run() { updateMirrorFromImageView(iv, mirror, 0); }
        }, 250);
    }

    private void updateMirrorFromImageView(final ImageView source, final ImageView mirror, final int attempt) {
        try {
            Drawable d = source.getDrawable();
            if (d == null) {
                if (attempt < 8) {
                    source.postDelayed(new Runnable() {
                        public void run() { updateMirrorFromImageView(source, mirror, attempt + 1); }
                    }, 200);
                }
                return;
            }
            Bitmap original = drawableToBitmap(d, source.getWidth() > 0 ? source.getWidth() : 44, source.getHeight() > 0 ? source.getHeight() : 44);
            if (original == null) return;
            Bitmap reflected = createReflectionBitmap(original);
            if (reflected != null) mirror.setImageBitmap(reflected);
        } catch (Throwable e) { }
    }

    private Bitmap drawableToBitmap(Drawable drawable, int reqW, int reqH) {
        try {
            if (drawable instanceof BitmapDrawable) {
                Bitmap b = ((BitmapDrawable) drawable).getBitmap();
                if (b != null) return b;
            }
            int w = reqW > 0 ? reqW : 44;
            int h = reqH > 0 ? reqH : 44;
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable e) {
            return null;
        }
    }

    private Bitmap createReflectionBitmap(Bitmap original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            if (width <= 0 || height <= 1) return null;
            int reflectionHeight = Math.max(8, height / 2);
            Matrix matrix = new Matrix();
            matrix.preScale(1, -1);
            Bitmap reflection = Bitmap.createBitmap(original, 0, height - reflectionHeight, width, reflectionHeight, matrix, false);
            Bitmap bitmapWithReflection = Bitmap.createBitmap(width, reflectionHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapWithReflection);
            canvas.drawBitmap(reflection, 0, 0, null);
            Paint paint = new Paint();
            LinearGradient shader = new LinearGradient(0, 0, 0, reflectionHeight, 0x66ffffff, 0x00ffffff, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawRect(0, 0, width, reflectionHeight, paint);
            return bitmapWithReflection;
        } catch (Throwable e) {
            return null;
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    // TODO
    private void checkClientUpdateIfNeeded() {
        if (true) return;
        final boolean ru = isRu();
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    String s = Http.getString("");
                    if (s == null || s.length() == 0) return null;
                    return new JSONObject(s);
                } catch (Exception e) { return null; }
            }
            protected void onPostExecute(JSONObject o) {
                if (o == null) return;
                try {
                    int latestCode = o.optInt("version_code", 0);
                    String latestName = o.optString("version_name", "");
                    String updateUrl = o.optString("update_url", "");
                    String notes = ru ? o.optString("notes_ru", "") : o.optString("notes_en", "");
                    PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                    if (latestCode > pi.versionCode && updateUrl != null && updateUrl.length() > 0) {
                        StringBuilder msg = new StringBuilder(getString(R.string.client_update_message));
                        if (latestName != null && latestName.length() > 0) {
                            msg.append("\\n\\n").append(R.string.version).append(latestName);
                        }
                        if (notes != null && notes.length() > 0) {
                            msg.append("\\n\\n").append(getString(R.string.client_update_note_prefix)).append(notes);
                        }
                        final String finalUrl = updateUrl;
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.client_update_title))
                                .setMessage(msg.toString())
                                .setPositiveButton(getString(R.string.update_now), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)));
                                        } catch (Exception e) { }
                                    }
                                })
                                .setNegativeButton(getString(R.string.later), null)
                                .show();
                    }
                } catch (Exception e) { }
            }
        }.execute();
    }

    // TODO
    private void sendAnalyticsIfNeeded() {
        if (true) return;
        long now = System.currentTimeMillis();
        if (now - Prefs.getLastAnalyticsSentAt(this) < 12L * 60L * 60L * 1000L) return;
        Prefs.setLastAnalyticsSentAt(this, now);
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                    JSONObject o = new JSONObject();
                    o.put("api_level", Build.VERSION.SDK_INT);
                    o.put("app_version_code", pi.versionCode);
                    o.put("app_version_name", pi.versionName == null ? "" : pi.versionName);
                    o.put("device_model", Build.MODEL == null ? "" : Build.MODEL);
                    o.put("manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
                    o.put("lang", java.util.Locale.getDefault().getLanguage());
                    Http.postJson("", o.toString());
                } catch (Exception e) { }
                return null;
            }
        }.execute();
    }

    private void showApi25WarningIfNeeded() {
        if (Build.VERSION.SDK_INT == 25) {
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("OldMarket can work badly on newer devices.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) { }
                    })
                    .show();
        }
    }
}
