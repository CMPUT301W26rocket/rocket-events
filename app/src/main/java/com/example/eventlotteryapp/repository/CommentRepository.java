package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Comment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Firestore operations for comment subcollections under
 * events/{eventId}/comments/{commentId}.
 * This merged version keeps support for both the newer comment UI
 * and the admin comment-removal flow.
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
        if (eventId == null || eventId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }

        if (comment == null) {
            callback.onFailure(new IllegalArgumentException("Comment is required"));
            return;
        }

        if (comment.getTimestamp() == null) {
            comment.setTimestamp(Timestamp.now());
        }

        db.collection("events")
                .document(eventId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(docRef -> {
                    String id = docRef.getId();
                    comment.setCommentId(id);
                    comment.setEventId(eventId);

                    docRef.update("commentId", id)
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getCommentsForEvent(String eventId, FirestoreCallback<List<Comment>> callback) {
        db.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    List<Comment> comments = new ArrayList<>();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setCommentId(doc.getId());
                            comment.setEventId(eventId);
                            comments.add(comment);
                        }
                    }

                    callback.onSuccess(comments);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public ListenerRegistration listenForComments(String eventId, FirestoreCallback<List<Comment>> callback) {
        return db.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((query, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }

                    List<Comment> comments = new ArrayList<>();
                    if (query != null) {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(doc.getId());
                                comment.setEventId(eventId);
                                comments.add(comment);
                            }
                        }
                    }

                    callback.onSuccess(comments);
                });
    }

    public void deleteComment(String eventId, String commentId, FirestoreCallback<Void> callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Event ID is required"));
            return;
        }

        if (commentId == null || commentId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Comment ID is required"));
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}