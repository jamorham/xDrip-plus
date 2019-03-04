package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 20/10/2017.
 *
 * Common interface for Shelf implementations
 */

public interface ViewShelf {

    boolean get(String id) ;
    void set(String id, boolean value) ;
    void pset(String id, boolean value) ;

    void ptoggle(String id);
    //public void populate();

}
