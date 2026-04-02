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
 *
 * @author Mazen
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
     * Callback interface for image upload and delete operations.
     */
    public interface UploadCallback {

        /**
         * Called when the image operation succeeds.
         *
         * @param downloadUrl download URL of the uploaded image, or the deleted image URL
         */
        void onSuccess(String downloadUrl);

        /**
         * Called when the image operation fails.
         *
         * @param e exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Compresses the image from the given URI and uploads it to Firebase Storage.
     *
     * @param context Android context used to access the image content
     * @param deviceId device ID used to generate the file name
     * @param imageUri URI of the selected image
     * @param callback callback receiving the uploaded image URL or an error
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
     * Deletes an uploaded event poster from Firebase Storage using its download URL.
     *
     * @param imageUrl the Firebase Storage download URL of the image
     * @param callback receives success or failure for the delete operation
     */
    public void deleteImageByUrl(String imageUrl, UploadCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onFailure(new Exception("Image URL is missing"));
            return;
        }

        try {
            StorageReference ref = storage.getReferenceFromUrl(imageUrl);
            ref.delete()
                    .addOnSuccessListener(unused -> callback.onSuccess(imageUrl))
                    .addOnFailureListener(callback::onFailure);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    /**
     * Reads, scales, and compresses an image from the given URI into a JPEG byte array.
     *
     * @param context Android context used to access the image content
     * @param uri URI of the image to process
     * @return compressed image bytes, or {@code null} if processing fails
     */
    private byte[] compressImage(Context context, Uri uri) {
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(stream);
            if (stream != null) stream.close();
            if (original == null) return null;

            Bitmap scaled = scaleBitmap(original);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);

            if (scaled != original) scaled.recycle();
            original.recycle();

            return out.toByteArray();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Scales a bitmap so its longest side does not exceed {@code MAX_DIMENSION}.
     * Returns the original bitmap unchanged if scaling is not needed.
     *
     * @param bitmap bitmap to scale
     * @return scaled bitmap, or the original bitmap if already within size limits
     */
    private Bitmap scaleBitmap(Bitmap bitmap) {
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return bitmap;
        }

        float scale = (float) MAX_DIMENSION / Math.max(width, height);
        int newWidth  = Math.round(width  * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
}