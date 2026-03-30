package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Comment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all Firestore operations for comment subcollections stored at
 * {@code events/{eventId}/comments/{commentId}}.
 * User Stories Implemented:
 * US 01.08.01 As an entrant, I want to post a comment on an event so that I can share feedback, ask questions, or engage with other users about the event.
 * US 01.08.02 As an entrant, I want to view comments on an event so that I can read feedback, questions, or discussion related to that event.
 * US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.
 * US 02.08.02 As an organizer, I want to comment on my events so that I can share updates, answer questions, or engage with entrants in the event discussion.
 * @author Daniel
 */
public class CommentRepository {

    private final FirebaseFirestore db;

    public CommentRepository() {
        db = new FirebaseConnector().getDb();
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
    public void addComment(String eventId, Comment comment, FirestoreCallback<Void> callback) {
        db.collection("events")
                .document(eventId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(ref -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
    public ListenerRegistration listenToComments(String eventId, FirestoreCallback<List<Comment>> callback) {
        return db.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((query, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }
                    if (query != null) {
                        List<Comment> comments = new ArrayList<>();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(doc.getId());
                                comments.add(comment);
                            }
                        }
                        callback.onSuccess(comments);
                    }
                });
    }

    public void deleteComment(String eventId, String commentId, FirestoreCallback<Void> callback) {
        db.collection("events")
                .document(eventId)
                .collection("comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
