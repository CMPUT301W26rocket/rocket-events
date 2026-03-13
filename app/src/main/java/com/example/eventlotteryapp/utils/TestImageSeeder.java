package com.example.eventlotteryapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;

/**
 * Seeds a small set of sample event poster images into the device gallery on first run.
 * Subsequent runs are no-ops (tracked via SharedPreferences).
 *
 * <p>Call {@link #seedIfNeeded(Context)} once at app startup (e.g., from SplashActivity).
 * The images appear in the Photos app under Pictures/EventLotteryApp/TestPosters and can be
 * selected when choosing an event poster.
 */
public class TestImageSeeder {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_SEEDED = "gallery_test_images_seeded";

    /**
     * Seeds test images to the gallery if this has not been done before.
     * Uses SharedPreferences to track whether seeding has already occurred.
     *
     * @param context application context
     */
    public static void seedIfNeeded(Context context) {
        var prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SEEDED, false)) return;

        seedTestImages(context);
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
    }

    private static void seedTestImages(Context context) {
        String[][] events = {
            {"Summer Festival 2025",  "#E91E63", "#FFFFFF"},
            {"Tech Conference",       "#1565C0", "#FFFFFF"},
            {"Community Game Night",  "#2E7D32", "#FFFFFF"},
            {"Charity Fun Run",       "#E65100", "#FFFFFF"},
        };

        for (int i = 0; i < events.length; i++) {
            String label    = events[i][0];
            int bgColor     = Color.parseColor(events[i][1]);
            int textColor   = Color.parseColor(events[i][2]);

            Bitmap bmp = createPosterBitmap(label, bgColor, textColor);
            saveToGallery(context, bmp, "test_poster_" + (i + 1));
        }
    }

    private static Bitmap createPosterBitmap(String label, int bgColor, int textColor) {
        int w = 800, h = 600;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Background
        canvas.drawColor(bgColor);

        // Decorative band at the top
        Paint band = new Paint();
        band.setColor(0x33000000); // semi-transparent black
        canvas.drawRect(0, 0, w, 80, band);

        // "EVENT LOTTERY APP" header
        Paint header = new Paint(Paint.ANTI_ALIAS_FLAG);
        header.setColor(0xCCFFFFFF);
        header.setTextSize(28f);
        header.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        header.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("EVENT LOTTERY APP", w / 2f, 52f, header);

        // Main title
        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(textColor);
        title.setTextSize(68f);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, w / 2f, h / 2f - 10f, title);

        // Subtitle
        Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
        sub.setColor(textColor);
        sub.setAlpha(200);
        sub.setTextSize(32f);
        sub.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Sample Event Poster", w / 2f, h / 2f + 60f, sub);

        return bmp;
    }

    private static void saveToGallery(Context context, Bitmap bitmap, String name) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/EventLotteryApp/TestPosters");
        }

        try {
            android.net.Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return;
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                if (out == null) return;
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
        } catch (Exception ignored) {}
    }
}
