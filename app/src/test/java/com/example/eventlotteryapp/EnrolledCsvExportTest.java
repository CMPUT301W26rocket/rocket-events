package com.example.eventlotteryapp;

import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.ui.fragments.EntrantListFragment;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link EntrantListFragment#buildEnrolledCsv}.
 *
 * <p>The CSV export is used by organizers to download a list of enrolled entrants.
 * These tests verify that the output is correctly formatted so the file opens
 * properly in spreadsheet tools like Excel or Google Sheets.
 * @author Leyla
 */
public class EnrolledCsvExportTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Entrant makeEntrant(String deviceId) {
        Entrant e = new Entrant();
        e.setDeviceId(deviceId);
        e.setStatus(Entrant.STATUS_ENROLLED);
        return e;
    }

    private String csv(List<Entrant> entrants, Map<String, String> names) {
        return EntrantListFragment.buildEnrolledCsv(entrants, names);
    }

    // -----------------------------------------------------------------------
    // Header row
    // -----------------------------------------------------------------------

    /**
     * The first line must always be the header row "Name,Device ID".
     * Spreadsheet tools rely on this to label the columns.
     */
    @Test
    public void output_alwaysStartsWithHeaderRow() {
        String result = csv(Collections.emptyList(), null);
        String firstLine = result.split("\n")[0];
        assertEquals("Name,Device ID", firstLine);
    }

    /**
     * Even when the enrolled list is empty, the header row must be present.
     */
    @Test
    public void emptyList_outputIsJustHeaderRow() {
        String result = csv(Collections.emptyList(), null);
        assertEquals("Name,Device ID\n", result);
    }

    // -----------------------------------------------------------------------
    // Data rows
    // -----------------------------------------------------------------------

    /**
     * A single enrolled entrant whose name is resolved must appear as
     * "Name,deviceId" on the second line.
     */
    @Test
    public void singleEntrant_withResolvedName_appearsOnDataRow() {
        Map<String, String> names = new HashMap<>();
        names.put("device_alice", "Alice Smith");

        String result = csv(
                Collections.singletonList(makeEntrant("device_alice")),
                names);

        String[] lines = result.split("\n");
        assertEquals(2, lines.length);              // header + 1 data row
        assertEquals("Alice Smith,device_alice", lines[1]);
    }

    /**
     * When there is no name entry for an entrant, the device ID must be used
     * in the Name column too. This prevents blank cells in the exported file.
     */
    @Test
    public void entrant_withNoResolvedName_fallsBackToDeviceId() {
        String result = csv(
                Collections.singletonList(makeEntrant("device_unknown")),
                new HashMap<>());   // empty map — no name for this device

        String[] lines = result.split("\n");
        assertEquals("device_unknown,device_unknown", lines[1]);
    }

    /**
     * When the names map is null entirely, every entrant's device ID is used
     * as their name. This prevents a NullPointerException.
     */
    @Test
    public void nullNamesMap_fallsBackToDeviceId_forAllEntrants() {
        String result = csv(
                Collections.singletonList(makeEntrant("device_abc")),
                null);

        String[] lines = result.split("\n");
        assertEquals("device_abc,device_abc", lines[1]);
    }

    /**
     * Multiple entrants must each appear on their own line in the same order
     * they appear in the input list.
     */
    @Test
    public void multipleEntrants_eachOnSeparateLine_inInputOrder() {
        Map<String, String> names = new HashMap<>();
        names.put("device_1", "Alice");
        names.put("device_2", "Bob");
        names.put("device_3", "Carol");

        String result = csv(
                Arrays.asList(
                        makeEntrant("device_1"),
                        makeEntrant("device_2"),
                        makeEntrant("device_3")),
                names);

        String[] lines = result.split("\n");
        assertEquals(4, lines.length);             // header + 3 data rows
        assertEquals("Alice,device_1", lines[1]);
        assertEquals("Bob,device_2",   lines[2]);
        assertEquals("Carol,device_3", lines[3]);
    }

    // -----------------------------------------------------------------------
    // CSV escaping — logic issues the TA would look for
    // -----------------------------------------------------------------------

    /**
     * A name that contains a comma must be wrapped in double-quotes.
     * Without this, the comma would be interpreted as a column separator,
     * corrupting the file layout in any spreadsheet tool.
     */
    @Test
    public void nameWithComma_isWrappedInDoubleQuotes() {
        Map<String, String> names = new HashMap<>();
        names.put("device_x", "Smith, Alice");

        String result = csv(
                Collections.singletonList(makeEntrant("device_x")),
                names);

        String[] lines = result.split("\n");
        assertEquals("\"Smith, Alice\",device_x", lines[1]);
    }

    /**
     * A name that contains a double-quote character must have the quote escaped
     * by doubling it ("" instead of "), per RFC 4180.
     * Without this, the file would be malformed and could crash parsers.
     */
    @Test
    public void nameWithDoubleQuote_isEscapedByDoubling() {
        Map<String, String> names = new HashMap<>();
        names.put("device_x", "Alice \"Al\" Smith");

        String result = csv(
                Collections.singletonList(makeEntrant("device_x")),
                names);

        String[] lines = result.split("\n");
        // The quote is escaped; if the name also had a comma it would be wrapped,
        // but here there's no comma so no outer quotes needed
        assertEquals("Alice \"\"Al\"\" Smith,device_x", lines[1]);
    }

    /**
     * A name that contains BOTH a comma and a double-quote must be wrapped in
     * outer quotes AND have its inner quote escaped — both rules apply together.
     */
    @Test
    public void nameWithCommaAndDoubleQuote_isEscapedAndWrapped() {
        Map<String, String> names = new HashMap<>();
        names.put("device_x", "O'Brien, \"Pat\"");

        String result = csv(
                Collections.singletonList(makeEntrant("device_x")),
                names);

        String[] lines = result.split("\n");
        // Inner quote escaped first → O'Brien, ""Pat""
        // Name contains comma → wrap in outer quotes → "O'Brien, ""Pat"""
        assertEquals("\"O'Brien, \"\"Pat\"\"\",device_x", lines[1]);
    }

    /**
     * A plain name with no commas or quotes must NOT be wrapped in double-quotes.
     * Unnecessary quoting is valid CSV but can confuse some tools.
     */
    @Test
    public void plainName_isNotWrappedInQuotes() {
        Map<String, String> names = new HashMap<>();
        names.put("device_x", "Alice Smith");

        String result = csv(
                Collections.singletonList(makeEntrant("device_x")),
                names);

        String[] lines = result.split("\n");
        assertFalse(lines[1].startsWith("\""));
    }
}
