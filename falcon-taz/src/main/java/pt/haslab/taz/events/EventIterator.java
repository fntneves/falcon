package pt.haslab.taz.events;

import java.util.Collection;

public class EventIterator extends CatIterator<Event> {
    public EventIterator (Collection<? extends Iterable<Event>> eventIterables) {
       super(eventIterables);
    }
}
