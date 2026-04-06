package com.example.eventlotteryapp;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.utils.QRCodeUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link QRCodeUtils#generateQrBitmap}.
 *
 * <p>These run as instrumented tests (not src/test/) because {@link Bitmap}
 * is an Android SDK class and cannot be instantiated on the plain JVM.
 *
 * <p>{@link QRCodeUtils#saveQrToGallery} is tested at the bottom of this file.
 * It performs a real MediaStore write (no special permission needed on API 29+)
 * and cleans up the inserted file in the same test.
 */
@RunWith(AndroidJUnit4.class)
public class QRCodeUtilsTest {

    private static final String VALID_CONTENT = "eventlotteryapp://event/abc123";
    private static final int SIZE = 512;

    // -----------------------------------------------------------------------
    // generateQrBitmap — valid input
    // -----------------------------------------------------------------------

    /**
     * Generating a QR code for a valid content string must return a non-null bitmap.
     */
    @Test
    public void generateQrBitmap_returnsNonNull_forValidContent() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);

        assertNotNull(result);
    }

    /**
     * The generated bitmap must have exactly the requested width.
     */
    @Test
    public void generateQrBitmap_hasCorrectWidth() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);

        assertNotNull(result);
        assertEquals(SIZE, result.getWidth());
    }

    /**
     * The generated bitmap must have exactly the requested height.
     */
    @Test
    public void generateQrBitmap_hasCorrectHeight() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);

        assertNotNull(result);
        assertEquals(SIZE, result.getHeight());
    }

    /**
     * The generated bitmap must use the ARGB_8888 config so it has a full alpha channel.
     */
    @Test
    public void generateQrBitmap_usesArgb8888Config() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);

        assertNotNull(result);
        assertEquals(Bitmap.Config.ARGB_8888, result.getConfig());
    }

    /**
     * Every pixel in the generated bitmap must be either pure black (0xFF000000)
     * or pure white (0xFFFFFFFF). No other colours should be present.
     */
    @Test
    public void generateQrBitmap_containsOnlyBlackAndWhitePixels() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);
        assertNotNull(result);

        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
                int pixel = result.getPixel(x, y);
                assertTrue(
                        "Unexpected pixel colour at (" + x + "," + y + "): " + pixel,
                        pixel == 0xFF000000 || pixel == 0xFFFFFFFF
                );
            }
        }
    }

    /**
     * Two QR codes generated from the same content and size must produce identical bitmaps.
     */
    @Test
    public void generateQrBitmap_isDeterministic_forSameInput() {
        Bitmap first  = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);
        Bitmap second = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.sameAs(second));
    }

    /**
     * Two QR codes generated from different content strings must NOT produce the same bitmap.
     */
    @Test
    public void generateQrBitmap_producesDifferentBitmap_forDifferentContent() {
        Bitmap first  = QRCodeUtils.generateQrBitmap("eventlotteryapp://event/abc123", SIZE);
        Bitmap second = QRCodeUtils.generateQrBitmap("eventlotteryapp://event/xyz999", SIZE);

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(!first.sameAs(second));
    }

    /**
     * Generating a QR code with a small size (e.g. 64px) must still succeed and
     * return a bitmap of the requested dimensions.
     */
    @Test
    public void generateQrBitmap_worksForSmallSize() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, 64);

        assertNotNull(result);
        assertEquals(64, result.getWidth());
        assertEquals(64, result.getHeight());
    }

    /**
     * Generating a QR code with a large size (e.g. 1024px) must succeed and
     * return a bitmap of the requested dimensions.
     */
    @Test
    public void generateQrBitmap_worksForLargeSize() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, 1024);

        assertNotNull(result);
        assertEquals(1024, result.getWidth());
        assertEquals(1024, result.getHeight());
    }

    /**
     * The generated bitmap must contain at least one black pixel and at least one
     * white pixel, confirming that real QR content was encoded and the result is
     * not a blank canvas.
     */
    @Test
    public void generateQrBitmap_containsBothBlackAndWhitePixels() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);
        assertNotNull(result);

        boolean foundBlack = false;
        boolean foundWhite = false;
        outer:
        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
                int pixel = result.getPixel(x, y);
                if (pixel == 0xFF000000) foundBlack = true;
                if (pixel == 0xFFFFFFFF) foundWhite = true;
                if (foundBlack && foundWhite) break outer;
            }
        }

        assertTrue("Expected at least one black pixel", foundBlack);
        assertTrue("Expected at least one white pixel", foundWhite);
    }

    // -----------------------------------------------------------------------
    // generateQrBitmap — edge cases
    // -----------------------------------------------------------------------

    /**
     * Passing a null content string must return null rather than crashing.
     * ZXing throws a WriterException for null input which is caught and returns null.
     */
    @Test
    public void generateQrBitmap_returnsNull_forNullContent() {
        Bitmap result = QRCodeUtils.generateQrBitmap(null, SIZE);

        assertNull(result);
    }

    /**
     * Passing an empty content string must return null rather than crashing.
     * ZXing throws a WriterException for empty input which is caught and returns null.
     */
    @Test
    public void generateQrBitmap_returnsNull_forEmptyContent() {
        Bitmap result = QRCodeUtils.generateQrBitmap("", SIZE);

        assertNull(result);
    }

    /**
     * Passing zero as the size must return null rather than crashing.
     * ZXing or Bitmap.createBitmap throws an exception for zero dimensions
     * which is caught and returns null.
     */
    @Test
    public void generateQrBitmap_returnsNull_forZeroSize() {
        Bitmap result = QRCodeUtils.generateQrBitmap(VALID_CONTENT, 0);

        assertNull(result);
    }

    // -----------------------------------------------------------------------
    // saveQrToGallery
    // -----------------------------------------------------------------------

    /**
     * Saving a valid QR bitmap to the gallery must return {@code true}.
     * No special permission is needed on API 29+ (RELATIVE_PATH is used).
     * The test cleans up the inserted file from MediaStore after asserting.
     */
    @Test
    public void saveQrToGallery_returnsTrue_forValidBitmap() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap bitmap = QRCodeUtils.generateQrBitmap(VALID_CONTENT, SIZE);
        assertNotNull(bitmap);

        boolean result = QRCodeUtils.saveQrToGallery(context, bitmap, "test event");

        assertTrue(result);

        // Clean up: remove the file we just wrote so it does not litter the device gallery
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?",
                new String[]{"QR_test_event_%.png"},
                MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                context.getContentResolver().delete(uri, null, null);
            }
        }
    }

}
