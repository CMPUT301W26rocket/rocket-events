package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Event;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all Firestore read and write operations for the {@code events} collection.
 * Uses {@link FirebaseConnector} to obtain collection references.
 */
public class EventRepository {

    private final FirebaseConnector firebaseConnector;

    /**
     * Creates a new EventRepository with a default {@link FirebaseConnector}.
     */
    public EventRepository() {
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
     * Adds a new event to Firestore. Firestore generates the document ID, which is then
     * written back into the event and persisted as the {@code eventId} field.
     *
     * @param event    the event to save
     * @param callback receives the new Firestore document ID on success
     */
    public void addEvent(Event event, FirestoreCallback<String> callback) {
        firebaseConnector.getEventsCollection()
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    String id = documentReference.getId();
                    event.setEventId(id);

                    firebaseConnector.getEventsCollection()
                            .document(id)
                            .set(event)
                            .addOnSuccessListener(unused -> callback.onSuccess(id))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches a single event by its Firestore document ID.
     *
     * @param eventId  the Firestore document ID of the event
     * @param callback receives the {@link Event} on success, or {@code null} if not found
     */
    public void getEventById(String eventId, FirestoreCallback<Event> callback) {
        firebaseConnector.getEventsCollection()
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(documentSnapshot.getId());
                        }
                        callback.onSuccess(event);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all events from the {@code events} collection.
     *
     * @param callback receives the list of all {@link Event} objects on success
     */
    public void getAllEvents(FirestoreCallback<List<Event>> callback) {
        firebaseConnector.getEventsCollection()
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(doc.getId());
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Overwrites an existing event document with the provided event's data.
     * Fails immediately if the event has no {@code eventId} set.
     *
     * @param event    the event to update; must have a non-empty {@code eventId}
     * @param callback receives {@code null} on success
     */
    public void updateEvent(Event event, FirestoreCallback<Void> callback) {
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Event ID is required."));
            return;
        }

        firebaseConnector.getEventsCollection()
                .document(event.getEventId())
                .set(event)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all events created by a specific organizer.
     *
     * @param organizerId the device ID of the organizer
     * @param callback    receives the list of matching {@link Event} objects on success
     */
    public void getEventsByOrganizerId(String organizerId, FirestoreCallback<List<Event>> callback) {
        firebaseConnector.getEventsCollection()
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(doc.getId());
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes an event document from Firestore.
     *
     * @param eventId  the Firestore document ID of the event to delete
     * @param callback receives {@code null} on success
     */
    public void deleteEvent(String eventId, FirestoreCallback<Void> callback) {
        firebaseConnector.getEventsCollection()
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
    /**
     * Updates the lotteryCompleted flag for an event.
     * @author Santiago
     * @param eventId   the Firestore document ID of the event
     * @param completed true if the lottery has been completed
     * @param callback  receives null on success
     */
    public void updateLotteryCompleted(String eventId, boolean completed, FirestoreCallback<Void> callback) {
        firebaseConnector.getEventsCollection()
                .document(eventId)
                .update("lotteryCompleted", completed)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates only the {@code posterUrl} field of an event document.
     *
     * @param eventId   the ID of the event to update
     * @param posterUrl the new Firebase Storage download URL for the poster
     * @param callback  receives {@code null} on success
     */
    public void updatePosterUrl(String eventId, String posterUrl, FirestoreCallback<Void> callback) {
        firebaseConnector.getEventsCollection()
                .document(eventId)
                .update("posterUrl", posterUrl)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
