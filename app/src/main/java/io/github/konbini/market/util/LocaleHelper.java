package io.github.konbini.market.util;

import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;

public class LocaleHelper {
    public static void applySavedLocale(Context context) {
        String lang = Prefs.getLang(context);
        if (lang == null || lang.length() == 0) return;

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}
