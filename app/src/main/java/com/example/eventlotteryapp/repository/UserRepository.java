package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.User;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private final FirebaseConnector firebaseConnector;

    public UserRepository() {
        firebaseConnector = new FirebaseConnector();
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    /**
     * Fetches a user by deviceId. Returns null in onSuccess if the document doesn't exist.
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
     * Creates or updates a user's profile fields.
     * Uses set+merge so it works whether the document exists or not,
     * and never overwrites other fields like eventsHosting.
     */
    public void saveUserProfile(String deviceId, String name, String email, String phone,
                                FirestoreCallback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("notificationsEnabled", true);

        firebaseConnector.getUsersCollection()
                .document(deviceId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
