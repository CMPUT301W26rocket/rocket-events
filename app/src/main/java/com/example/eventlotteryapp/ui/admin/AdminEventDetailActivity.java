package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Comment;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.CommentRepository;
import com.example.eventlotteryapp.repository.EventRepository;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminEventDetailActivity extends AppCompatActivity {

    private TextView textDetailTitle;
    private TextView textDetailOrganizerName;
    private TextView textDetailOrganizerId;
    private TextView textDetailDescription;
    private TextView textDetailEventId;
    private TextView textNoComments;

    private LinearLayout commentsContainer;

    private ImageButton buttonBack;
    private Button btnDeleteEvent;

    private EventRepository eventRepository;
    private CommentRepository commentRepository;

    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_detail);

        textDetailTitle = findViewById(R.id.text_detail_title);
        textDetailOrganizerName = findViewById(R.id.text_detail_organizer_name);
        textDetailOrganizerId = findViewById(R.id.text_detail_organizer_id);
        textDetailDescription = findViewById(R.id.text_detail_description);
        textDetailEventId = findViewById(R.id.text_detail_event_id);
        textNoComments = findViewById(R.id.text_no_comments);

        commentsContainer = findViewById(R.id.comments_container);

        buttonBack = findViewById(R.id.button_back);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);

        eventRepository = new EventRepository();
        commentRepository = new CommentRepository();

        eventId = getIntent().getStringExtra("eventId");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String organizerId = getIntent().getStringExtra("organizerId");

        textDetailTitle.setText(title == null || title.isEmpty() ? "No title" : title);
        textDetailOrganizerName.setText("Organizer");
        textDetailOrganizerId.setText("Organizer ID: " + safeText(organizerId));
        textDetailDescription.setText(description == null || description.isEmpty() ? "No description" : description);
        textDetailEventId.setText("Event ID: " + safeText(eventId));

        buttonBack.setOnClickListener(v -> finish());
        btnDeleteEvent.setOnClickListener(v -> showDeleteEventConfirmation());

        loadEventDetails();
        loadComments();
    }

    private void loadEventDetails() {
        if (eventId == null || eventId.isEmpty()) {
            return;
        }

        eventRepository.getEventById(eventId, new EventRepository.FirestoreCallback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null) {
                    return;
                }

                textDetailTitle.setText(safeText(event.getTitle()));
                textDetailOrganizerName.setText(safeText(event.getOrganizerName()));
                textDetailOrganizerId.setText("Organizer ID: " + safeText(event.getOrganizerId()));
                textDetailDescription.setText(safeText(event.getDescription()));
                textDetailEventId.setText("Event ID: " + safeText(eventId));
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        AdminEventDetailActivity.this,
                        "Failed to load event details: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

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

    private View createCommentView(Comment comment) {
        View commentView = LayoutInflater.from(this).inflate(
                R.layout.item_admin_comment,
                commentsContainer,
                false
        );

        TextView textCommentAuthor = commentView.findViewById(R.id.textCommentAuthor);
        TextView textCommentTime = commentView.findViewById(R.id.textCommentTime);
        TextView textCommentBody = commentView.findViewById(R.id.textCommentBody);
        Button btnDeleteComment = commentView.findViewById(R.id.btnDeleteComment);

        textCommentAuthor.setText("Author ID: " + safeText(comment.getAuthorId()));
        textCommentTime.setText("Time: " + formatTimestamp(comment));
        textCommentBody.setText(safeText(comment.getText()));

        btnDeleteComment.setOnClickListener(v -> showDeleteCommentConfirmation(comment));

        return commentView;
    }

    private void showDeleteCommentConfirmation(Comment comment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to remove this comment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteComment(comment))
                .setNegativeButton("Cancel", null)
                .show();
    }

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

    private void showDeleteEventConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

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

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "(empty)" : value;
    }

    private String formatTimestamp(Comment comment) {
        if (comment == null || comment.getTimestamp() == null || comment.getTimestamp().toDate() == null) {
            return "Unknown time";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return formatter.format(comment.getTimestamp().toDate());
    }
}