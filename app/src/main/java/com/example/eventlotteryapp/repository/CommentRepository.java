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
 *
 * @author Mazen
 */
public class CommentRepository {

    private final FirebaseFirestore db;

    /**
     * Creates a new comment repository using the shared Firebase connector.
     */
    public CommentRepository() {
        db = new FirebaseConnector().getDb();
    }

    /**
     * Generic callback interface for asynchronous Firestore operations.
     *
     * @param <T> result type returned on success
     */
    public interface FirestoreCallback<T> {

        /**
         * Called when the Firestore operation completes successfully.
         *
         * @param result operation result
         */
        void onSuccess(T result);

        /**
         * Called when the Firestore operation fails.
         *
         * @param e exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Adds a new comment to the given event's comments subcollection.
     * If the comment has no timestamp, the current time is assigned before saving.
     *
     * @param eventId event ID that owns the comment
     * @param comment comment to add
     * @param callback callback receiving success or failure
     */
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

    /**
     * Retrieves all comments for a given event ordered by timestamp ascending.
     *
     * @param eventId event ID whose comments should be loaded
     * @param callback callback receiving the loaded comment list or an error
     */
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

    /**
     * Registers a realtime listener for comments on a given event ordered by timestamp ascending.
     *
     * @param eventId event ID whose comments should be observed
     * @param callback callback receiving updated comment lists or errors
     * @return Firestore listener registration that can be removed later
     */
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

    /**
     * Alias for {@link #listenForComments(String, FirestoreCallback)} kept for compatibility.
     *
     * @param eventId event ID whose comments should be observed
     * @param callback callback receiving updated comment lists or errors
     * @return Firestore listener registration that can be removed later
     */
    public ListenerRegistration listenToComments(String eventId, FirestoreCallback<List<Comment>> callback) {
        return listenForComments(eventId, callback);
    }

    /**
     * Deletes a specific comment from an event's comments subcollection.
     *
     * @param eventId event ID that owns the comment
     * @param commentId comment document ID to delete
     * @param callback callback receiving success or failure
     */
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