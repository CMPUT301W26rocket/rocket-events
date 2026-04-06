package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 *  Represents a comment left by a user on an event.
 *  Comment documents are stored in Firestore under {@code events/{eventId}/comments/{commentId}}.
 *  User Stories Implemented:
 *  US 01.08.01 As an entrant, I want to post a comment on an event so that I can share feedback, ask questions, or engage with other users about the event.
 *  US 01.08.02 As an entrant, I want to view comments on an event so that I can read feedback, questions, or discussion related to that event.
 *  US 02.08.02 As an organizer, I want to comment on my events so that I can share updates, answer questions, or engage with entrants in the event discussion.
 * @author Daniel
 * @co-author Mazen
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