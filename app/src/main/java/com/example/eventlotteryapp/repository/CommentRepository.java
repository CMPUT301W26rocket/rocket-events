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
 * @author Daniel
 */
public class CommentRepository {

    private final FirebaseFirestore db;

    public CommentRepository() {
        db = new FirebaseConnector().getDb();
    }

    /**
     * Generic callback interface for asynchronous Firestore operations.
     *
     * @param <T> the type of the result returned on success
     */
    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    /**
     * Writes a new comment document to {@code events/{eventId}/comments/}.
     * Firestore auto-generates the document ID.
     *
     * @param eventId  the ID of the event being commented on
     * @param comment  the comment to save
     * @param callback receives {@code null} on success
     */
    public void addComment(String eventId, Comment comment, FirestoreCallback<Void> callback) {
        db.collection("events")
                .document(eventId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(ref -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Attaches a real-time listener that fires whenever the comment list changes.
     * Comments are returned in chronological order (oldest first).
     * The caller must call {@link ListenerRegistration#remove()} when done.
     *
     * @param eventId  the ID of the event whose comments to watch
     * @param callback receives the full updated list of {@link Comment} objects on each change
     * @return a {@link ListenerRegistration} that must be removed when no longer needed
     */
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
}
