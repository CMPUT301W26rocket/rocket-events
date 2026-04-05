package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.Comment;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Comment} model class.
 * Covers getters/setters and the authorId/authorDeviceId sync behaviour,
 * which ensures compatibility between the older admin flow and the newer comment UI.
 * @author Leyla
 */
public class CommentTest {

    private Comment comment;

    @Before
    public void setUp() {
        comment = new Comment();
        comment.setCommentId("comment123");
        comment.setEventId("event456");
        comment.setAuthorId("device789");
        comment.setAuthorName("Jane Smith");
        comment.setText("Looking forward to this event!");
    }

    // --- Default constructor ---

    @Test
    public void emptyConstructor_allFieldsNull() {
        Comment empty = new Comment();
        assertNull(empty.getCommentId());
        assertNull(empty.getEventId());
        assertNull(empty.getAuthorId());
        assertNull(empty.getAuthorDeviceId());
        assertNull(empty.getAuthorName());
        assertNull(empty.getText());
        assertNull(empty.getTimestamp());
    }

    // --- Basic setters ---

    @Test
    public void setCommentId_updatesCommentId() {
        comment.setCommentId("newComment999");
        assertEquals("newComment999", comment.getCommentId());
    }

    @Test
    public void setEventId_updatesEventId() {
        comment.setEventId("newEvent111");
        assertEquals("newEvent111", comment.getEventId());
    }

    @Test
    public void setAuthorName_updatesAuthorName() {
        comment.setAuthorName("John Doe");
        assertEquals("John Doe", comment.getAuthorName());
    }

    @Test
    public void setText_updatesText() {
        comment.setText("Updated comment text");
        assertEquals("Updated comment text", comment.getText());
    }

    // --- authorId / authorDeviceId sync ---
    // These two fields are kept in sync so the comment works in both
    // the admin flow (uses authorId) and the comment UI (uses authorDeviceId)

    @Test
    public void setAuthorId_alsoUpdatesAuthorDeviceId() {
        // Setting authorId should keep authorDeviceId in sync
        comment.setAuthorId("deviceA");
        assertEquals("deviceA", comment.getAuthorId());
        assertEquals("deviceA", comment.getAuthorDeviceId());
    }

    @Test
    public void setAuthorDeviceId_alsoUpdatesAuthorId() {
        // Setting authorDeviceId should keep authorId in sync
        comment.setAuthorDeviceId("deviceB");
        assertEquals("deviceB", comment.getAuthorDeviceId());
        assertEquals("deviceB", comment.getAuthorId());
    }

    @Test
    public void getAuthorId_fallsBackToAuthorDeviceId_whenAuthorIdIsNull() {
        // If only authorDeviceId is set (e.g. older Firestore doc), getAuthorId() still works
        Comment c = new Comment();
        c.setAuthorDeviceId("deviceOnly");
        assertEquals("deviceOnly", c.getAuthorId());
    }

    @Test
    public void getAuthorDeviceId_fallsBackToAuthorId_whenAuthorDeviceIdIsNull() {
        // If only authorId is set (e.g. older Firestore doc), getAuthorDeviceId() still works
        Comment c = new Comment();
        c.setAuthorId("idOnly");
        assertEquals("idOnly", c.getAuthorDeviceId());
    }
}
