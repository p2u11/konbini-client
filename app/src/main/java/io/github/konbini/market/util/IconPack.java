package io.github.konbini.market.util;

import android.content.Context;

public class IconPack {
    public static final String PACK_DEFAULT = "default";
    public static final String PACK_MARKET1 = "market1";

    public static String getPack(Context c) {
        return c.getSharedPreferences("oldmarket_prefs", Context.MODE_PRIVATE).getString("icon_pack", PACK_DEFAULT);
    }

    public static void setPack(Context c, String pack) {
        if (pack == null || pack.length() == 0) pack = PACK_DEFAULT;
        c.getSharedPreferences("oldmarket_prefs", Context.MODE_PRIVATE).edit().putString("icon_pack", pack).commit();
    }

    public static int bagNormal(Context c) {
        return isMarket1(c) ? res(c, "ic_market1_bag_normal", "ic_market_bag_normal") : res(c, "ic_market_bag_normal", "ic_market_bag_normal");
    }
    public static int bagPressed(Context c) {
        return isMarket1(c) ? res(c, "ic_market1_bag_pressed", "ic_market_bag_pressed") : res(c, "ic_market_bag_pressed", "ic_market_bag_pressed");
    }
    public static int bagSelected(Context c) {
        return isMarket1(c) ? res(c, "ic_market1_bag_selected", "ic_market_bag_selected") : res(c, "ic_market_bag_selected", "ic_market_bag_selected");
    }
    public static int launcher(Context c) {
        return isMarket1(c) ? res(c, "ic_launcher_androidmarket", "ic_launcher") : res(c, "ic_launcher_androidmarket", "ic_launcher");
    }

    private static boolean isMarket1(Context c) {
        return PACK_MARKET1.equals(getPack(c));
    }

    private static int res(Context c, String preferred, String fallback) {
        int id = c.getResources().getIdentifier(preferred, "drawable", c.getPackageName());
        if (id == 0) id = c.getResources().getIdentifier(fallback, "drawable", c.getPackageName());
        return id;
    }
}
