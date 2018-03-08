package pt.haslab.causalSolver.events;

import java.util.Comparator;

/**
 * Created by nunomachado on 19/10/17.
 */
public class TimestampComparator implements Comparator<TimestampedEvent> {

    public int compare(TimestampedEvent o1, TimestampedEvent o2) {
        Long ts1 = Long.valueOf(o1.getTimestamp());
        Long ts2 = Long.valueOf(o2.getTimestamp());

        return ts1.compareTo(ts2);
    }

    public boolean equals(Object obj) {
        if(this == obj)
            return true;

        return false;
    }
}
