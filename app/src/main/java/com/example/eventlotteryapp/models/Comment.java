package com.example.eventlotteryapp.models;

import com.google.firebase.Timestamp;

/**
 * Represents a comment stored for an event.
 * This merged version keeps fields needed by both the older admin comment flow
 * and the newer comment UI code.
 *
 * @author Mazen
 */
public class Comment {

    private String commentId;
    private String eventId;
    private String authorId;
    private String authorDeviceId;
    private String authorName;
    private String text;
    private Timestamp timestamp;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Comment() {}

    /**
     * Creates a comment with author information, message text, and timestamp.
     *
     * @param authorDeviceId device ID of the comment author
     * @param authorName display name of the comment author
     * @param text comment body text
     * @param timestamp time the comment was created
     */
    public Comment(String authorDeviceId, String authorName, String text, Timestamp timestamp) {
        this.authorDeviceId = authorDeviceId;
        this.authorId = authorDeviceId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = timestamp;
    }

    /**
     * Returns the comment document ID.
     *
     * @return comment ID
     */
    public String getCommentId() {
        return commentId;
    }

    /**
     * Sets the comment document ID.
     *
     * @param commentId comment ID
     */
    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    /**
     * Returns the related event ID.
     *
     * @return event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the related event ID.
     *
     * @param eventId event ID
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the author ID, falling back to {@code authorDeviceId} if needed.
     *
     * @return author ID
     */
    public String getAuthorId() {
        return authorId != null && !authorId.isEmpty() ? authorId : authorDeviceId;
    }

    /**
     * Sets the author ID and keeps {@code authorDeviceId} in sync.
     *
     * @param authorId author ID
     */
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
        this.authorDeviceId = authorId;
    }

    /**
     * Returns the author device ID, falling back to {@code authorId} if needed.
     *
     * @return author device ID
     */
    public String getAuthorDeviceId() {
        return authorDeviceId != null && !authorDeviceId.isEmpty() ? authorDeviceId : authorId;
    }

    /**
     * Sets the author device ID and keeps {@code authorId} in sync.
     *
     * @param authorDeviceId author device ID
     */
    public void setAuthorDeviceId(String authorDeviceId) {
        this.authorDeviceId = authorDeviceId;
        this.authorId = authorDeviceId;
    }

    /**
     * Returns the display name of the comment author.
     *
     * @return author name
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * Sets the display name of the comment author.
     *
     * @param authorName author name
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * Returns the comment text.
     *
     * @return comment body text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the comment text.
     *
     * @param text comment body text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the comment timestamp.
     *
     * @return comment timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the comment timestamp.
     *
     * @param timestamp comment timestamp
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}