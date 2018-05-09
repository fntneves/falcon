package pt.haslab.taz.events;

/**
 * Created by nunomachado on 05/03/18.
 * Represents a message handler delimiter.
 * This class is associated with the event types HANDLERBEGIN and HANDLEREND.
 */
public class HandlerEvent
                extends Event
{

    public HandlerEvent( String timestamp, EventType type, String thread, int eventNumber, String lineOfCode )
    {
        super( timestamp, type, thread, eventNumber, lineOfCode );
    }

    public HandlerEvent( Event e )
    {
        super( e );
    }

    public String toString()
    {
        String res = type + "_" + thread + "_" + eventId + "@" + loc;
        return res;
    }

}
