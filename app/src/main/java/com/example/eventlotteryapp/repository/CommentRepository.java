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
 *  Handles all Firestore operations for comment subcollections stored at
 *  {@code events/{eventId}/comments/{commentId}}.
 *  User Stories Implemented:
 *  US 01.08.01 As an entrant, I want to post a comment on an event so that I can share feedback, ask questions, or engage with other users about the event.
 *  US 01.08.02 As an entrant, I want to view comments on an event so that I can read feedback, questions, or discussion related to that event.
 *  US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.
 *  US 02.08.02 As an organizer, I want to comment on my events so that I can share updates, answer questions, or engage with entrants in the event discussion.
 * @author Daniel
 * @co-author Mazen
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

    /**
     * Adds a new comment to the event's comments subcollection.
     * Automatically sets the {@code commentId} and {@code eventId} fields on the comment after creation.
     *
     * @param eventId  the ID of the event to post the comment on
     * @param comment  the comment to add
     * @param callback receives {@code null} on success
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
     * Fetches all comments for an event as a one-time read, ordered by timestamp ascending.
     * Used by the admin comment screen.
     *
     * @param eventId  the ID of the event whose comments to fetch
     * @param callback receives the list of {@link Comment} objects on success
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
     * Attaches a real-time listener that fires whenever comments change for the event.
     * Comments are ordered by timestamp ascending. The caller must call
     * {@link ListenerRegistration#remove()} when done to avoid memory leaks.
     *
     * @param eventId  the ID of the event to watch
     * @param callback receives the updated list of comments on each change
     * @return a {@link ListenerRegistration} that must be removed when no longer needed
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
     * Alias for {@link #listenForComments(String, FirestoreCallback)}.
     * Provided for compatibility with call sites that use the {@code listenToComments} name.
     *
     * @param eventId  the ID of the event to watch
     * @param callback receives the updated list of comments on each change
     * @return a {@link ListenerRegistration} that must be removed when no longer needed
     */
    public ListenerRegistration listenToComments(String eventId, FirestoreCallback<List<Comment>> callback) {
        return listenForComments(eventId, callback);
    }
    /**
     * Deletes a single comment from the event's comments subcollection.
     *
     * @param eventId   the ID of the event the comment belongs to
     * @param commentId the ID of the comment to delete
     * @param callback  receives {@code null} on success
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