package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents a comment left by a user on an event.
 * Comment documents are stored in Firestore under {@code events/{eventId}/comments/{commentId}}.
 * @author Daniel
 */
public class Comment {
    private String commentId;
    private String authorDeviceId;
    private String authorName;
    private String text;
    private Timestamp timestamp;

    /** Required empty constructor for Firestore deserialization. */
    public Comment() {}

    public Comment(String authorDeviceId, String authorName, String text, Timestamp timestamp) {
        this.authorDeviceId = authorDeviceId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = timestamp;
    }

    // --- Getters ---

    public String getCommentId() { return commentId; }
    public String getAuthorDeviceId() { return authorDeviceId; }
    public String getAuthorName() { return authorName; }
    public String getText() { return text; }
    public Timestamp getTimestamp() { return timestamp; }

    // --- Setters ---

    public void setCommentId(String commentId) { this.commentId = commentId; }
    public void setAuthorDeviceId(String authorDeviceId) { this.authorDeviceId = authorDeviceId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
