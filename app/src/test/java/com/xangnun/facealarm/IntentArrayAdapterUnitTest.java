package com.xangnun.facealarm;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by sidney on 11/27/16.
 */

public class IntentArrayAdapterUnitTest {
    @Test
    public void toHour_isCorrect() throws Exception {
        assertEquals(IntentArrayAdapter.toHour("5"), "05");
        assertEquals(IntentArrayAdapter.toHour("10"), "10");
        assertEquals(IntentArrayAdapter.toHour("0"), "12");
    }

    @Test
    public void toMinute_isCorrect() throws Exception {
        assertEquals(IntentArrayAdapter.toMinute("5"), "05");
        assertEquals(IntentArrayAdapter.toMinute("23"), "23");
        assertEquals(IntentArrayAdapter.toMinute("0"), "00");
    }
}
