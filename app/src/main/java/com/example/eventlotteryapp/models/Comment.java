package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents a single comment attached to one event.
 * Comment documents are stored at {@code events/{eventId}/comments/{commentId}} in Firestore.
 * The {@code commentId} and {@code eventId} fields are populated locally after querying.
 */
public class Comment {

    private String commentId;
    private String eventId;
    private String authorId;
    private String text;
    private Timestamp timestamp;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Comment() {}

    /**
     * Creates a fully initialized Comment.
     *
     * @param commentId the comment document ID (local only, may be null before Firestore add)
     * @param eventId the event ID this comment belongs to (local only)
     * @param authorId the device ID of the user who posted the comment
     * @param text the comment text
     * @param timestamp the time the comment was created
     */
    public Comment(String commentId, String eventId, String authorId, String text, Timestamp timestamp) {
        this.commentId = commentId;
        this.eventId = eventId;
        this.authorId = authorId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getText() {
        return text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}