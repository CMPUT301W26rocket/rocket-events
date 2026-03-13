package com.example.eventlotteryapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlotteryapp.models.Event;
import com.example.eventlotteryapp.models.User;
import com.example.eventlotteryapp.repository.EventRepository;
import com.example.eventlotteryapp.repository.UserRepository;
import com.example.eventlotteryapp.ui.fragments.CreateEventFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.Date;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * UI tests for {@link CreateEventFragment}.
 *
 * Two categories of tests:
 * - Validation tests: fill the form incorrectly and check error messages appear.
 *   These don't need mocking because validation runs before any Firebase call.
 * - Save tests: fill the form correctly and check the fragment behaves correctly
 *   after a successful or failed save. These mock UserRepository and EventRepository.
 */
@RunWith(AndroidJUnit4.class)
public class CreateEventFragmentTest {

    @Mock EventRepository mockEventRepo;
    @Mock UserRepository mockUserRepo;

    // Dates used in save tests — open yesterday, close tomorrow, event next week
    private Date eventStartDate;
    private Date regOpenDate;
    private Date regCloseDate;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        eventStartDate = new Date(System.currentTimeMillis() + 7 * 86400000L);  // next week
        regOpenDate    = new Date(System.currentTimeMillis() - 86400000L);       // yesterday
        regCloseDate   = new Date(System.currentTimeMillis() + 86400000L);       // tomorrow
    }

    /**
     * Launches CreateEventFragment.
     *
     * @param injectDates  if true, pre-sets the three date fields via setters so
     *                     tests don't need to interact with DatePickerDialog
     * @param injectMocks  if true, injects mock repositories so Firebase is never called
     */
    private void launch(boolean injectDates, boolean injectMocks) {
        Bundle args = new Bundle();
        args.putString("deviceId", "device123");

        FragmentFactory factory = new FragmentFactory() {
            @Override
            public Fragment instantiate(ClassLoader classLoader, String className) {
                CreateEventFragment fragment = new CreateEventFragment();

                if (injectMocks) {
                    fragment.setEventRepository(mockEventRepo);
                    fragment.setUserRepository(mockUserRepo);
                }

                // Pre-set dates so tests don't need to open DatePickerDialogs
                if (injectDates) {
                    fragment.setSelectedEventStartDate(eventStartDate);
                    fragment.setSelectedRegistrationOpenDate(regOpenDate);
                    fragment.setSelectedRegistrationCloseDate(regCloseDate);
                }

                return fragment;
            }
        };

        FragmentScenario.launchInContainer(CreateEventFragment.class, args, R.style.Theme_EventLotteryApp, factory);
    }

    // -----------------------------------------------------------------------
    // Display tests
    // -----------------------------------------------------------------------

    @Test
    public void allMainFields_areDisplayed() {
        launch(false, false);

        // scrollTo() brings each field into the viewport before checking isDisplayed()
        // because the form is inside a ScrollView and fields below the fold are off-screen
        onView(withId(R.id.edit_event_title)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.edit_event_description)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.edit_event_location)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.edit_lottery_capacity)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.button_create_event)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void createButton_isEnabledOnLaunch() {
        launch(false, false);

        onView(withId(R.id.button_create_event)).perform(scrollTo()).check(matches(isEnabled()));
    }

    // -----------------------------------------------------------------------
    // Validation tests — no mocking needed, validation runs before Firebase
    // -----------------------------------------------------------------------

    @Test
    public void submit_withEmptyTitle_showsTitleError() {
        launch(false, false);

        // Leave title empty and click submit
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        // Fragment should set an error on the title field
        onView(withId(R.id.edit_event_title))
                .check(matches(hasErrorText("Title is required")));
    }

    @Test
    public void submit_withEmptyLocation_showsLocationError() {
        launch(false, false);

        // Fill title but leave location empty
        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        onView(withId(R.id.edit_event_location))
                .check(matches(hasErrorText("Location is required")));
    }

    @Test
    public void submit_withEmptyCapacity_showsCapacityError() {
        launch(false, false);

        // Fill title and location but leave capacity empty
        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        onView(withId(R.id.edit_lottery_capacity))
                .check(matches(hasErrorText("Enrollment capacity is required")));
    }

    @Test
    public void submit_withZeroCapacity_showsCapacityError() {
        // Dates must be injected — validation checks dates before checking capacity value
        launch(true, false);

        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        // Capacity of 0 is invalid
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("0"), closeSoftKeyboard());
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        onView(withId(R.id.edit_lottery_capacity))
                .check(matches(hasErrorText("Capacity must be greater than 0")));
    }

    @Test
    public void submit_withNegativeFee_showsFeeError() {
        // injectDates=true so we get past the date validation and reach fee validation
        launch(true, false);

        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("50"), closeSoftKeyboard());
        // Enter a negative fee
        onView(withId(R.id.edit_registration_fee))
                .perform(scrollTo(), replaceText("-5"), closeSoftKeyboard());
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        onView(withId(R.id.edit_registration_fee))
                .check(matches(hasErrorText("Fee cannot be negative")));
    }

    @Test
    public void submit_withNoEventStartDate_showsDateError() {
        launch(false, false);

        // Fill required text fields but don't set any dates
        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("50"), closeSoftKeyboard());
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        // No date was set via DatePicker, so selectedEventStartDate is still null
        onView(withId(R.id.edit_event_start_date))
                .check(matches(hasErrorText("Event start date is required")));
    }

    @Test
    public void submit_withWaitlistLimitChecked_emptyLimit_showsLimitError() {
        // injectDates=true so we pass all earlier validations and reach the waitlist check
        launch(true, false);

        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("50"), closeSoftKeyboard());

        // Check the "has waitlist limit" checkbox but leave the limit field empty
        onView(withId(R.id.checkbox_has_waitlist_limit)).perform(scrollTo(), click());
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        onView(withId(R.id.edit_waitlist_limit))
                .check(matches(hasErrorText("Waitlist limit is required when enabled")));
    }

    @Test
    public void submit_withWaitlistLimitChecked_zeroLimit_showsLimitError() {
        launch(true, false);

        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("50"), closeSoftKeyboard());
        onView(withId(R.id.checkbox_has_waitlist_limit)).perform(scrollTo(), click());
        // Enter 0 as waitlist limit — invalid
        onView(withId(R.id.edit_waitlist_limit))
                .perform(scrollTo(), replaceText("0"), closeSoftKeyboard());
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        onView(withId(R.id.edit_waitlist_limit))
                .check(matches(hasErrorText("Waitlist limit must be greater than 0")));
    }

    // -----------------------------------------------------------------------
    // Save tests — mock UserRepository and EventRepository
    // -----------------------------------------------------------------------

    @Test
    public void submit_withValidForm_clearsFormOnSuccess() {
        // Mock getUser() to return a fake user with a name
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            User user = new User("device123", "Jane Smith", "jane@email.com", "780-555-0000");
            cb.onSuccess(user);
            return null;
        }).when(mockUserRepo).getUser(any(), any());

        // Mock addEvent() to immediately succeed
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<String> cb = invocation.getArgument(1);
            cb.onSuccess("newEventId123"); // Firestore returns the new event ID
            return null;
        }).when(mockEventRepo).addEvent(any(), any());

        // Launch with dates pre-set and mocks injected
        launch(true, true);

        // Fill in all required fields
        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("50"), closeSoftKeyboard());

        // Click submit
        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        // A "Event Created!" success dialog appears with a QR code — dismiss it
        onView(withId(R.id.button_done)).perform(click());

        // After dismissing the dialog, clearForm() has run — title field should be empty
        onView(withId(R.id.edit_event_title)).check(matches(withText("")));
    }

    @Test
    public void submit_withValidForm_reEnablesButtonOnFailure() {
        // Mock getUser() to succeed
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            cb.onSuccess(new User("device123", "Jane Smith", "jane@email.com", "780-555-0000"));
            return null;
        }).when(mockUserRepo).getUser(any(), any());

        // Mock addEvent() to FAIL — simulates a Firestore write error
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<String> cb = invocation.getArgument(1);
            cb.onFailure(new Exception("Firestore error"));
            return null;
        }).when(mockEventRepo).addEvent(any(), any());

        launch(true, true);

        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton"), closeSoftKeyboard());
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("50"), closeSoftKeyboard());

        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        // On failure, createButton.setEnabled(true) is called — button should be clickable again
        onView(withId(R.id.button_create_event)).check(matches(isEnabled()));
    }

    @Test
    public void submit_withValidForm_passesCorrectEventDataToRepository() {
        // ArgumentCaptor intercepts the Event object passed to addEvent()
        // so we can inspect its fields after the call
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // Mock getUser() to return a fake organizer
        doAnswer(invocation -> {
            UserRepository.FirestoreCallback<User> cb = invocation.getArgument(1);
            cb.onSuccess(new User("device123", "Jane Smith", "jane@email.com", "780-555-0000"));
            return null;
        }).when(mockUserRepo).getUser(any(), any());

        // Mock addEvent() to succeed AND capture the Event it receives
        // eventCaptor.capture() tells Mockito to record whatever Event is passed in
        doAnswer(invocation -> {
            EventRepository.FirestoreCallback<String> cb = invocation.getArgument(1);
            cb.onSuccess("newEventId123");
            return null;
        }).when(mockEventRepo).addEvent(eventCaptor.capture(), any());

        launch(true, true);

        // Fill the form
        onView(withId(R.id.edit_event_title))
                .perform(scrollTo(), replaceText("Spring Festival"), closeSoftKeyboard());
        onView(withId(R.id.edit_event_location))
                .perform(scrollTo(), replaceText("Edmonton Convention Centre"), closeSoftKeyboard());
        onView(withId(R.id.edit_lottery_capacity))
                .perform(scrollTo(), replaceText("100"), closeSoftKeyboard());
        onView(withId(R.id.edit_registration_fee))
                .perform(scrollTo(), replaceText("25.50"), closeSoftKeyboard());

        onView(withId(R.id.button_create_event)).perform(scrollTo(), click());

        // verify() confirms addEvent() was actually called exactly once
        // If the form was blocked by a bug before reaching the save, this would fail
        verify(mockEventRepo).addEvent(any(), any());

        // Now inspect the captured Event object — its fields should match the form input
        Event saved = eventCaptor.getValue();
        assertEquals("Spring Festival", saved.getTitle());
        assertEquals("Edmonton Convention Centre", saved.getLocation());
        assertEquals(100, saved.getLotteryCapacity());
        assertEquals(25.50, saved.getRegistrationFee(), 0.001);

        // Check organizer info was correctly pulled from the mocked user
        assertEquals("Jane Smith", saved.getOrganizerName());
        assertEquals("device123", saved.getOrganizerId());
    }
}
