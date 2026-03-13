package com.example.eventlotteryapp;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

/**
 * NOT a standalone activity — do NOT run this directly.
 *
 * <p>This is a test-only helper used exclusively by {@link NavigationTest}.
 * It provides a real {@code R.id.fragment_container} so that fragments which call
 * {@code requireActivity().getSupportFragmentManager().replace(R.id.fragment_container, ...)}
 * can actually navigate during tests. {@code FragmentScenario}'s built-in container does
 * not have this ID, which is why this activity exists.
 *
 * <p>To run navigation tests, run {@link NavigationTest} instead.
 */
public class NavigationTestContainerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.fragment_container);
        container.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(container);
    }
}
