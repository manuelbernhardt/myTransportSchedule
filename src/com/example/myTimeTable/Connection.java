package com.example.myTimeTable;

import java.util.Date;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class Connection {

    public final String vehicle;
    public final Date start;
    public final Date end;

    public Connection(String vehicle, Date start, Date end) {
        this.vehicle = vehicle;
        this.start = start;
        this.end = end;
    }
}
