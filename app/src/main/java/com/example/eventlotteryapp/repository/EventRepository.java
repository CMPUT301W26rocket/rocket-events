package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Event;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventRepository {

    private final FirebaseConnector firebaseConnector;

    public EventRepository() {
        firebaseConnector = new FirebaseConnector();
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

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

    public void deleteEvent(String eventId, FirestoreCallback<Void> callback) {
        firebaseConnector.getEventsCollection()
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}