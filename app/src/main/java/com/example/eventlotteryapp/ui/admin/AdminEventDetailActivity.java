package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Comment;
import com.example.eventlotteryapp.repository.CommentRepository;
import com.example.eventlotteryapp.repository.EventRepository;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Admin screen for viewing event details, moderating event comments,
 * and deleting an event.
 *
 * User Stories Implemented:
 * US 03.01.01 As an administrator, I want to be able to remove events.
 * US 03.10.01 As an administrator, I want to remove event comments that violate app policy.
 *
 * @author Mazen
 */
public class AdminEventDetailActivity extends AppCompatActivity {

    private TextView textEventId;
    private TextView textEventTitle;
    private TextView textEventDescription;
    private TextView textOrganizerId;
    private TextView textNoComments;

    private LinearLayout commentsContainer;

    private Button btnDeleteEvent;
    private Button btnBack;

    private EventRepository eventRepository;
    private CommentRepository commentRepository;

    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_detail);

        textEventId = findViewById(R.id.textEventId);
        textEventTitle = findViewById(R.id.textEventTitle);
        textEventDescription = findViewById(R.id.textEventDescription);
        textOrganizerId = findViewById(R.id.textOrganizerId);
        textNoComments = findViewById(R.id.text_no_comments);

        commentsContainer = findViewById(R.id.comments_container);

        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        btnBack = findViewById(R.id.btnBack);

        eventRepository = new EventRepository();
        commentRepository = new CommentRepository();

        eventId = getIntent().getStringExtra("eventId");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String organizerId = getIntent().getStringExtra("organizerId");

        textEventId.setText(eventId == null || eventId.isEmpty() ? "No event ID" : eventId);
        textEventTitle.setText(title == null || title.isEmpty() ? "No title" : title);
        textEventDescription.setText(description == null || description.isEmpty() ? "No description" : description);
        textOrganizerId.setText(organizerId == null || organizerId.isEmpty() ? "No organizer ID" : organizerId);

        btnDeleteEvent.setOnClickListener(v -> showDeleteEventConfirmation());
        btnBack.setOnClickListener(v -> finish());

        loadComments();
    }

    /**
     * Loads all comments for the current event and displays them in the comments container.
     */
    private void loadComments() {
        if (eventId == null || eventId.isEmpty()) {
            textNoComments.setText("Missing event ID. Cannot load comments.");
            textNoComments.setVisibility(View.VISIBLE);
            return;
        }

        commentsContainer.removeAllViews();
        commentsContainer.addView(textNoComments);
        textNoComments.setText("Loading comments...");
        textNoComments.setVisibility(View.VISIBLE);

        commentRepository.getCommentsForEvent(eventId, new CommentRepository.FirestoreCallback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> comments) {
                commentsContainer.removeAllViews();

                if (comments == null || comments.isEmpty()) {
                    textNoComments.setText("No comments yet.");
                    textNoComments.setVisibility(View.VISIBLE);
                    commentsContainer.addView(textNoComments);
                    return;
                }

                textNoComments.setVisibility(View.GONE);

                for (Comment comment : comments) {
                    commentsContainer.addView(createCommentView(comment));
                }
            }

            @Override
            public void onFailure(Exception e) {
                commentsContainer.removeAllViews();
                textNoComments.setText("Failed to load comments.");
                textNoComments.setVisibility(View.VISIBLE);
                commentsContainer.addView(textNoComments);

                Toast.makeText(
                        AdminEventDetailActivity.this,
                        "Failed to load comments: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Creates one admin comment view with comment text and a delete button.
     *
     * @param comment the comment to render
     * @return a fully built view for the comment
     */
    private View createCommentView(Comment comment) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        wrapperParams.bottomMargin = dpToPx(12);
        wrapper.setLayoutParams(wrapperParams);
        wrapper.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        TextView authorView = new TextView(this);
        authorView.setText("Author ID: " + safeText(comment.getAuthorId()));
        authorView.setTypeface(null, Typeface.BOLD);
        authorView.setTextSize(14f);

        TextView timeView = new TextView(this);
        timeView.setText("Time: " + formatTimestamp(comment));
        timeView.setTextSize(12f);

        TextView commentTextView = new TextView(this);
        commentTextView.setText(safeText(comment.getText()));
        commentTextView.setTextSize(15f);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.topMargin = dpToPx(8);
        commentTextView.setLayoutParams(textParams);

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete Comment");

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dpToPx(10);
        deleteButton.setLayoutParams(buttonParams);

        deleteButton.setOnClickListener(v -> showDeleteCommentConfirmation(comment));

        wrapper.addView(authorView);
        wrapper.addView(timeView);
        wrapper.addView(commentTextView);
        wrapper.addView(deleteButton);

        return wrapper;
    }

    /**
     * Shows a confirmation dialog before deleting a comment.
     *
     * @param comment the comment to delete
     */
    private void showDeleteCommentConfirmation(Comment comment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to remove this comment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteComment(comment))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes a selected comment and refreshes the comment list.
     *
     * @param comment the comment to delete
     */
    private void deleteComment(Comment comment) {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment == null || comment.getCommentId() == null || comment.getCommentId().isEmpty()) {
            Toast.makeText(this, "Missing comment ID", Toast.LENGTH_SHORT).show();
            return;
        }

        commentRepository.deleteComment(eventId, comment.getCommentId(),
                new CommentRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(
                                AdminEventDetailActivity.this,
                                "Comment deleted",
                                Toast.LENGTH_SHORT
                        ).show();
                        loadComments();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(
                                AdminEventDetailActivity.this,
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    /**
     * Shows a confirmation dialog before deleting the selected event.
     */
    private void showDeleteEventConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the selected event through the shared event repository.
     */
    private void deleteEvent() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepository.deleteEvent(eventId, new EventRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(AdminEventDetailActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        AdminEventDetailActivity.this,
                        "Delete failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Safely formats comment text values for display.
     *
     * @param value input string
     * @return display-safe text
     */
    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "(empty)" : value;
    }

    /**
     * Formats a comment timestamp for display.
     *
     * @param comment the comment
     * @return formatted timestamp text
     */
    private String formatTimestamp(Comment comment) {
        if (comment == null || comment.getTimestamp() == null || comment.getTimestamp().toDate() == null) {
            return "Unknown time";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return formatter.format(comment.getTimestamp().toDate());
    }

    /**
     * Converts dp to px.
     *
     * @param dp density-independent pixels
     * @return pixel value
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}