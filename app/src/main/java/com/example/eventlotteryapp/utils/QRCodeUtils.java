package com.example.eventlotteryapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for generating QR code bitmaps and saving them to the device gallery.
 */
public class QRCodeUtils {

    /**
     * Generates a square QR code bitmap encoding the given content string.
     *
     * @param content the text/URL to encode in the QR code
     * @param sizePx  the width and height of the output bitmap in pixels
     * @return a {@link Bitmap} of the QR code, or {@code null} if generation failed
     * @author Daniel
     */
    public static Bitmap generateQrBitmap(String content, int sizePx) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(
                    content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (WriterException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Saves a QR code bitmap to the device's Pictures/EventLotteryApp folder via MediaStore.
     * Works on API 24+. No special permission is required on API 29+; on API 24–28 the
     * WRITE_EXTERNAL_STORAGE permission must be granted before calling this.
     *
     * @param context    application context
     * @param bitmap     the QR code bitmap to save
     * @param eventTitle event title used to build the filename
     * @return {@code true} if the image was saved successfully, {@code false} otherwise
     */
    public static boolean saveQrToGallery(Context context, Bitmap bitmap, String eventTitle) {
        String safeName = eventTitle.replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = "QR_" + safeName + "_" + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/EventLotteryApp");
        }

        try {
            android.net.Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return false;
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                if (out == null) return false;
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
