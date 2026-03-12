package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Entrant;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all Firestore operations for entrant subcollections stored at
 * {@code events/{eventId}/entrants/{deviceId}}.
 * Manages waitlist membership, status transitions, and history queries.
 */
public class EntrantRepository {

    private final FirebaseConnector firebaseConnector;
    private final FirebaseFirestore db;

    /**
     * Creates a new EntrantRepository with a default {@link FirebaseConnector}.
     */
    public EntrantRepository() {
        firebaseConnector = new FirebaseConnector();
        db = firebaseConnector.getDb();
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
     * Adds a user to an event's waitlist.
     * Creates the entrant doc at {@code events/{eventId}/entrants/{deviceId}}
     * with status {@link Entrant#STATUS_WAITLIST}.
     *
     * @param eventId  the ID of the event to join
     * @param deviceId the device ID of the user joining
     * @param callback receives {@code null} on success
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
     * Also updates the {@code statusUpdatedAt} timestamp to now.
     *
     * @param eventId   the ID of the event
     * @param deviceId  the device ID of the entrant
     * @param newStatus the new status value (use {@link Entrant} {@code STATUS_*} constants)
     * @param callback  receives {@code null} on success
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
     * e.g. {@code getEntrantsByStatus(eventId, Entrant.STATUS_WAITLIST, callback)}
     *
     * @param eventId  the ID of the event
     * @param status   the status to filter by (use {@link Entrant} {@code STATUS_*} constants)
     * @param callback receives the list of matching {@link Entrant} objects on success
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
     *
     * @param eventId  the ID of the event
     * @param callback receives the list of all {@link Entrant} objects on success
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
     * <p>NOTE: This requires a Firestore composite index.
     * When you first run this, Firestore will log a link in Logcat — click it to auto-create the index.
     * Or go to Firebase Console → Firestore → Indexes → Add:
     * Collection group: entrants | Field: deviceId ASC
     *
     * @param deviceId the device ID of the user whose history to fetch
     * @param callback receives the list of {@link Entrant} objects (with {@code eventId} populated) on success
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
     * Checks whether the waitlist for an event has reached its limit.
     * Queries all entrants with status {@link Entrant#STATUS_WAITLIST} and compares
     * the count against the provided limit.
     *
     * @param eventId  the ID of the event to check
     * @param limit    the maximum number of waitlist spots allowed
     * @param callback receives {@code true} if the waitlist is full, {@code false} if there is space
     */
    public void isWaitlistFull(String eventId, int limit, FirestoreCallback<Boolean> callback) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .whereEqualTo("status", Entrant.STATUS_WAITLIST)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(query.size() >= limit))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes a user from an event's waitlist by deleting their entrant document.
     * Used when the user chooses to leave the waitlist or remove themselves after not being selected.
     *
     * @param eventId  the ID of the event to leave
     * @param deviceId the device ID of the user leaving
     * @param callback receives {@code null} on success
     */
    public void leaveWaitlist(String eventId, String deviceId, FirestoreCallback<Void> callback) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches a single entrant record for a user in a specific event, regardless of status.
     * Useful for checking whether a user has already joined an event's waitlist.
     *
     * @param eventId  the ID of the event to look up
     * @param deviceId the device ID of the user to check
     * @param callback receives the {@link Entrant} if the user is an entrant, or {@code null} if not found
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
