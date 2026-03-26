package com.example.eventlotteryapp.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eventlotteryapp.R;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.google.zxing.BarcodeFormat;

import java.util.Collections;

/**
 * Full-screen in-app QR code scanner using the ZXing embedded barcode view.
 * Scans continuously until a result is found, then delivers it back to the
 * calling fragment via the Fragment Result API under the key {@link #REQUEST_KEY}
 * with the scanned string in {@link #RESULT_CONTENT}.
 * User Stories Implemented:
 * US 01.06.01 As an entrant I want to view event details within the app by scanning the promotional QR code.
 * @author Daniel
 */
public class QrScannerFragment extends Fragment {

    public static final String REQUEST_KEY = "qr_scan_request";
    public static final String RESULT_CONTENT = "content";

    private DecoratedBarcodeView barcodeView;
    private boolean resultDelivered = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    barcodeView.resume();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scanner, container, false);

        barcodeView = view.findViewById(R.id.barcode_scanner);
        barcodeView.setStatusText("Point camera at an event QR code");
        barcodeView.getBarcodeView().setDecoderFactory(
                new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));

        barcodeView.decodeContinuous(result -> {
            if (resultDelivered) return;
            resultDelivered = true;

            Bundle bundle = new Bundle();
            bundle.putString(RESULT_CONTENT, result.getText());
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, bundle);
            getParentFragmentManager().popBackStack();
        });

        View closeButton = view.findViewById(R.id.button_close_scanner);
        closeButton.bringToFront();
        closeButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resultDelivered = false;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
