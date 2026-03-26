package com.example.eventlotteryapp.repository;

import com.example.eventlotteryapp.models.Comment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Firestore operations for comment subcollections stored at
 * {@code events/{eventId}/comments/{commentId}}.
 */
public class CommentRepository {

    private final FirebaseConnector firebaseConnector;
    private final FirebaseFirestore db;

    /**
     * Creates a new CommentRepository with a default FirebaseConnector.
     */
    public CommentRepository() {
        firebaseConnector = new FirebaseConnector();
        db = firebaseConnector.getDb();
    }

    /**
     * Generic callback interface for asynchronous Firestore operations.
     *
     * @param <T> the type of result returned on success
     */
    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    /**
     * Fetches all comments for a given event ordered by newest first.
     *
     * @param eventId the event ID
     * @param callback receives the list of comments on success
     */
    public void getCommentsForEvent(String eventId, FirestoreCallback<List<Comment>> callback) {
        db.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
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

    /**
     * Adds a new comment to an event.
     *
     * @param eventId the event ID
     * @param authorId the device ID of the user posting the comment
     * @param text the comment text
     * @param callback receives the new comment ID on success
     */
    public void addComment(String eventId, String authorId, String text,
                           FirestoreCallback<String> callback) {
        Comment comment = new Comment(null, eventId, authorId, text, Timestamp.now());

        db.collection("events")
                .document(eventId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    String commentId = documentReference.getId();
                    documentReference.update("commentId", commentId)
                            .addOnSuccessListener(unused -> callback.onSuccess(commentId))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes one comment from an event.
     *
     * @param eventId the event ID
     * @param commentId the comment document ID
     * @param callback receives null on success
     */
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