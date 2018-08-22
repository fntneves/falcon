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

public class CatIterator<T extends Comparable>
                implements Iterator<T>
{
    private PriorityQueue<CausalPair<Iterator<T>, T>> eventHeap;

    //keeps track of the last Iterator next'd for the remove method
    private Iterator<T> lastIt;

    private final Comparator<CausalPair<Iterator<T>, T>> heapOrder =
                    new Comparator<CausalPair<Iterator<T>, T>>()
                    {
                        //doesnt support null arguments
                        public int compare( CausalPair<Iterator<T>, T> o1,
                                            CausalPair<Iterator<T>, T> o2 )
                        {
                            return o1.getSecond().compareTo( o2.getSecond()) ;
                        }
                    };

    public CatIterator(Collection<? extends Iterable<T>> eventIterables)
    {
        //Holds the first value of a list and an iterator of the rest of the list
        eventHeap = new PriorityQueue<CausalPair<Iterator<T>, T>>(eventIterables.size(), heapOrder);

        for ( Iterable<T> eventIterable : eventIterables )
        {
            if (eventIterable != null)
            {
                Iterator<T> it = eventIterable.iterator();
                if (it.hasNext())
                {
                    T firstElem = it.next();
                    eventHeap.add(new CausalPair<Iterator<T>, T>(it, firstElem));
                }
            }
        }
    }

    public boolean hasNext() {
        return !eventHeap.isEmpty() || ( lastIt != null && lastIt.hasNext());
    }

    public T next() {
        if ( !this.hasNext() ) {
            throw new NoSuchElementException( "There are no more elements" );
        }

        if ( lastIt != null && lastIt.hasNext() ) {
            T toInsert = lastIt.next();
            eventHeap.add(new CausalPair<Iterator<T>, T>(lastIt, toInsert));
        }

        CausalPair<Iterator<T>, T> heapTop = eventHeap.poll();

        Iterator<T> it = heapTop.getFirst();
        T next = heapTop.getSecond();
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

        if( lastIt.hasNext() ) {
            T firstElem = lastIt.next();
            eventHeap.add(new CausalPair<Iterator<T>, T>(lastIt, firstElem));
        }

        lastIt = null;
    }

}
