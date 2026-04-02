package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.ImageRepository;

/**
 * Admin screen for viewing and removing an event poster image.
 * Displays the poster associated with a selected event and allows
 * the administrator to permanently remove it from both Firebase Storage
 * and the event record in Firestore.
 *
 * User Stories Implemented:
 * US 03.03.01 As an administrator, I want to be able to remove images.
 *
 * @author Mazen
 */
public class AdminImageDetailActivity extends AppCompatActivity {

    private ImageView imagePoster;
    private TextView textTitle;
    private Button btnRemoveImage;
    private ImageButton buttonBack;

    private EventRepository eventRepository;
    private ImageRepository imageRepository;

    private String eventId;
    private String posterUrl;
    private String title;

    /**
     * Initializes the image detail screen, reads the event and poster data from
     * the intent, displays the image, and sets up button listeners.
     *
     * @param savedInstanceState saved Android instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_image_detail);

        imagePoster = findViewById(R.id.imagePoster);
        textTitle = findViewById(R.id.textTitle);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        buttonBack = findViewById(R.id.button_back);

        eventRepository = new EventRepository();
        imageRepository = new ImageRepository();

        eventId = getIntent().getStringExtra("eventId");
        posterUrl = getIntent().getStringExtra("posterUrl");
        title = getIntent().getStringExtra("title");

        textTitle.setText(title == null || title.isEmpty() ? "Untitled Event" : title);

        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imagePoster);
        } else {
            imagePoster.setImageResource(R.drawable.ic_image_placeholder);
        }

        btnRemoveImage.setOnClickListener(v -> showRemoveConfirmation());
        buttonBack.setOnClickListener(v -> finish());
    }

    /**
     * Shows a confirmation dialog before removing the current event poster.
     */
    private void showRemoveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Image")
                .setMessage("Are you sure you want to remove this event poster?")
                .setPositiveButton("Remove", (dialog, which) -> removeImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Removes the event poster by deleting the image from Firebase Storage,
     * then clearing the poster URL from the related event document in Firestore.
     */
    private void removeImage() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (posterUrl == null || posterUrl.isEmpty()) {
            Toast.makeText(this, "Missing poster URL", Toast.LENGTH_SHORT).show();
            return;
        }

        imageRepository.deleteImageByUrl(posterUrl, new ImageRepository.UploadCallback() {
            @Override
            public void onSuccess(String ignored) {
                eventRepository.getEventById(eventId, new EventRepository.FirestoreCallback<Event>() {
                    @Override
                    public void onSuccess(Event event) {
                        if (event == null) {
                            Toast.makeText(AdminImageDetailActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        event.setPosterUrl("");

                        eventRepository.updateEvent(event, new EventRepository.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(AdminImageDetailActivity.this, "Image removed", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(
                                        AdminImageDetailActivity.this,
                                        "Failed to update event: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(
                                AdminImageDetailActivity.this,
                                "Failed to load event: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        AdminImageDetailActivity.this,
                        "Failed to delete image: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}