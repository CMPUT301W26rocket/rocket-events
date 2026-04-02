package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles all Firestore read and write operations for the {@code users} collection.
 * Uses {@link FirebaseConnector} to obtain collection references.
 * @author William
 */
public class UserRepository {

    private final FirebaseConnector firebaseConnector;

    /**
     * Creates a new UserRepository with a default {@link FirebaseConnector}.
     */
    public UserRepository() {
        firebaseConnector = new FirebaseConnector();
    }

    /**
     * Generic callback interface for asynchronous Firestore operations.
     *
     * @param <T> the type of the result returned on success
     */
    public interface FirestoreCallback<T> {
        /**
         * Called when the operation completes successfully.
         *
         * @param result the result of the operation, or {@code null} if none
         */
        void onSuccess(T result);

        /**
         * Called when the operation fails.
         *
         * @param e the exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Fetches a user by deviceId. Returns null in onSuccess if the document doesn't exist.
     *
     * @param deviceId the device's unique ANDROID_ID
     * @param callback receives the {@link User} on success, or {@code null} if not found
     */
    public void getUser(String deviceId, FirestoreCallback<User> callback) {
        firebaseConnector.getUsersCollection()
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess(doc.toObject(User.class));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Creates or updates a user's profile fields in Firestore.
     * Uses set+merge so it works whether the document already exists or not.
     *
     * @param deviceId             the device's unique ANDROID_ID, used as the Firestore document ID
     * @param name                 the user's display name
     * @param email                the user's email address
     * @param phone                the user's phone number, or empty string if not provided
     * @param notificationsEnabled whether the user wants to receive notifications
     * @param callback             receives {@code null} on success
     */
    public void saveUserProfile(String deviceId, String name, String email, String phone,
                                boolean notificationsEnabled, FirestoreCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("notificationsEnabled", notificationsEnabled);

        firebaseConnector.getUsersCollection()
                .document(deviceId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates only the notificationsEnabled field for a user.
     *
     * @param deviceId the device's unique ANDROID_ID
     * @param enabled  {@code true} to enable notifications, {@code false} to disable
     * @param callback receives {@code null} on success
     */
    public void setNotificationsEnabled(String deviceId, boolean enabled, FirestoreCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("notificationsEnabled", enabled);

        firebaseConnector.getUsersCollection()
                .document(deviceId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Searches users whose name, email, or phone contains the given query string (case-insensitive).
     * Fetches the entire users collection and filters client-side.
     *
     * @param query    the search term to match against name, email, or phone
     * @param callback receives the list of matching {@link User} objects on success
     */
    public void searchUsers(String query, FirestoreCallback<List<User>> callback) {
        String queryLower = query.toLowerCase(Locale.getDefault()).trim();
        firebaseConnector.getUsersCollection()
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> results = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user == null) continue;
                        String name  = user.getName()  != null ? user.getName().toLowerCase(Locale.getDefault())  : "";
                        String email = user.getEmail() != null ? user.getEmail().toLowerCase(Locale.getDefault()) : "";
                        String phone = user.getPhone() != null ? user.getPhone()  : "";
                        if (name.contains(queryLower) || email.contains(queryLower) || phone.contains(queryLower)) {
                            results.add(user);
                        }
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes a user's profile document from Firestore.
     *
     * @param deviceId the device's unique ANDROID_ID
     * @param callback receives {@code null} on success
     */
    public void deleteUser(String deviceId, FirestoreCallback<Void> callback) {
        firebaseConnector.getUsersCollection()
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
