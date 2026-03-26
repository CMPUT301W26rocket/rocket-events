package com.example.eventlotteryapp.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Provides a single point of access to the Firebase Firestore instance
 * and exposes named collection references used throughout the app.
 */
public class FirebaseConnector {
    private final FirebaseFirestore db;

    /**
     * Initialises the connector by obtaining the singleton Firestore instance.
     */
    public FirebaseConnector() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns the underlying Firestore database instance.
     *
     * @return the {@link FirebaseFirestore} instance
     */
    public FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Returns a reference to the {@code events} collection.
     *
     * @return {@link CollectionReference} for {@code events}
     */
    public CollectionReference getEventsCollection() {
        return db.collection("events");
    }

    /**
     * Returns a reference to the {@code users} collection.
     *
     * @return {@link CollectionReference} for {@code users}
     */
    public CollectionReference getUsersCollection() {
        return db.collection("users");
    }

    /**
     * Returns a reference to the {@code admins} collection.
     *
     * @return {@link CollectionReference} for {@code admins}
     */
    public CollectionReference getAdminsCollection() {
        return db.collection("admins");
    }

    /**
     * Returns a reference to the {@code history} collection.
     *
     * @return {@link CollectionReference} for {@code history}
     */
    public CollectionReference getHistoryCollection() {
        return db.collection("history");
    }

    /**
     * Returns a reference to the {@code adminDevices} collection.
     * Each document ID is a device ID that has been granted admin privileges.
     *
     * @return {@link CollectionReference} for {@code adminDevices}
     */
    public CollectionReference getAdminDevicesCollection() {
        return db.collection("adminDevices");
    }

    /**
     * Checks whether the given device ID has admin privileges by looking up
     * its document in the {@code adminDevices} collection.
     *
     * @param deviceId the device ID to check
     * @param callback called with {@code true} if admin, {@code false} otherwise
     */
    public void isAdminDevice(String deviceId, IsAdminCallback callback) {
        getAdminDevicesCollection().document(deviceId).get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists()))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    /** Callback for {@link #isAdminDevice}. */
    public interface IsAdminCallback {
        void onResult(boolean isAdmin);
    }
}
