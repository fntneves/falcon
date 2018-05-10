package pt.haslab.taz.events;

import java.util.Comparator;

/**
 * Compares two events according to their timestamp.
 * Created by nunomachado on 05/03/18.
 */
public class TimestampComparator
                implements Comparator<Event>
{

    public int compare( Event o1, Event o2 )
    {
        Long ts1 = Long.valueOf( o1.getTimestamp() );
        Long ts2 = Long.valueOf( o2.getTimestamp() );

        return ts1.compareTo( ts2 );
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;

        return false;
    }
}
