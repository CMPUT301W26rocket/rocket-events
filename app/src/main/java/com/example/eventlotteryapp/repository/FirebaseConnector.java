package com.example.eventlotteryapp.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseConnector {
    private final FirebaseFirestore db;

    public FirebaseConnector() {
        db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public CollectionReference getEventsCollection() {
        return db.collection("events");
    }

    public CollectionReference getUsersCollection() {
        return db.collection("users");
    }

    public CollectionReference getHistoryCollection() {
        return db.collection("history");
    }
}