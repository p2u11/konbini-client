package io.github.konbini.market.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.konbini.market.api.*;

import io.github.konbini.market.R;
import io.github.konbini.market.net.Http;
import io.github.konbini.market.service.DownloadService;
import io.github.konbini.market.util.AndroidVersions;
import io.github.konbini.market.util.ImageLoader;
import io.github.konbini.market.util.LocaleHelper;
import io.github.konbini.market.util.Prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.GONE;

public class AppDetailActivity extends Activity {
    private int appId;

    private View header;
    private ImageView imgIcon;
    private TextView txtName, txtAuthor, txtMeta, txtDesc, txtToggle;
    private ImageView imgAndroidHeaderLogo;
    private TextView txtDownloadsInfo, txtReviewsInfo, txtHeaderRating, txtreviewinfo;
    private RatingBar ratingHeader, ratingAddReview;
    private Button btnInstall, btnOpen, btnUninstall, btnCancelDownload;
    private TextView txtScreensTitle, txtReviewsTitle, txtDownloadProgress;
    private HorizontalScrollView screensScroll;
    private LinearLayout screensContainer, downloadPanel, installButtons;
    private ProgressBar progressDownload;

    private LinearLayout detailsContainer, reviewsTabContainer;
    private View detailsScrollView, reviewsScrollView;
    private ListView list;
    private ListView listVersions;
    private Button btnTabDetails, btnTabVersions, btnTabReviews;
    private ArrayList<ReviewItem> reviews = new ArrayList<ReviewItem>();
    private ReviewAdapter adapter;
    private int activeTab = 0;

    private String pkgName = "";
    private String selectedVersion = "";
    private int currentMinApi = 1;
    private boolean hasOwnReview = false;
    private String currentIconFile = "";

    private View loadingOverlay;
    private TextView txtLoading;

    private App app;
    private Boolean appInitialized = false;

    private boolean descCollapsed = true;

    private Api api;

    private final BroadcastReceiver dlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("app_id", -1);
            if (id != appId) return;

            int percent = intent.getIntExtra("percent", 0);
            long speed = intent.getLongExtra("speed_bps", 0);
            boolean done = intent.getBooleanExtra("done", false);
            boolean error = intent.getBooleanExtra("error", false);
            boolean cancelled = intent.getBooleanExtra("cancelled", false);
            boolean active = intent.getBooleanExtra("active", false);

            if (active) showDownloadUi(percent, speed);
            if (done) {
                hideDownloadUi();
                String path = intent.getStringExtra("file_path");
                if (path != null && hasWindowFocus()) openInstaller(path);
            }
            if (error || cancelled) hideDownloadUi();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_app_detail);

        appId = getIntent().getIntExtra("app_id", 0);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        txtLoading = (TextView) findViewById(R.id.txtLoading);
        detailsContainer = (LinearLayout) findViewById(R.id.detailsContainer);
        reviewsTabContainer = (LinearLayout) findViewById(R.id.reviewsTabContainer);
        detailsScrollView = findViewById(R.id.detailsScrollView);
        reviewsScrollView = findViewById(R.id.reviewsScrollView);
        list = (ListView) findViewById(R.id.listReviews);
        listVersions = (ListView) findViewById(R.id.listVersions);
        btnTabDetails = (Button) findViewById(R.id.btnTabDetails);
        btnTabVersions = (Button) findViewById(R.id.btnTabVersions);
        btnTabReviews = (Button) findViewById(R.id.btnTabReviews);

        header = LayoutInflater.from(this).inflate(R.layout.app_detail_header, detailsContainer, false);
        detailsContainer.addView(header);

        imgIcon = (ImageView) findViewById(R.id.imgIcon);
        txtName = (TextView) findViewById(R.id.txtName);
        txtAuthor = (TextView) findViewById(R.id.txtAuthor);
        txtHeaderRating = (TextView) findViewById(R.id.txtHeaderRating);
        ratingHeader = (RatingBar) findViewById(R.id.ratingHeader);

        txtDownloadsInfo = (TextView) header.findViewById(R.id.txtDownloadsInfo);
        txtReviewsInfo = (TextView) header.findViewById(R.id.txtReviewsInfo);
        txtMeta = (TextView) header.findViewById(R.id.txtMeta);
        txtDesc = (TextView) header.findViewById(R.id.txtDesc);
        txtToggle = (TextView) header.findViewById(R.id.toggleDescriptionTextView);
        txtScreensTitle = (TextView) header.findViewById(R.id.txtScreensTitle);
        screensScroll = (HorizontalScrollView) header.findViewById(R.id.screensScroll);
        screensContainer = (LinearLayout) header.findViewById(R.id.screensContainer);
        txtReviewsTitle = (TextView) findViewById(R.id.txtReviewsTitle);
        ratingAddReview = (RatingBar) findViewById(R.id.ratingAddReview);
        txtreviewinfo = (TextView) findViewById(R.id.txtreviewinfo);

        btnInstall = (Button) findViewById(R.id.btnInstall);
        btnOpen = (Button) findViewById(R.id.btnOpen);
        btnUninstall = (Button) findViewById(R.id.btnUninstall);
        btnCancelDownload = (Button) findViewById(R.id.btnCancelDownload);
        txtDownloadProgress = (TextView) findViewById(R.id.txtDownloadProgress);
        progressDownload = (ProgressBar) findViewById(R.id.progressDownload);
        downloadPanel = (LinearLayout) findViewById(R.id.downloadPanel);
        installButtons = (LinearLayout) findViewById(R.id.installButtons);

        adapter = new ReviewAdapter();
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= reviews.size()) return;
                showReviewActionsDialog(reviews.get(position));
            }
        });

        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppVersion target = getLatestSupportedVersion();
                if (target != null) {
                    startDownload(target.id);
                } else {
                    chooseVersionAndDownload();
                }
            }
        });
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openApp(); }
        });
        btnUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { uninstallApp(); }
        });
        btnCancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AppDetailActivity.this, DownloadService.class);
                i.setAction(DownloadService.ACTION_CANCEL);
                i.putExtra("app_id", appId);
                startService(i);
            }
        });

        btnTabDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showTab(0); }
        });
        btnTabVersions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showTab(1); }
        });
        btnTabReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showTab(2); }
        });
        listVersions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppVersion version = ((AppVersionAdapter) parent.getAdapter()).getItem(position);
                if (version != null) startDownload(version.id);
            }
        });

        ratingAddReview.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                if (event.getAction() != MotionEvent.ACTION_UP) return true;
                if (!Prefs.isLoggedIn(AppDetailActivity.this)) {
                    startActivity(new Intent(AppDetailActivity.this, LoginActivity.class));
                    return true;
                }
                if (hasOwnReview) return true;
                RatingBar rb = (RatingBar) v;
                float stars = rb.getNumStars() * event.getX() / Math.max(1f, rb.getWidth());
                int rating = (int) Math.ceil(stars);
                if (rating < 1) rating = 1;
                if (rating > 5) rating = 5;
                rb.setRating(rating);
                showAddReviewDialog(rating);
                rb.setRating(0f);
                return true;
            }
        });
//                (View v, MotionEvent event) -> {
//            v.performClick();
//            if (event.getAction() != MotionEvent.ACTION_UP) return true;
//            if (!Prefs.isLoggedIn(AppDetailActivity.this)) {
//                startActivity(new Intent(AppDetailActivity.this, LoginActivity.class));
//                return true;
//            }
//            if (hasOwnReview) return true;
//            RatingBar rb = (RatingBar) v;
//            float stars = rb.getNumStars() * event.getX() / Math.max(1f, rb.getWidth());
//            int rating = (int) Math.ceil(stars);
//            if (rating < 1) rating = 1;
//            if (rating > 5) rating = 5;
//            rb.setRating(rating);
//            showAddReviewDialog(rating);
//            rb.setRating(0f);
//            return true;
//        });

        api = Api.getInstance(AppDetailActivity.this);

        try {
            int androidLogoRes = getResources().getIdentifier("market_android_logo", "drawable", getPackageName());
            if (imgAndroidHeaderLogo != null && androidLogoRes != 0) imgAndroidHeaderLogo.setImageResource(androidLogoRes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        showLoading(true, getString(R.string.loading));
        showTab(0);
        loadDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("AppDetailActivity", "onResume is called!");
        registerReceiver(dlReceiver, new IntentFilter(DownloadService.ACTION_PROGRESS));
        restoreDownloadState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(dlReceiver); } catch (Exception e) { e.printStackTrace(); }
    }

    private SharedPreferences downloadPrefs() {
        return getSharedPreferences("download_state", MODE_PRIVATE);
    }

    private void restoreDownloadState() {
        SharedPreferences p = downloadPrefs();
        if (p.getBoolean("active", false) && p.getInt("app_id", -1) == appId) {
            showDownloadUi(p.getInt("percent", 0), p.getLong("speed_bps", 0));
        } else {
            hideDownloadUi();
        }
    }

    private void showDownloadUi(int percent, long speed) {
        downloadPanel.setVisibility(View.VISIBLE);
        installButtons.setVisibility(GONE);
        progressDownload.setProgress(percent);
        String speedText = speed >= 1024 * 1024
                ? String.format(Locale.US, "%.1f MB/s", speed / 1024f / 1024f)
                : Math.max(1, speed / 1024) + " KB/s";
        txtDownloadProgress.setText(percent + "%  •  " + speedText);
    }

    private void hideDownloadUi() {
        Log.d("AppDetailActivity", "hideDownloadUi is called!");
        downloadPanel.setVisibility(GONE);
        installButtons.setVisibility(View.VISIBLE);
        progressDownload.setProgress(0);
        txtDownloadProgress.setText("0%");
        Log.i("AppDetailActivity", "App is initialized: "+(this.appInitialized ? "true" : "false"));
        refreshInstalledButtons(this.app);
    }

    private void showTab(int tab) {
        activeTab = tab;
        detailsScrollView.setVisibility(tab == 0 ? View.VISIBLE : GONE);
        listVersions.setVisibility(tab == 1 ? View.VISIBLE : GONE);
        reviewsScrollView.setVisibility(tab == 2 ? View.VISIBLE : GONE);

        btnTabDetails.setSelected(tab == 0);
        btnTabVersions.setSelected(tab == 1);
        btnTabReviews.setSelected(tab == 2);
        btnTabDetails.setEnabled(true);
        btnTabVersions.setEnabled(true);
        btnTabReviews.setEnabled(true);
    }

    private void bindVersionsTab() {
        if (app == null || app.versions == null || app.versions.size() == 0) {
            listVersions.setAdapter(null);
            return;
        }
        AppVersionAdapter versionsAdapter = new AppVersionAdapter(this, app.versions);
        listVersions.setAdapter(versionsAdapter);
    }

    private AppVersion getLatestSupportedVersion() {
        if (app == null || app.versions == null || app.versions.size() == 0) return null;

        AppVersion latest = null;
        for (int i = 0; i < app.versions.size(); i++) {
            AppVersion version = app.versions.valueAt(i);
            if (version == null || !version.isSupported()) continue;
            if (latest == null || version.versionCode > latest.versionCode) {
                latest = version;
            }
        }
        return latest;
    }

    private void loadDetails() {
        try { int androidLogoRes = getResources().getIdentifier("market_android_logo", "drawable", getPackageName()); if (imgAndroidHeaderLogo != null && androidLogoRes != 0) imgAndroidHeaderLogo.setImageResource(androidLogoRes); } catch (Exception e) { }
        showLoading(true, getString(R.string.loading));
        new AsyncTask<Void, Void, App>() {
            @Override
            protected App doInBackground(Void... v) {
                Logger logger = Logger.getLogger(AppDetailActivity.this.getPackageName());
                try {
                    return api.getApp(appId);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e.getMessage());
                    appInitialized = true;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(App o) {
                showLoading(false, null);
                if (o == null) {
                    msg(getString(R.string.error_network));
                    return;
                }

                AppDetailActivity.this.app = o;
                String name = o.name;
                final String dev = o.author;
                final String desc = o.description;
                final String shortDesc = desc.substring(0, 100) + "...";
                String icon = o.icon;
                currentIconFile = icon;

                AppVersion minver = app.getFirstVersion();
                AppVersion maxver = app.getLastVersion();
                int downloads = 0;
                int reviewCount = 0;
                float avgRating = 0;
                pkgName = o.packageId;

                txtName.setText(name);
                txtAuthor.setText(dev);
                AppVersion firstVersion = app.getFirstVersion();
                AppVersion lastVersion = app.getLastVersion();
                String compatibility = "";
                if (firstVersion != null) {
                    String range = firstVersion.versionName;
                    if (lastVersion != null && lastVersion.versionName != null && !lastVersion.versionName.equals(firstVersion.versionName)) {
                        range = firstVersion.versionName + " – " + lastVersion.versionName;
                    }
                    compatibility += getString(R.string.version) + " " + range;
                }
                if (firstVersion != null) {
                    compatibility += " • Android " + AndroidVersions.apiToAndroid(firstVersion.minSdk) + " (API " + firstVersion.minSdk + ")";
                }
                compatibility += app.isSupported() ? " • Compatible" : " • Not compatible";
                txtMeta.setText(compatibility);
                txtMeta.setVisibility(View.VISIBLE);
                //txtAuthor.setCompoundDrawables(null, null, AppDetailActivity.this.getResources().getDrawable(R.drawable.check), null);
                txtAuthor.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AppDetailActivity.this, CategoryAppsActivity.class);
                        intent.putExtra("type", "author");
                        intent.putExtra("query", dev);
                        intent.putExtra("title", "by "+dev);
                        startActivity(intent);
                    }
                });
                if (desc.length() > 100) {
                    txtDesc.setText(shortDesc);
                    txtToggle.setVisibility(View.VISIBLE);
                    txtToggle.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            descCollapsed = !descCollapsed;
                            txtToggle.setText(descCollapsed ? R.string.expand_desc : R.string.collapse_desc);
                            txtDesc.setText(descCollapsed ? shortDesc : desc);
                        }
                    });
                } else {
                    txtDesc.setText(desc);
                }
                txtDownloadsInfo.setText(downloads + " " + getString(R.string.downloads_count));
                txtReviewsInfo.setText(reviewCount + " " + getString(R.string.reviews_count));
                txtHeaderRating.setText(String.format(Locale.US, "%.1f", avgRating));
                ratingHeader.setRating(avgRating);
                txtReviewsTitle.setText(getString(R.string.reviews) + " (" + reviewCount + ")");

                if (icon != null && icon.length() > 0) {
                    ImageLoader.load(AppDetailActivity.this, icon, imgIcon, R.drawable.icon_placeholder);
                } else {
                    imgIcon.setImageResource(R.drawable.icon_placeholder);
                }

                refreshInstalledButtons(AppDetailActivity.this.app);
                restoreDownloadState();
                bindVersionsTab();

                loadScreenshots();
                loadReviews();
            }
        }.execute();
    }

    private void loadScreenshots() {
        new AsyncTask<Void, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(Void... v) {
                AppDetailActivity activity = AppDetailActivity.this;
                if (activity.isFinishing()) {
                    Log.e("AppDetailActivity", "activity is finishing!!");
                    return null;
                }

                if (activity.app != null) {
                    if (activity.app.screenshots != null) {
                        return activity.app.screenshots;
                    } else {
                        Log.e("AppDetailActivity", "app.screenshots is null!!");
                    }
                } else {
                    Log.e("AppDetailActivity", "app is null!!");
                }

                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<String> arr) {
                if (arr == null || arr.size() == 0) {
                    txtScreensTitle.setVisibility(GONE);
                    screensScroll.setVisibility(GONE);
                    return;
                }
                txtScreensTitle.setVisibility(View.VISIBLE);
                screensScroll.setVisibility(View.VISIBLE);
                screensContainer.removeAllViews();
                for (int i = 0; i < arr.size(); i++) {
                    String file = arr.get(i);
                    if (file == null || file.length() == 0) continue;
                    ImageView iv = new ImageView(AppDetailActivity.this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(240, 400);
                    lp.rightMargin = 10;
                    iv.setLayoutParams(lp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    screensContainer.addView(iv);
                    ImageLoader.load(AppDetailActivity.this, file, iv, R.drawable.banner_placeholder);
                }
            }
        }.execute();
    }

    private void loadReviews() {
        new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... v) {
                try {
                    int viewerId = Prefs.getUserId(AppDetailActivity.this);
                    // TODO
                    String s = null;
                    if (s == null) return "null response";
                    JSONArray arr = new JSONArray(s);
                    ArrayList<ReviewItem> out = new ArrayList<ReviewItem>();
                    for (int i = 0; i < arr.length(); i++) {
                        out.add(parseReview(arr.getJSONObject(i)));
                    }
                    return out;
                } catch (Exception e) {
                    return e.toString();
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void onPostExecute(Object out) {
                if (out instanceof String) {
                    txtReviewsTitle.setText(getString(R.string.reviews) + " (0)");
                    txtReviewsInfo.setText("0 " + getString(R.string.reviews_count));
                    hasOwnReview = false;
                    ratingAddReview.setVisibility(View.VISIBLE);
                    txtreviewinfo.setVisibility(View.VISIBLE);
                    return;
                }

                ArrayList<ReviewItem> listOut = (ArrayList<ReviewItem>) out;
                reviews.clear();
                reviews.addAll(listOut);
                hasOwnReview = false;
                int myId = Prefs.getUserId(AppDetailActivity.this);
                for (int i = 0; i < reviews.size(); i++) {
                    if (reviews.get(i).userId == myId && myId > 0) {
                        hasOwnReview = true;
                        break;
                    }
                }

                txtReviewsTitle.setText(getString(R.string.reviews) + " (" + reviews.size() + ")");
                txtReviewsInfo.setText(reviews.size() + " " + getString(R.string.reviews_count));
                ratingAddReview.setVisibility(hasOwnReview ? GONE : View.VISIBLE);
                txtreviewinfo.setVisibility(hasOwnReview ? GONE : View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        }.execute();
    }

    private ReviewItem parseReview(JSONObject r) {
        ReviewItem ri = new ReviewItem();
        ri.id = r.optInt("id", 0);
        ri.userId = r.optInt("user_id", 0);
        ri.username = r.optString("username", "User");
        ri.avatar = r.optString("avatar", "default_avatar.png");
        ri.rating = r.optInt("rating", 0);
        ri.text = r.optString("comment", r.optString("text", ""));
        ri.createdAt = r.optString("created_at", "");
        ri.likes = r.optInt("likes", 0);
        ri.dislikes = r.optInt("dislikes", 0);
        ri.commentsCount = r.optInt("comments_count", 0);
        ri.userReaction = r.optInt("user_reaction", 0);
        return ri;
    }

    private void showAddReviewDialog(final int presetRating) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (10 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView lbl = new TextView(this);
        lbl.setText(String.format(getString(R.string.rating), presetRating));
        layout.addView(lbl);

        final EditText edt = new EditText(this);
        edt.setHint(getString(R.string.your_review));
        edt.setMinLines(3);
        layout.addView(edt);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.post_review))
                .setView(layout)
                .setPositiveButton(getString(R.string.send_review), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = edt.getText().toString().trim();
                        sendReview(text, presetRating);
                    }
                })
                .setNegativeButton(getString(R.string.cancel_review), null)
                .show();
    }

    private void sendReview(final String text, final int rating) {
        final int uid = Prefs.getUserId(this);
        final String token = Prefs.getAuthKey(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        final int safeRating = Math.max(1, Math.min(5, rating));
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                try {
                    // TODO
                    if (true) return null;
                    String url = ""; //Api.baseUrl(AppDetailActivity.this) + "/api/app/" + appId + "/review";
                    JSONObject o = new JSONObject();
                    o.put("token", token);
                    o.put("user_id", uid);
                    o.put("rating", safeRating);
                    o.put("comment", text);
                    return Http.postJson(AppDetailActivity.this, url, o.toString());
                } catch (Exception e) {
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String s) {
                if (s == null) {
                    msg("Network error");
                    return;
                }
                loadReviews();
                Toast.makeText(AppDetailActivity.this, isRu() ? "Отправлено" : "Sent", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void showCommentsDialog(final int reviewId) {
        new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... v) {
                try {
                    // TODO
                    String s = null; //Http.getString(Api.reviewCommentsUrl(AppDetailActivity.this, reviewId));
                    if (s == null) return "null response";
                    return new JSONArray(s);
                } catch (Exception e) {
                    return e.toString();
                }
            }
            @Override
            protected void onPostExecute(Object out) {
                if (out instanceof String) {
                    msg("Comments error: " + out);
                    return;
                }
                JSONArray arr = (JSONArray) out;
                final String[] items = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.optJSONObject(i);
                    if (c == null) {
                        items[i] = String.valueOf(arr.opt(i));
                    } else {
                        String u = c.optString("username", "User");
                        String t = c.optString("text", "");
                        String d = c.optString("created_at", "");
                        items[i] = u + ": " + t + (d.length() > 0 ? ("  (" + d + ")") : "");
                    }
                }
                new AlertDialog.Builder(AppDetailActivity.this)
                        .setTitle(isRu() ? "Комментарии" : "Comments")
                        .setItems(items, null)
                        .setPositiveButton(isRu() ? "Добавить" : "Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!Prefs.isLoggedIn(AppDetailActivity.this)) {
                                    startActivity(new Intent(AppDetailActivity.this, LoginActivity.class));
                                    return;
                                }
                                showAddCommentDialog(reviewId);
                            }
                        })
                        .setNegativeButton(isRu() ? "Закрыть" : "Close", null)
                        .show();
            }
        }.execute();
    }

    private void showAddCommentDialog(final int reviewId) {
        final EditText edt = new EditText(this);
        edt.setHint(isRu() ? "Комментарий" : "Comment");
        new AlertDialog.Builder(this)
                .setTitle(isRu() ? "Добавить комментарий" : "Add comment")
                .setView(edt)
                .setPositiveButton(isRu() ? "Отправить" : "Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String text = edt.getText().toString().trim();
                        if (text.length() == 0) return;
                        addReviewComment(reviewId, text);
                    }
                })
                .setNegativeButton(isRu() ? "Отмена" : "Cancel", null)
                .show();
    }

    private void addReviewComment(final int reviewId, final String text) {
        final int uid = Prefs.getUserId(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("user_id", uid);
                    o.put("text", text);
                    // TODO
                    return null; //Http.postJson(AppDetailActivity.this, Api.reviewAddCommentUrl(AppDetailActivity.this, reviewId), o.toString());
                } catch (Exception e) {
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String s) {
                if (s == null) {
                    msg("Network error");
                    return;
                }
                loadReviews();
            }
        }.execute();
    }

    private void sendReaction(final int reviewId, final int value) {
        final int uid = Prefs.getUserId(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("user_id", uid);
                    o.put("value", value);
                    // TODO
                    return null; //Http.postJson(AppDetailActivity.this, Api.reviewReactionUrl(AppDetailActivity.this, reviewId), o.toString());
                } catch (Exception e) {
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String s) {
                if (s == null) {
                    msg("Network error");
                    return;
                }
                loadReviews();
            }
        }.execute();
    }

    private void reportReview(final int reviewId) {
        final int uid = Prefs.getUserId(this);
        if (uid <= 0) {
            msg("Login required");
            return;
        }
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("user_id", uid);
                    // TODO
                    return null;
//                    return Http.postJson(AppDetailActivity.this, Api.reviewReportUrl(AppDetailActivity.this, reviewId), o.toString());
                } catch (Exception e) {
                    return null;
                }
            }
            @Override
            protected void onPostExecute(String s) {
                if (s == null) {
                    msg("Network error");
                    return;
                }
                msg(isRu() ? "Отправлено" : "Reported");
            }
        }.execute();
    }

    private void chooseVersionAndDownload() {
        if (!btnInstall.isEnabled()) return;
        showLoading(true, getString(R.string.loading_versions));
        new AsyncTask<Void, Void, SparseArray<AppVersion>>() {
            @Override
            protected SparseArray<AppVersion> doInBackground(Void... v) {
                try {
                    return app.versions;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            @Override
            protected void onPostExecute(SparseArray<AppVersion> out) {
                showLoading(false, null);
                if (out == null) {
                    Toast.makeText(AppDetailActivity.this, "Failed to parse version list.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Logger.getLogger(AppDetailActivity.this.getPackageName()).log(Level.INFO, out.getClass().getName());
                Logger.getLogger(AppDetailActivity.this.getPackageName()).log(Level.INFO, out.toString());
                Logger.getLogger(AppDetailActivity.this.getPackageName()).log(Level.INFO, out.getClass().getName());
                Logger.getLogger(AppDetailActivity.this.getPackageName()).log(Level.INFO, out.toString());
                Logger.getLogger(AppDetailActivity.this.getPackageName()).log(Level.INFO, String.valueOf(out.size()));
                if (out.size() == 0) {
                    Toast.makeText(AppDetailActivity.this, "Failed to parse version list.", Toast.LENGTH_SHORT).show();
                    return;
                }

//                final ArrayList<String> versList = new ArrayList<>();
//                final ArrayList<Integer> versValue = new ArrayList<>();
//                for (int i = 0; i < out.size(); i++) {
//                    AppVersion version = out.valueAt(i);
//
//                    if (!version.isSupported()) continue;
//
//
//                    String label = String.format(Locale.ENGLISH,
//                            "%s (%d) - Android %s (API %d) - %s", version.versionName,
//                            version.versionCode, AndroidVersions.apiToAndroid(version.minSdk),
//                            version.minSdk, abisList);
//
//                    versList.add(label);
//                    versValue.add(version.id);
//                }
//                if (versList.size() == 0) {
//                    Toast.makeText(AppDetailActivity.this, "Failed to parse version list.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                AppVersionAdapter adapter = new AppVersionAdapter(AppDetailActivity.this, app.versions);

                new AlertDialog.Builder(AppDetailActivity.this)
                        .setTitle(isRu() ? "Выберите версию" : "Select version")
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startDownload(app.versions.keyAt(which));
                            }
                        })
                        .setNegativeButton(isRu() ? "Отмена" : "Cancel", null)
                        .show();

            }
        }.execute();
    }

    private void startDownload(final int version_id) {
        final int uid = Prefs.getUserId(this);
        final String url;

        // Create a background thread to handle the network operation
        new Thread(new Runnable() {
            @Override
            public void run() {
            String url = null;
            try {
                Log.d("AppDetailActivity", "Versions found: "+String.valueOf(app.versions.size()));
                Log.d("AppDetailActivity", "Version requested: "+String.valueOf(version_id));
                for (int i = 0; i < app.versions.size(); i++) {
                    Log.d("AppDetailActivity", "Version found: "+app.versions.keyAt(i));
                }
                AppVersion version = app.versions.get(version_id, null);

                if (version == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AppDetailActivity.this, "Failed to get the app.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                final String finalUrl = version.downloadUrl;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(AppDetailActivity.this, DownloadService.class);
                        i.setAction(DownloadService.ACTION_START);
                        i.putExtra("app_id", appId);
                        i.putExtra("app_name", txtName == null ? "" : String.valueOf(txtName.getText()));
                        i.putExtra("icon", currentIconFile == null ? "" : currentIconFile);
                        i.putExtra("url", finalUrl);
                        i.putExtra("file_name", "konbini_" + appId + (selectedVersion.length() > 0 ? ("_" + selectedVersion) : "") + ".apk");
                        startService(i);
                        showDownloadUi(0, 0);
                    }
                });

            } catch (Exception e) {
                Log.e("AppDetailActivity", "Error fetching download URL", e);
            }
            }
        }).start();
    }

    private void refreshInstalledButtons(App app) {
        String pkgName = (app == null) ? "" : app.packageId;
        boolean installed = pkgName != null && pkgName.length() > 0 && isInstalled(pkgName);
        if (app == null || !app.isSupported()) {
            btnInstall.setVisibility(View.VISIBLE);
            btnOpen.setVisibility(GONE);
            btnUninstall.setVisibility(GONE);
            btnInstall.setEnabled(false);
            btnInstall.setText(getString(R.string.not_compatible));
            return;
        }

        if (installed) {
            btnInstall.setVisibility(View.VISIBLE);
            btnOpen.setVisibility(View.VISIBLE);
            btnUninstall.setVisibility(View.VISIBLE);
            btnInstall.setText(isRu() ? "Скачать" : "Download");
            btnOpen.setText(getString(R.string.open));
            btnUninstall.setText(getString(R.string.uninstall));
            btnInstall.setEnabled(true);
        } else {
            btnInstall.setVisibility(View.VISIBLE);
            btnOpen.setVisibility(GONE);
            btnUninstall.setVisibility(GONE);
            btnInstall.setText(getString(R.string.install));
            btnInstall.setEnabled(true);
        }
    }

    private boolean isInstalled(String packageName) {
        if (packageName == null || packageName.length() == 0) return false;
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openApp() {
        if (pkgName == null || pkgName.length() == 0) return;
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(pkgName);
        if (launch != null) startActivity(launch);
    }

    private void uninstallApp() {
        if (pkgName == null || pkgName.length() == 0) return;
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + pkgName));
        startActivity(intent);
    }

    private void openInstaller(String path) {
        try {
            File f = new File(path);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) { }
    }

    private void msg(String s) {
        try {
            new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK", null).show();
        } catch (Exception e) { }
    }

    private void showLoading(boolean show, String text) {
        if (txtLoading != null && text != null) txtLoading.setText(text);
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : GONE);
    }

    private boolean isRu() {
        try {
            String lang = Locale.getDefault().getLanguage();
            return lang != null && lang.toLowerCase().startsWith("ru");
        } catch (Exception e) {
            return false;
        }
    }

    private void showReviewActionsDialog(final ReviewItem r) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (10 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        Button btnProfile = new Button(this);
        btnProfile.setText((isRu() ? "Профиль" : "Profile") + (r.userId > 0 ? "" : ""));
        layout.addView(btnProfile);

        final Button btnLike = new Button(this);
        btnLike.setText((isRu() ? "Лайк" : "Like") + " (" + r.likes + ")");
        layout.addView(btnLike);

        final Button btnDislike = new Button(this);
        btnDislike.setText((isRu() ? "Дизлайк" : "Dislike") + " (" + r.dislikes + ")");
        layout.addView(btnDislike);

        Button btnComments = new Button(this);
        btnComments.setText(getString(R.string.comments, r.commentsCount));
        layout.addView(btnComments);

        Button btnReport = new Button(this);
        btnReport.setText(R.string.report);
        layout.addView(btnReport);

        boolean logged = Prefs.isLoggedIn(this);
        if (!logged) {
            btnLike.setEnabled(false);
            btnDislike.setEnabled(false);
            btnReport.setEnabled(false);
        } else {
            if (r.userReaction == 1) btnLike.setEnabled(false);
            if (r.userReaction == -1) btnDislike.setEnabled(false);
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(r.username)
                .setView(layout)
                .setNegativeButton(isRu() ? "Закрыть" : "Close", null)
                .create();

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (r.userId > 0) {
                    Intent i = new Intent(AppDetailActivity.this, UserProfileActivity.class);
                    i.putExtra("user_id", r.userId);
                    startActivity(i);
                }
            }
        });
        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                sendReaction(r.id, 1);
            }
        });
        btnDislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                sendReaction(r.id, -1);
            }
        });
        btnComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showCommentsDialog(r.id);
            }
        });
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                reportReview(r.id);
            }
        });

        dialog.show();
    }

    private static class ReviewItem {
        int id;
        int userId;
        String username = "User";
        String avatar = "default_avatar.png";
        int rating = 0;
        String text = "";
        String createdAt = "";
        int likes = 0;
        int dislikes = 0;
        int commentsCount = 0;
        int userReaction = 0;
    }

    private class ReviewAdapter extends BaseAdapter {
        @Override
        public int getCount() { return reviews.size(); }
        @Override
        public Object getItem(int position) { return reviews.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AppDetailActivity.this).inflate(R.layout.list_item_review, parent, false);
            }
            ReviewItem r = reviews.get(position);
            ImageView imgUser = (ImageView) convertView.findViewById(R.id.imgUser);
            TextView txtUser = (TextView) convertView.findViewById(R.id.txtUser);
            TextView txtDate = (TextView) convertView.findViewById(R.id.txtDate);
            TextView txtMetaLocal = (TextView) convertView.findViewById(R.id.txtMeta);
            TextView txtText = (TextView) convertView.findViewById(R.id.txtText);
            RatingBar rb = (RatingBar) convertView.findViewById(R.id.ratingBarReview);

            txtUser.setText(r.username);
            txtDate.setText(r.createdAt);
            txtMetaLocal.setText("");
            txtText.setText(r.text);
            rb.setRating(r.rating);
            //TODO
            ImageLoader.load(AppDetailActivity.this, "" /*Api.avatarUrl(AppDetailActivity.this, r.avatar)*/, imgUser, R.drawable.icon_placeholder);
            return convertView;
        }
    }
}
