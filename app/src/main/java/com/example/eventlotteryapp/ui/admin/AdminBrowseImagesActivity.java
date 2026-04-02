package com.example.eventlotteryapp.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for browsing event posters currently stored in the system.
 * Only events with a non-empty poster URL are shown.
 * User Stories Implemented:
 * US 03.06.01 As an administrator, I want to be able to browse images that are uploaded so I can remove them if necessary.
 *
 * @author Mazen
 */
public class AdminBrowseImagesActivity extends AppCompatActivity {

    private RecyclerView recyclerImages;
    private TextView textEmptyImages;
    private ImageButton buttonBack;
    private EventRepository eventRepository;
    private List<Event> imageEventList;
    private AdminImageAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_images);

        recyclerImages = findViewById(R.id.recyclerImages);
        textEmptyImages = findViewById(R.id.textEmptyImages);
        buttonBack = findViewById(R.id.button_back);

        eventRepository = new EventRepository();
        imageEventList = new ArrayList<>();

        imageAdapter = new AdminImageAdapter(imageEventList, event -> {
            Intent intent = new Intent(AdminBrowseImagesActivity.this, AdminImageDetailActivity.class);
            intent.putExtra("eventId", event.getEventId());
            intent.putExtra("title", event.getTitle());
            intent.putExtra("posterUrl", event.getPosterUrl());
            startActivity(intent);
        });

        recyclerImages.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerImages.setAdapter(imageAdapter);

        buttonBack.setOnClickListener(v -> finish());

        loadImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }

    private void loadImages() {
        eventRepository.getAllEvents(new EventRepository.FirestoreCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> result) {
                imageEventList.clear();

                for (Event event : result) {
                    if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                        imageEventList.add(event);
                    }
                }

                imageAdapter.notifyDataSetChanged();

                if (imageEventList.isEmpty()) {
                    textEmptyImages.setVisibility(View.VISIBLE);
                    recyclerImages.setVisibility(View.GONE);
                } else {
                    textEmptyImages.setVisibility(View.GONE);
                    recyclerImages.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        AdminBrowseImagesActivity.this,
                        "Failed to load images: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}