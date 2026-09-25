package io.github.konbini.market.ui;

import java.util.ArrayList;
import java.util.List;

import io.github.konbini.market.R;
import io.github.konbini.market.api.AppShort;
import io.github.konbini.market.api.Api;
import io.github.konbini.market.util.LocaleHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity {
    private EditText edt;
    private ImageButton btn;
    private ListView list;
    private View loadingOverlay;
    private ArrayList<AppShort> data = new ArrayList<>();
    private AppListAdapter adapter;
    private SearchTask searchTask;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LocaleHelper.applySavedLocale(this);
        setContentView(R.layout.activity_search);

        edt = (EditText) findViewById(R.id.edtQuery);
        btn = (ImageButton) findViewById(R.id.btnDoSearch);
        list = (ListView) findViewById(R.id.list);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        adapter = new AppListAdapter(this, data);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppShort it = data.get(position);
                Intent i = new Intent(SearchActivity.this, AppDetailActivity.class);
                i.putExtra("app_id", it.id);
                startActivity(i);
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doSearch(); }
        });

        edt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });
    }

    private void doSearch() {
        final String q = edt.getText().toString().trim();
        if (q.length() == 0) return;
        if (searchTask != null) searchTask.cancel(true);
        searchTask = new SearchTask(q);
        searchTask.execute();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private class SearchTask extends AsyncTask<Void, Void, Object> {
        private final String q;
        private String url;
        SearchTask(String q) { this.q = q; }

        protected void onPreExecute() { showLoading(true); }

        protected Object doInBackground(Void... v) {
            try {
                ArrayList<AppShort> out = Api.getInstance(SearchActivity.this).searchApps(q);
                return out == null ? "Unable to load app catalog" : out;
            } catch (Exception e) {
                return "URL=" + url + "\n" + e.toString();
            }
        }

        @SuppressWarnings("unchecked")
        protected void onPostExecute(Object out) {
            showLoading(false);
            if (out instanceof String) {
                Toast.makeText(SearchActivity.this, "Search error: " + out, Toast.LENGTH_LONG).show();
                return;
            }
            List<AppShort> listOut = (List<AppShort>) out;
            data.clear();
            data.addAll(listOut);
            adapter.refreshInstalledPackages();
            adapter.notifyDataSetChanged();
            if (listOut.size() == 0) Toast.makeText(SearchActivity.this, R.string.nothing_found, Toast.LENGTH_SHORT).show();
        }

        protected void onCancelled() { showLoading(false); }
    }
}
