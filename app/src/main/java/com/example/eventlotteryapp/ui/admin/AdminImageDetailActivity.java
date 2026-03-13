package com.example.eventlotteryapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.ImageRepository;

public class AdminImageDetailActivity extends AppCompatActivity {

    private ImageView imagePoster;
    private TextView textTitle;
    private Button btnRemoveImage;
    private Button btnBack;

    private EventRepository eventRepository;
    private ImageRepository imageRepository;

    private String eventId;
    private String posterUrl;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_image_detail);

        imagePoster = findViewById(R.id.imagePoster);
        textTitle = findViewById(R.id.textTitle);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        btnBack = findViewById(R.id.btnBack);

        eventRepository = new EventRepository();
        imageRepository = new ImageRepository();

        eventId = getIntent().getStringExtra("eventId");
        posterUrl = getIntent().getStringExtra("posterUrl");
        title = getIntent().getStringExtra("title");

        textTitle.setText(title == null || title.isEmpty() ? "Untitled Event" : title);

        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this).load(posterUrl).into(imagePoster);
        }

        btnRemoveImage.setOnClickListener(v -> showRemoveConfirmation());
        btnBack.setOnClickListener(v -> finish());
    }

    private void showRemoveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Image")
                .setMessage("Are you sure you want to remove this event poster?")
                .setPositiveButton("Remove", (dialog, which) -> removeImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

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