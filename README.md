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

1. Clone the repo and open in Android Studio
2. Connect to Firebase — place your `google-services.json` in `app/`
3. Deploy Firestore indexes (see below)
4. Run on an emulator or physical device (API 26+)

---

## Firestore Index Management

Firestore indexes are tracked in `firestore.indexes.json` at the project root. Any query that uses a collection group scope or composite filter requires an index to be deployed before it will work.

### Prerequisites

Install the Firebase CLI and log in:

```bash
npm install -g firebase-tools
firebase login
```

### Deploy indexes

Run this after pulling any branch that modifies `firestore.indexes.json`:

```bash
firebase deploy --only firestore:indexes --project eventlotteryapp-24fc3
```

### View currently deployed indexes

```bash
firebase firestore:indexes --project eventlotteryapp-24fc3
```

### Freeze current live indexes to file

```bash
firebase firestore:indexes --project eventlotteryapp-24fc3 > firestore.indexes.json
```

### When you need a new index

If you add a Firestore query using `collectionGroup(...)`, multiple `where` clauses on different fields, or `orderBy` on a non-filtered field, Firestore will fail with `FAILED_PRECONDITION`. Filter Logcat by `Firestore` to find the error — it will describe the required index. Add it to `firestore.indexes.json` and run the deploy command above.
