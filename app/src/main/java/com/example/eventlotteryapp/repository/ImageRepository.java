package com.example.eventlotteryapp.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageRepository {

    private static final int MAX_DIMENSION = 1024; // px
    private static final int JPEG_QUALITY  = 80;   // %

    private final FirebaseStorage storage;

    public ImageRepository() {
        storage = FirebaseStorage.getInstance();
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    /**
     * Compresses the image from the given Uri, then uploads it to Firebase Storage.
     * Scales down to MAX_DIMENSION on the longest side and compresses to JPEG at JPEG_QUALITY.
     * Path: event_posters/{deviceId}_{timestamp}.jpg
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
     * Reads the image from Uri, scales it down if needed, and compresses to JPEG bytes.
     * Returns null if anything goes wrong.
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
