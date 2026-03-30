Closes #42 #43 #45 #47 #29 #31 #33 #34

---

**#42 US 01.04.01 / #29 US 02.05.01 — Win Notification**
A "Send Win Notification" button was added to the Invited tab in the organizer's entrant list view. Pressing it creates a notification document under `users/{deviceId}/notifications` for each invited entrant containing the event name and a win message. The button is disabled with a count of 0 if no invited entrants exist. On the entrant side, the notification appears in the new Notifications tab (Tab 3) and tapping it navigates to the event details page where they can accept or decline.

**#43 US 01.04.02 — Loss Notification**
A "Send Loss Notification" button was added to the Waitlist tab. It only sends to entrants with `STATUS_NOT_SELECTED` (i.e. those who went through the lottery and lost), not those still actively waitlisted. The button is disabled until the lottery has been run and not_selected entrants exist.

**#45 US 01.05.01 / #31 US 02.05.03 — Replacement Draw**
A "Draw Replacement" button was added to the Invited tab. It randomly selects one entrant from the pool of `STATUS_NOT_SELECTED` or `STATUS_WAITLIST` entrants (those who went through the lottery but weren't picked), updates their status to `INVITED` in Firestore, sends them a replacement notification, and reloads the list. The button is gated behind two conditions: lottery completed and registration period closed. While either condition is unmet the button is disabled and its label indicates which condition is blocking (e.g. "Draw Replacement (Lottery Not Run)" or "Draw Replacement (Registration Open)").

**#47 US 01.05.02 — Accept Invitation**
When an entrant taps a win or replacement notification in the Notifications tab they are navigated to the event details page. If their status is `INVITED` the action button reads "Invited" and tapping it opens an accept/decline dialog. Accepting sets their status to `ENROLLED` and disables the button permanently.

**#33 US 02.07.02 — Notify All Selected Entrants**
A separate "Send Notification" button was added to the Invited tab alongside the win notification button. It sends a general pending invitation reminder to all currently invited entrants. Disabled if no invited entrants exist.

**#34 US 02.07.03 — Notify Cancelled Entrants**
A "Send Notification" button was wired up in the Cancelled tab. It sends a general notification to all cancelled entrants. Disabled if the cancelled list is empty.

**Cancelled tab now shows declined entrants**
Entrants who declined their own invitation (`STATUS_DECLINED`) now appear in the Cancelled tab alongside organizer-cancelled entrants (`STATUS_CANCELLED`). Each card is annotated with "(Declined)" or "(Cancelled)" so the organizer can distinguish between the two.

---

Known gaps:
- US 01.04.03 opt-out: `notificationsEnabled` flag exists on User and the ProfileFragment toggle writes it to Firestore, but `addNotification` does not yet check it before writing (TODO comment left in NotificationRepository). will decide to fix this after TA gets back to us about what notifications to opt in/out of
- US 02.07.01 (#32): the waitlist notify button reaches both `not_selected` and `waitlist` entrants.
- All organizer notification buttons send hardcoded messages. The acceptance criteria for these stories require a custom message input prompt before sending.
