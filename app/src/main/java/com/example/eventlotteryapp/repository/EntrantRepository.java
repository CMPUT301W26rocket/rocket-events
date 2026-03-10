package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Entrant;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EntrantRepository {

    private final FirebaseConnector firebaseConnector;
    private final FirebaseFirestore db;

    public EntrantRepository() {
        firebaseConnector = new FirebaseConnector();
        db = firebaseConnector.getDb();
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    /**
     * Adds a user to an event's waitlist.
     * Creates the entrant doc at events/{eventId}/entrants/{deviceId}.
     */
    public void joinWaitlist(String eventId, String deviceId, FirestoreCallback<Void> callback) {
        Timestamp now = Timestamp.now();
        Entrant entrant = new Entrant(deviceId, eventId, Entrant.STATUS_WAITLIST, now, now);

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(deviceId)
                .set(entrant)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates an entrant's status (e.g. waitlist → invited, invited → enrolled).
     */
    public void updateStatus(String eventId, String deviceId, String newStatus,
                             FirestoreCallback<Void> callback) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(deviceId)
                .update("status", newStatus, "statusUpdatedAt", Timestamp.now())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns all entrants for an event with a specific status.
     * e.g. getEntrantsByStatus(eventId, Entrant.STATUS_WAITLIST, callback)
     */
    public void getEntrantsByStatus(String eventId, String status,
                                    FirestoreCallback<List<Entrant>> callback) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(query -> {
                    List<Entrant> entrants = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) {
                            entrant.setEventId(eventId);
                            entrants.add(entrant);
                        }
                    }
                    callback.onSuccess(entrants);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns all entrant docs for an event regardless of status.
     * Useful for showing the full entrant list grouped by status.
     */
    public void getAllEntrantsForEvent(String eventId, FirestoreCallback<List<Entrant>> callback) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(query -> {
                    List<Entrant> entrants = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) {
                            entrant.setEventId(eventId);
                            entrants.add(entrant);
                        }
                    }
                    callback.onSuccess(entrants);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns the full event history for a user across ALL events.
     * Uses a Firestore collection group query on all "entrants" subcollections.
     *
     * NOTE: This requires a Firestore composite index.
     * When you first run this, Firestore will log a link in Logcat — click it to auto-create the index.
     * Or go to Firebase Console → Firestore → Indexes → Add:
     *   Collection group: entrants | Field: deviceId ASC
     */
    public void getUserEventHistory(String deviceId, FirestoreCallback<List<Entrant>> callback) {
        db.collectionGroup("entrants")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(query -> {
                    List<Entrant> history = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) {
                            // The eventId is the parent document's ID
                            // Path: events/{eventId}/entrants/{deviceId}
                            entrant.setEventId(doc.getReference().getParent().getParent().getId());
                            history.add(entrant);
                        }
                    }
                    callback.onSuccess(history);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Checks if a user is already an entrant in an event (any status).
     */
    public void getEntrant(String eventId, String deviceId, FirestoreCallback<Entrant> callback) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) entrant.setEventId(eventId);
                        callback.onSuccess(entrant);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
}
