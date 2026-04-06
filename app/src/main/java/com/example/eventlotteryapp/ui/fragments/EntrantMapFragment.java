package com.example.eventlotteryapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.repository.EntrantRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a Google Map with markers showing where STATUS_WAITLIST entrants joined the event from.
 * Only shown for events that have geolocation required. Entrants who joined without location data
 * (permission denied) are silently omitted from the map.
 *
 * <p><b>Note on testability:</b> This fragment is not covered by instrumented UI tests for two reasons:
 * <ol>
 *   <li>{@code SupportMapFragment} is declared as a {@code <fragment>} XML tag, which causes
 *       {@code FragmentScenario.launchInContainer} to enter infinite recursion in the fragment
 *       lifecycle callback dispatcher, resulting in a {@code StackOverflowError}.</li>
 *   <li>{@link com.google.android.gms.maps.GoogleMap} is a final SDK class that cannot be mocked
 *       with Mockito, and {@link #onMapReady} fires asynchronously based on Google Play Services
 *       initialisation, making the marker and camera logic unreachable from tests.</li>
 * </ol>
 *
 * User Stories Implemented:
 * US 02.02.02 As an organizer I want to see on a map where my entrants joined the waiting list from.
 *
 * @author Leyla
 */
public class EntrantMapFragment extends Fragment implements OnMapReadyCallback {

    private String eventId;
    private String eventTitle;
    private EntrantRepository entrantRepository;
    private GoogleMap googleMap;

    /** Required empty public constructor. */
    public EntrantMapFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_entrant_map, container, false);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            eventId    = getArguments().getString("eventId");
            eventTitle = getArguments().getString("eventTitle", "Event");
        }

        if (entrantRepository == null) entrantRepository = new EntrantRepository();

        SupportMapFragment mapFrag = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }

        return view;
    }

    /**
     * Called when the map is ready. Loads all STATUS_WAITLIST entrants for the event
     * and places a marker at each entrant's recorded join location. Entrants without
     * location data are skipped.
     *
     * @param map the ready {@link GoogleMap} instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        entrantRepository.getEntrantsByStatus(eventId, Entrant.STATUS_WAITLIST,
                new EntrantRepository.FirestoreCallback<List<Entrant>>() {
                    @Override
                    public void onSuccess(List<Entrant> entrants) {
                        if (!isAdded() || googleMap == null) return;
                        plotMarkers(entrants);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(getContext(),
                                    "Failed to load entrant locations", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Places markers on the map for each entrant that has recorded latitude/longitude data.
     * Zooms the camera to fit all markers; falls back to a world view if none have coordinates.
     *
     * @param entrants all STATUS_WAITLIST entrants for the event
     */
    private void plotMarkers(List<Entrant> entrants) {
        List<LatLng> points = new ArrayList<>();

        for (Entrant e : entrants) {
            if (e.getLatitude() != null && e.getLongitude() != null) {
                LatLng pos = new LatLng(e.getLatitude(), e.getLongitude());
                googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(e.getDeviceId())
                        .icon(BitmapDescriptorFactory.defaultMarker(291f)));
                points.add(pos);
            }
        }

        if (points.isEmpty()) {
            Toast.makeText(getContext(),
                    "No location data recorded for waitlist entrants.", Toast.LENGTH_LONG).show();
            return;
        }

        if (points.size() == 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 12f));
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng p : points) boundsBuilder.include(p);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
    }
}
