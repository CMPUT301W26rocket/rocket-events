package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents a comment stored for an event.
 * This merged version keeps fields needed by both the older admin comment flow
 * and the newer comment UI code.
 */
public class Comment {

    private String commentId;
    private String eventId;
    private String authorId;
    private String authorDeviceId;
    private String authorName;
    private String text;
    private Timestamp timestamp;

    public Comment() {}

    public Comment(String authorDeviceId, String authorName, String text, Timestamp timestamp) {
        this.authorDeviceId = authorDeviceId;
        this.authorId = authorDeviceId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAuthorId() {
        return authorId != null && !authorId.isEmpty() ? authorId : authorDeviceId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
        this.authorDeviceId = authorId;
    }

    public String getAuthorDeviceId() {
        return authorDeviceId != null && !authorDeviceId.isEmpty() ? authorDeviceId : authorId;
    }

    public void setAuthorDeviceId(String authorDeviceId) {
        this.authorDeviceId = authorDeviceId;
        this.authorId = authorDeviceId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}