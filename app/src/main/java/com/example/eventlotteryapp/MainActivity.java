package com.example.eventlotteryapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceId = getIntent().getStringExtra("deviceId");

        // Set up bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_my_events) {
                // selectedFragment = new MyEventsFragment();
            } else if (itemId == R.id.nav_create) {
                // selectedFragment = new CreateEventFragment();
            } else if (itemId == R.id.nav_notifications) {
                // selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_profile) {
                // selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Load default fragment (Home)
        // getSupportFragmentManager().beginTransaction()
        //         .replace(R.id.fragment_container, new HomeFragment())
        //         .commit();
    }
}