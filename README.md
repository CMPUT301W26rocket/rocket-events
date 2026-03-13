# rocket-events

This is william testing permissions.


HomeFragment.java       (browse events + QR)

MyEventsFragment.java   (events I'm hosting)

CreateEventFragment.java (+ button)

NotificationsFragment.java

ProfileFragment.java

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
