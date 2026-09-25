package io.github.konbini.market.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.konbini.market.net.Http;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

// safer loader for old Android / low RAM devices
public class ImageLoader {

    private static final int MAX_MEM_ITEMS = 40;

    private static final LinkedHashMap<String, Bitmap> mem =
            new LinkedHashMap<String, Bitmap>(MAX_MEM_ITEMS, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
                    return size() > MAX_MEM_ITEMS;
                }
            };

    private static File iconCacheDir(Context c) {
        File d = new File(c.getCacheDir(), "icons");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static Bitmap memGet(String k) {
        synchronized (mem) { return mem.get(k); }
    }

    private static void memPut(String k, Bitmap b) {
        synchronized (mem) { mem.put(k, b); }
    }

    private static int calcInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (reqWidth <= 0) reqWidth = 64;
        if (reqHeight <= 0) reqHeight = 64;

        while ((height / inSampleSize) > reqHeight * 2 || (width / inSampleSize) > reqWidth * 2) {
            inSampleSize *= 2;
        }

        if (inSampleSize < 1) inSampleSize = 1;
        return inSampleSize;
    }

    private static Bitmap decodeSampled(byte[] data, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calcInSampleSize(bounds, reqWidth, reqHeight);
            opts.inPreferredConfig = Bitmap.Config.RGB_565; // 2 bytes per pixel, less RAM
            opts.inDither = true;

            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (OutOfMemoryError e) {
            try {
                BitmapFactory.Options opts2 = new BitmapFactory.Options();
                opts2.inSampleSize = 8;
                opts2.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeByteArray(data, 0, data.length, opts2);
            } catch (Throwable t) {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap decodeSampledFile(String path, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calcInSampleSize(bounds, reqWidth, reqHeight);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inDither = true;

            return BitmapFactory.decodeFile(path, opts);
        } catch (OutOfMemoryError e) {
            try {
                BitmapFactory.Options opts2 = new BitmapFactory.Options();
                opts2.inSampleSize = 8;
                opts2.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeFile(path, opts2);
            } catch (Throwable t) {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static void load(final Context c, final String url, final ImageView iv, final int placeholderRes) {
        iv.setImageResource(placeholderRes);
        iv.setTag(url);

        if (url == null || url.length() == 0) return;

        Bitmap cached = memGet(url);
        if (cached != null) {
            Object tag = iv.getTag();
            if (tag != null && url.equals(tag.toString())) {
                iv.setImageBitmap(cached);
            }
            return;
        }

        final int reqW = (iv.getLayoutParams() != null && iv.getLayoutParams().width > 0)
                ? iv.getLayoutParams().width : 96;
        final int reqH = (iv.getLayoutParams() != null && iv.getLayoutParams().height > 0)
                ? iv.getLayoutParams().height : 96;

        final String key = Hash.md5(url);
        final File f = new File(iconCacheDir(c), key + ".img");

        if (f.exists()) {
            Bitmap fb = decodeSampledFile(f.getAbsolutePath(), reqW, reqH);
            if (fb != null) {
                memPut(url, fb);
                Object tag = iv.getTag();
                if (tag != null && url.equals(tag.toString())) {
                    iv.setImageBitmap(fb);
                }
                return;
            }
        }

        new AsyncTask<Void, Void, Bitmap>() {
            protected Bitmap doInBackground(Void... v) {
                try {
                    byte[] data = Http.getBytes(url);
                    if (data == null) return null;

                    Bitmap bmp = decodeSampled(data, reqW, reqH);
                    if (bmp == null) return null;

                    try {
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(data);
                        fos.close();
                    } catch (Throwable e) { }

                    return bmp;
                } catch (OutOfMemoryError e) {
                    return null;
                } catch (Throwable e) {
                    return null;
                }
            }

            protected void onPostExecute(Bitmap bmp) {
                if (bmp != null) {
                    memPut(url, bmp);
                    Object tag = iv.getTag();
                    if (tag != null && url.equals(tag.toString())) {
                        try {
                            iv.setImageBitmap(bmp);
                        } catch (Throwable e) {
                            iv.setImageResource(placeholderRes);
                        }
                    }
                } else {
                    Object tag = iv.getTag();
                    if (tag != null && url.equals(tag.toString())) {
                        iv.setImageResource(placeholderRes);
                    }
                }
            }
        }.execute();
    }
    public static void loadBanner(final Context c, final String url, final ImageView iv, final int placeholderRes) {
        iv.setImageResource(placeholderRes);
        iv.setTag(url);

        if (url == null || url.length() == 0) return;

        Bitmap cached = memGet("banner:" + url);
        if (cached != null) {
            Object tag = iv.getTag();
            if (tag != null && url.equals(tag.toString())) {
                iv.setImageBitmap(cached);
            }
            return;
        }

        final int reqW = 1024;
        final int reqH = 400;

        new AsyncTask<Void, Void, Bitmap>() {
            protected Bitmap doInBackground(Void... v) {
                try {
                    byte[] data = Http.getBytes(url);
                    if (data == null) return null;

                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = calcInSampleSize(bounds, reqW, reqH);
                    opts.inPreferredConfig = Bitmap.Config.RGB_565;
                    opts.inDither = true;

                    return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                } catch (Throwable e) {
                    return null;
                }
            }

            protected void onPostExecute(Bitmap bmp) {
                if (bmp != null) {
                    memPut("banner:" + url, bmp);
                    Object tag = iv.getTag();
                    if (tag != null && url.equals(tag.toString())) {
                        iv.setImageBitmap(bmp);
                    }
                } else {
                    iv.setImageResource(placeholderRes);
                }
            }
        }.execute();
    }
}