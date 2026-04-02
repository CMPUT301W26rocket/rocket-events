package com.example.eventlotteryapp.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Admin screen for reviewing notification logs sent to entrants.
 * This activity loads notification documents from all user notification
 * subcollections, converts them into display items, sorts them newest first,
 * and shows them in a RecyclerView.
 *
 * User Stories Implemented:
 * US 03.08.01 As an administrator, I want to review logs of all notifications
 * sent to entrants by organizers.
 *
 * @author Mazen
 */

public class AdminNotificationLogsActivity extends AppCompatActivity {

    private RecyclerView recyclerNotificationLogs;
    private TextView textEmptyNotificationLogs;
    private ImageButton buttonBack;

    private FirebaseFirestore db;
    private List<AdminNotificationLogItem> logItems;
    private AdminNotificationLogAdapter logAdapter;

    /**
     * Initializes the notification log screen, sets up the RecyclerView,
     * and loads all notification log entries from Firestore.
     *
     * @param savedInstanceState saved Android instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_logs);

        recyclerNotificationLogs = findViewById(R.id.recyclerNotificationLogs);
        textEmptyNotificationLogs = findViewById(R.id.textEmptyNotificationLogs);
        buttonBack = findViewById(R.id.button_back);

        db = FirebaseFirestore.getInstance();
        logItems = new ArrayList<>();
        logAdapter = new AdminNotificationLogAdapter(logItems, this::showLogDetails);

        recyclerNotificationLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotificationLogs.setAdapter(logAdapter);

        buttonBack.setOnClickListener(v -> finish());

        loadNotificationLogs();
    }
    /**
     * Reloads notification logs when the activity returns to the foreground.
     * This keeps the displayed log list up to date after navigating back
     * from other admin screens.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadNotificationLogs();
    }
    /**
     * Displays the full details for one notification log entry in a dialog.
     *
     * @param item selected notification log item
     */
    private void showLogDetails(AdminNotificationLogItem item) {
        String title = item.getEventTitle() == null || item.getEventTitle().trim().isEmpty()
                ? "Notification Log"
                : item.getEventTitle();

        String recipient = item.getRecipientDeviceId() == null || item.getRecipientDeviceId().trim().isEmpty()
                ? "(unknown)"
                : item.getRecipientDeviceId();

        String type = item.getType() == null || item.getType().trim().isEmpty()
                ? "general"
                : item.getType();

        String message = item.getMessage() == null || item.getMessage().trim().isEmpty()
                ? "No message"
                : item.getMessage();

        String time = "Unknown time";
        if (item.getCreatedAt() != null && item.getCreatedAt().toDate() != null) {
            java.text.SimpleDateFormat formatter =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            time = formatter.format(item.getCreatedAt().toDate());
        }

        String details =
                "Recipient: " + recipient + "\n\n" +
                        "Type: " + type + "\n\n" +
                        "Time: " + time + "\n\n" +
                        "Message:\n" + message;

        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }
    /**
     * Loads all notification documents from every user's notifications subcollection,
     * converts them into display items, sorts them by timestamp in descending order,
     * and updates the list UI.
     */
    private void loadNotificationLogs() {
        db.collectionGroup("notifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    logItems.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification == null) {
                            continue;
                        }

                        String recipientDeviceId = "(unknown)";
                        if (doc.getReference().getParent() != null
                                && doc.getReference().getParent().getParent() != null) {
                            recipientDeviceId = doc.getReference().getParent().getParent().getId();
                        }

                        logItems.add(new AdminNotificationLogItem(
                                recipientDeviceId,
                                notification.getEventTitle(),
                                notification.getType(),
                                notification.getMessage(),
                                notification.getCreatedAt()
                        ));
                    }

                    Collections.sort(logItems, (a, b) -> {
                        Timestamp ta = a.getCreatedAt();
                        Timestamp tb = b.getCreatedAt();

                        long va = ta != null && ta.toDate() != null ? ta.toDate().getTime() : 0L;
                        long vb = tb != null && tb.toDate() != null ? tb.toDate().getTime() : 0L;

                        return Long.compare(vb, va);
                    });

                    logAdapter.notifyDataSetChanged();

                    if (logItems.isEmpty()) {
                        textEmptyNotificationLogs.setVisibility(View.VISIBLE);
                        recyclerNotificationLogs.setVisibility(View.GONE);
                    } else {
                        textEmptyNotificationLogs.setVisibility(View.GONE);
                        recyclerNotificationLogs.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    String message = e instanceof FirebaseFirestoreException
                            ? e.getMessage()
                            : "Unknown error";

                    Toast.makeText(
                            AdminNotificationLogsActivity.this,
                            "Failed to load notification logs: " + message,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}