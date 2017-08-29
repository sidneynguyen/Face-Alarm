package com.xangnun.facealarm;

/**
 * Created by normansun on 11/5/16.
 */

public class FacebookUser  {

    private String myName;
    private String myID;
    private int count;

    public FacebookUser(String id, String name) {
        myName = name;
        myID = id;
        count = 0;
    }


    public FacebookUser(String id, String name, int c) {
        myName = name;
        myID = id;
        count = c;
    }


    /* Get Methods */
    String getName() {
        return myName;
    }

    String getID() {
        return myID;
    }

    int getCount() {
        return count;
    }

    /* Setter Methods */
    void setCount(int num) {
        count = num;
    }

}
