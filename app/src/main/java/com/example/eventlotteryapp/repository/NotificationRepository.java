package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all Firestore operations for notification subcollections stored at
 * {@code users/{deviceId}/notifications/{notificationId}}.
 */
public class NotificationRepository {

    private final FirebaseFirestore db;

    public NotificationRepository() {
        db = new FirebaseConnector().getDb();
    }

    /**
     * Generic callback interface for asynchronous Firestore operations.
     */
    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    /**
     * Adds a notification document to a user's notifications subcollection.
     *
     * @param deviceId     the recipient user's device ID
     * @param notification the notification to store
     * @param callback     receives the generated document ID on success
     */
    // US 01.04.01 US 01.04.02 US 01.05.01 US 02.05.01 US 02.07.01 US 02.07.02 US 02.07.03
    // TODO US 01.04.03: check users/{deviceId}.notificationsEnabled before writing — skip if false
    public void addNotification(String deviceId, Notification notification,
                                FirestoreCallback<String> callback) {
        if (notification.getRecipientDeviceId() == null || notification.getRecipientDeviceId().isEmpty()) {
            notification.setRecipientDeviceId(deviceId);
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(com.google.firebase.Timestamp.now());
        }

        db.collection("users")
                .document(deviceId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(ref -> {
                    notification.setNotificationId(ref.getId());
                    ref.update("notificationId", ref.getId());
                    callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all notifications for a user, ordered newest first.
     *
     * @param deviceId the user's device ID
     * @param callback receives the list of {@link Notification} objects on success
     */
    public void getNotificationsForUser(String deviceId,
                                        FirestoreCallback<List<Notification>> callback) {
        db.collection("users")
                .document(deviceId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setNotificationId(doc.getId());
                            notifications.add(n);
                        }
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Marks a specific notification as read.
     *
     * @param deviceId       the user's device ID
     * @param notificationId the notification document ID
     * @param callback       receives {@code null} on success
     */
    public void markAsRead(String deviceId, String notificationId,
                           FirestoreCallback<Void> callback) {
        db.collection("users")
                .document(deviceId)
                .collection("notifications")
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
