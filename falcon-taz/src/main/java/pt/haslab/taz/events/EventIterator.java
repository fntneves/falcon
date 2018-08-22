package pt.haslab.taz.events;

/**
 * Created by joaopereira
 */

import pt.haslab.taz.causality.CausalPair;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

public class EventIterator
                implements Iterator<Event>
{
    private PriorityQueue<CausalPair<Iterator<Event>, Event>> eventHeap;

    //keeps track of the last Iterator next'd for the remove method
    private Iterator<Event> lastIt;

    private final Comparator<CausalPair<Iterator<Event>, Event>> heapOrder =
                    new Comparator<CausalPair<Iterator<Event>, Event>>()
                    {
                        //doesnt support null arguments
                        public int compare( CausalPair<Iterator<Event>, Event> o1,
                                            CausalPair<Iterator<Event>, Event> o2 )
                        {
                            return (int) ( o1.getSecond().getEventId() - o2.getSecond().getEventId() );
                        }
                    };

    public EventIterator(Collection<? extends Iterable<Event>> eventIterables)
    {
        //Holds the first value of a list and an iterator of the rest of the list
        eventHeap = new PriorityQueue<>(eventIterables.size(), heapOrder);

        for ( Iterable<Event> eventIterable : eventIterables )
        {
            if (eventIterable != null)
            {
                Iterator<Event> it = eventIterable.iterator();
                if (it.hasNext())
                {
                    Event firstElem = it.next();
                    eventHeap.add(new CausalPair<Iterator<Event>, Event>(it, firstElem));
                }
            }
        }
    }

    public boolean hasNext() {
        return !eventHeap.isEmpty() || ( lastIt != null && lastIt.hasNext());
    }

    public Event next() {
        if ( !this.hasNext() ) {
            throw new NoSuchElementException( "There are no more elements" );
        }

        if ( lastIt != null && lastIt.hasNext() ) {
            Event toInsert = lastIt.next();
            eventHeap.add(new CausalPair<Iterator<Event>, Event>(lastIt, toInsert));
        }

        CausalPair<Iterator<Event>, Event> heapTop = eventHeap.poll();

        Iterator<Event> it = heapTop.getFirst();
        Event next = heapTop.getSecond();
        lastIt = it;

        return next;
    }

    public void remove()
    {
        if ( lastIt == null )
        {
            throw new IllegalStateException();
        }
        //removes the last returned element
        lastIt.remove();
        lastIt = null;
    }

}
