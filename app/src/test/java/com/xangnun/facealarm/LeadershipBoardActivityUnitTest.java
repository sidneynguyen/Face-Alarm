package com.xangnun.facealarm;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by sidney on 11/27/16.
 */

public class LeadershipBoardActivityUnitTest {
    @Test
    public void construction_isCorrect() throws Exception {
        LeadershipBoardActivity.CustomComparator cmp = new LeadershipBoardActivity.CustomComparator();

        FacebookUser a = new FacebookUser("1", "a", 0);
        FacebookUser b = new FacebookUser("2", "b", 1);
        FacebookUser c = new FacebookUser("3", "c", 1);

        assertEquals(cmp.compare(a, b), 1);
        assertEquals(cmp.compare(b, a), -1);
        assertEquals(cmp.compare(b, c), 0);
    }
}
