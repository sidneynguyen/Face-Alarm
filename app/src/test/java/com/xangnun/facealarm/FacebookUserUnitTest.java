package com.xangnun.facealarm;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by sidney on 11/27/16.
 */

public class FacebookUserUnitTest {
    @Test
    public void construction_isCorrect() throws Exception {
        FacebookUser user = new FacebookUser("123", "Joe");
        assertEquals(user.getID(), "123");
        assertEquals(user.getCount(), 0);
        assertEquals(user.getName(), "Joe");
    }

    @Test
    public void countConstruction_isCorrect() throws Exception {
        FacebookUser user = new FacebookUser("123", "Joe", 100);
        assertEquals(user.getID(), "123");
        assertEquals(user.getCount(), 100);
        assertEquals(user.getName(), "Joe");
    }

    @Test
    public void setCount_isCorrect() throws Exception {
        FacebookUser user = new FacebookUser("123", "Joe");
        assertEquals(user.getID(), "123");
        assertEquals(user.getCount(), 0);
        assertEquals(user.getName(), "Joe");

        user.setCount(100);
        assertEquals(user.getCount(), 100);
    }
}
