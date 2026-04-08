# Rocket Events

An Android event lottery app that lets organizers create events with configurable waitlists and run fair lotteries to select entrants. Built with Java and Firebase Firestore.

## Features

- **Entrants** can browse events by QR code or event list, join/leave waitlists, and accept or decline lottery invitations
- **Organizers** can create events with custom capacity, registration periods, waitlist limits, and geolocation requirements, then run a lottery to select winners
- **Admins** can browse and remove events, user profiles, organizers, and event posters

## Tech Stack

- Java (Android SDK)
- Firebase Firestore (database)
- Firebase Storage (event poster images)
- Glide (image loading)
- ZXing (QR code scanning)
- Espresso + Mockito (UI and unit testing)

## Project Structure

```
app/src/main/java/com/example/eventlotteryapp/
├── models/          # User, Event, Entrant
├── repository/      # FirebaseConnector, UserRepository, EventRepository, EntrantRepository, ImageRepository
├── ui/
│   ├── auth/        # SplashActivity, ProfileSetupActivity
│   ├── main/        # MainActivity (bottom nav)
│   ├── fragments/   # HomeFragment, MyEventsFragment, CreateEventFragment,
│   │                  EntrantEventDetailsFragment, OrganizerEventDetailsFragment,
│   │                  EntrantListFragment, EventHistoryFragment, QrScannerFragment
│   ├── adapters/    # EventAdapter, EventHistoryAdapter
│   └── admin/       # AdminActivity and all admin browse/detail screens
└── utils/           # QRCodeUtils
```

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (bundled with Android Studio)
- Android device or emulator running API 26 (Android 8.0) or higher
- A Firebase project with Firestore and Storage enabled

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/CMPUT301W26rocket/rocket-events.git
   ```
   Then open the project in Android Studio (`File → Open`).

2. **Add Firebase credentials**
   Download `google-services.json` from your Firebase project console
   (`Project Settings → Your apps → google-services.json`) and place it in the `app/` directory.

3. **Deploy Firestore indexes**
   Some queries require composite indexes. Deploy them before first run:
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase deploy --only firestore:indexes --project eventlotteryapp-24fc3
   ```

4. **Run the app**
   Select a device or emulator in Android Studio and click **Run ▶**.
   The app targets API 26+ — make sure your emulator matches.

### Running the tests

- **Unit tests** (model layer, no device needed):
  Right-click `app/src/test` → **Run Tests**

- **Instrumented UI tests** (requires connected device or emulator):
  Right-click `app/src/androidTest` → **Run Tests**

---

> **Firestore indexes:** Some queries require composite indexes to be deployed. See the [wiki](../../wiki) for full index management instructions.
