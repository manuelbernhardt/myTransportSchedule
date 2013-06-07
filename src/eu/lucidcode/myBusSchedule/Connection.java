package eu.lucidcode.myBusSchedule;

import java.util.Date;
import java.util.concurrent.TimeUnit;

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

    public Long getDepartureMinutes() {
        long duration = start.getTime() - System.currentTimeMillis();
        if (duration > 0) {
            return TimeUnit.MILLISECONDS.toMinutes(duration);
        } else {
            return 0l;
        }
    }
}
