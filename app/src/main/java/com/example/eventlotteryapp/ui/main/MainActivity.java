package com.example.eventlotteryapp.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.ui.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.eventlotteryapp.ui.fragments.MyEventsFragment;
import com.example.eventlotteryapp.ui.fragments.ProfileFragment;
import android.view.View;

/**
 * The main activity for authenticated users, providing bottom navigation between
 * the Home, My Events, Notifications, and Profile tabs.
 *
 * <p>Receives the user's {@code deviceId} from the intent and passes it as arguments
 * to each fragment. The Home tab ({@link HomeFragment}) is loaded by default on first launch.
 */
public class MainActivity extends AppCompatActivity {

    private String deviceId;

    /**
     * Reads the device ID from the intent, hides the action bar, sets up the bottom navigation
     * listener to swap fragments (Home, My Events, Profile) with the device ID passed as arguments,
     * and loads {@link HomeFragment} as the default tab on first launch.
     *
     * @param savedInstanceState previously saved state, or null if none
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceId = getIntent().getStringExtra("deviceId");

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        // Set up bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                HomeFragment homeFragment = new HomeFragment();
                Bundle bundle = new Bundle();
                bundle.putString("deviceId", deviceId);
                homeFragment.setArguments(bundle);
                selectedFragment = homeFragment;

            } else if (itemId == R.id.nav_my_events) {
                MyEventsFragment myEventsFragment = new MyEventsFragment();
                Bundle bundle = new Bundle();
                bundle.putString("deviceId", deviceId);
                myEventsFragment.setArguments(bundle);
                selectedFragment = myEventsFragment;

            } else if (itemId == R.id.nav_notifications) {
                // selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_profile) {
                ProfileFragment profileFragment = new ProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("deviceId", deviceId);
                profileFragment.setArguments(bundle);
                selectedFragment = profileFragment;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }

            return false;
        });

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            HomeFragment homeFragment = new HomeFragment();
            Bundle bundle = new Bundle();
            bundle.putString("deviceId", deviceId);
            homeFragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .commit();
        }
    }
}
