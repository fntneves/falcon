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
    private PriorityQueue<CausalPair<ListIterator<Event>, Event>> eventHeap;

    //keeps track of the last Iterator next'd for the remove method
    private ListIterator<Event> lastIt;

    public final Comparator<CausalPair<ListIterator<Event>, Event>> heapOrder =
                    new Comparator<CausalPair<ListIterator<Event>, Event>>()
                    {
                        //doesnt support null arguments
                        public int compare( CausalPair<ListIterator<Event>, Event> o1,
                                            CausalPair<ListIterator<Event>, Event> o2 )
                        {
                            return (int) (o1.getSecond().getEventId() - o2.getSecond().getEventId());
                        }
                    };

    public EventIterator( Collection<List<Event>> eventLists )
    {
        //Holds the first value of a list and an iterator of the rest of the list
        eventHeap = new PriorityQueue<CausalPair<ListIterator<Event>, Event>>( eventLists.size(), heapOrder );

        for ( List<Event> eventList : eventLists )
        {
            if ( !eventList.isEmpty() )
            {
                ListIterator<Event> it = eventList.listIterator();
                Event firstElem = it.next();
                eventHeap.add( new CausalPair<ListIterator<Event>, Event>( it, firstElem ) );
            }
        }
    }

    public boolean hasNext()
    {
        return !eventHeap.isEmpty();
    }

    public Event next()
    {
        if ( !this.hasNext() )
        {
            throw new NoSuchElementException( "There are no more elements" );
        }
        CausalPair<ListIterator<Event>, Event> heapTop = eventHeap.poll();

        ListIterator<Event> it = heapTop.getFirst();
        Event next = heapTop.getSecond();
        lastIt = it;

        if ( it.hasNext() )
        {
            Event toInsert = it.next();
            eventHeap.add( new CausalPair<ListIterator<Event>, Event>( it, toInsert ) );
        }
        return next;
    }

    public void remove()
    {
        if ( lastIt == null )
        {
            throw new IllegalStateException();
        }
        //puts the cursor before the to be next element
        lastIt.previous();
        //puts the cursor before the last returned element
        lastIt.previous();
        //removes the last returned element
        lastIt.remove();
        //puts the cursor in its original position
        lastIt.next();
        lastIt = null;
    }
}
