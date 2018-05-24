package pt.haslab.taz.events;

/**
 * This class encodes the event types supported by falcon-taz.
 * Created by nunomachado on 05/03/18.
 */
public enum EventType
{
    // Thread events
    CREATE( "CREATE", 1 ),
    START( "START", 2 ),
    END( "END", 3 ),
    JOIN( "JOIN", 4 ),
    LOG( "LOG", 5 ),

    // Variable access events
    READ( "R", 6 ),
    WRITE( "W", 7 ),

    // Socket communication events
    SND( "SND", 8 ),
    RCV( "RCV", 9 ),
    CLOSE( "CLOSE", 10 ),
    CONNECT( "CONNECT", 11 ),
    ACCEPT( "ACCEPT", 12 ),
    SHUTDOWN( "SHUTDOWN", 13 ),

    // Message handlers delimiters
    HNDLBEG( "HANDLERBEGIN", 14 ),
    HNDLEND( "HANDLEREND", 15 ),

    // Locking events
    LOCK( "LOCK", 16 ),
    UNLOCK( "UNLOCK", 17 ),

    // Thread synchronization events
    WAIT( "WAIT", 18 ),
    NOTIFY( "NOTIFY", 19 ),
    NOTIFYALL( "NOTIFYALL", 20 );

    /* Textual description of the event type. */
    private final String desc;

    /* Integer code representing the event type. */
    private final int code;

    private EventType( String name, int identifier )
    {
        this.desc = name;
        this.code = identifier;
    }

    @Override
    public String toString()
    {
        return this.desc;
    }

    public int getCode()
    {
        return this.code;
    }

    /**
     * Translates a string representing the type of event into
     * the corresponding EventType enum element.
     *
     * @param type  the type of Event expressed by its textual name.
     * @return      the corresponding EventType object.
     */
    public static EventType getEventType( String type )
    {
        if ( type.equals( "CREATE" ) )
            return EventType.CREATE;
        else if ( type.equals( "START" ) )
            return EventType.START;
        else if ( type.equals( "END" ) )
            return EventType.END;
        else if ( type.equals( "JOIN" ) )
            return EventType.JOIN;
        else if ( type.equals( "R" ) )
            return EventType.READ;
        else if ( type.equals( "W" ) )
            return EventType.WRITE;
        else if ( type.equals( "SND" ) )
            return EventType.SND;
        else if ( type.equals( "RCV" ) )
            return EventType.RCV;
        else if ( type.equals( "CONNECT" ) )
            return EventType.CONNECT;
        else if ( type.equals( "ACCEPT" ) )
            return EventType.ACCEPT;
        else if ( type.equals( "CLOSE" ) )
            return EventType.CLOSE;
        else if ( type.equals( "SHUTDOWN" ) )
            return EventType.SHUTDOWN;
        else if ( type.equals( "HANDLERBEGIN" ) )
            return EventType.HNDLBEG;
        else if ( type.equals( "HANDLEREND" ) )
            return EventType.HNDLEND;
        else if ( type.equals( "LOCK" ) )
            return EventType.LOCK;
        else if ( type.equals( "UNLOCK" ) )
            return EventType.UNLOCK;
        else if ( type.equals( "WAIT" ) )
            return EventType.WAIT;
        else if ( type.equals( "NOTIFY" ) )
            return EventType.NOTIFY;
        else if ( type.equals( "NOTIFYALL" ) )
            return EventType.NOTIFYALL;
        else if ( type.equals( "LOG" ) )
            return EventType.LOG;
        else
            return null;
    }


    /**
     * Translates a integer value representing the type of event into
     * the corresponding EventType enum element.
     *
     * @param type  the type of Event expressed as an integer.
     * @return      the corresponding EventType object.
     */
    public static EventType getEventType( int type )
    {
        if ( type == 1 )
            return EventType.CREATE;
        else if ( type== 2 )
            return EventType.START;
        else if ( type == 3 )
            return EventType.END;
        else if ( type == 4 )
            return EventType.JOIN;
        else if ( type == 5 )
            return EventType.LOG;
        else if ( type == 6 )
            return EventType.READ;
        else if ( type == 7 )
            return EventType.WRITE;
        else if ( type ==  8 )
            return EventType.SND;
        else if ( type == 9 )
            return EventType.RCV;
        else if ( type == 10 )
            return EventType.CLOSE;
        else if ( type == 11 )
            return EventType.CONNECT;
        else if ( type == 12 )
            return EventType.ACCEPT;
        else if ( type == 13 )
            return EventType.SHUTDOWN;
        else if ( type == 14 )
            return EventType.HNDLBEG;
        else if ( type == 15 )
            return EventType.HNDLEND;
        else if ( type == 16 )
            return EventType.LOCK;
        else if ( type == 17 )
            return EventType.UNLOCK;
        else if ( type == 18 )
            return EventType.WAIT;
        else if ( type == 19 )
            return EventType.NOTIFY;
        else if ( type == 20 )
            return EventType.NOTIFYALL;
        else
            return null;
    }
}
