package io.github.konbini.market.util;

public class AndroidVersions {

    public static String apiToAndroid(int api) {
        switch (api) {
            case 1: return "1.0+";
            case 2: return "1.1+";
            case 3: return "1.5+";
            case 4: return "1.6+";
            case 5: return "2.0+";
            case 6: return "2.0.1+";
            case 7: return "2.1+";
            case 8: return "2.2+";
            case 9: return "2.3.0+";
            case 10: return "2.3.3+";
            case 11: return "3.0+";
            case 12: return "3.1+";
            case 13: return "3.2+";
            case 14: return "4.0.1+";
            case 15: return "4.0.3+";
            case 16: return "4.1+";
            case 17: return "4.2+";
            case 18: return "4.3+";
            case 19: return "4.4+";
            case 20: return "4.4W";
            case 21: return "5.0+";
            case 22: return "5.1+";
            case 23: return "6.0+";
            default: return "API " + api;
        }
    }

    public static String apiToAndroidLabel(int api) {
        return "Android " + apiToAndroid(api);
    }
}