package com.example.eventlotteryapp.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Handles uploading event poster images to Firebase Storage.
 * Images are scaled down to at most {@code MAX_DIMENSION} pixels on the longest side
 * and compressed to JPEG before upload.
 * Uploaded files are stored under {@code event_posters/{deviceId}_{timestamp}.jpg}.
 */
public class ImageRepository {

    private static final int MAX_DIMENSION = 1024; // px
    private static final int JPEG_QUALITY  = 80;   // %

    private final FirebaseStorage storage;

    /**
     * Creates a new ImageRepository using the default Firebase Storage instance.
     */
    public ImageRepository() {
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Callback interface for image upload operations.
     */
    public interface UploadCallback {
        /**
         * Called when the upload succeeds.
         *
         * @param downloadUrl the public download URL of the uploaded image
         */
        void onSuccess(String downloadUrl);

        /**
         * Called when the upload fails.
         *
         * @param e the exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Compresses the image from the given Uri, then uploads it to Firebase Storage.
     * Scales down to {@code MAX_DIMENSION} on the longest side and compresses to JPEG
     * at {@code JPEG_QUALITY}.
     * The storage path is {@code event_posters/{deviceId}_{timestamp}.jpg}.
     *
     * @param context   the Android context used to open the image URI
     * @param deviceId  the uploader's device ID, used to namespace the filename
     * @param imageUri  the URI of the image to upload
     * @param callback  receives the download URL on success, or an exception on failure
     */
    public void uploadEventPoster(Context context, String deviceId, Uri imageUri,
                                  UploadCallback callback) {
        byte[] compressed = compressImage(context, imageUri);
        if (compressed == null) {
            callback.onFailure(new Exception("Failed to process image"));
            return;
        }

        String fileName = "event_posters/" + deviceId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        ref.putBytes(compressed)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Opens the image at the given URI, scales it down if needed using {@link #scaleBitmap},
     * and compresses it to a JPEG byte array at {@code JPEG_QUALITY} percent quality.
     * Returns {@code null} if the image cannot be read or processed.
     *
     * @param context the Android context used to open the content URI
     * @param uri     the URI pointing to the image to compress
     * @return the compressed JPEG image as a byte array, or {@code null} on failure
     */
    private byte[] compressImage(Context context, Uri uri) {
        try {
            // Decode bitmap from Uri using InputStream (BitmapFactory.decodeStream
            // is the correct non-deprecated approach for all API levels)
            InputStream stream = context.getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(stream);
            if (stream != null) stream.close();
            if (original == null) return null;

            // Scale down if either dimension exceeds MAX_DIMENSION
            Bitmap scaled = scaleBitmap(original);

            // Compress to JPEG bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);

            // Free memory
            if (scaled != original) scaled.recycle();
            original.recycle();

            return out.toByteArray();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Scales a bitmap down so its longest side is at most {@code MAX_DIMENSION} pixels.
     * If both dimensions are already within the limit, the original bitmap is returned unchanged.
     *
     * @param bitmap the original bitmap to scale
     * @return a scaled-down {@link Bitmap}, or the original if no scaling was needed
     */
    private Bitmap scaleBitmap(Bitmap bitmap) {
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return bitmap; // already small enough, no scaling needed
        }

        float scale = (float) MAX_DIMENSION / Math.max(width, height);
        int newWidth  = Math.round(width  * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
}
