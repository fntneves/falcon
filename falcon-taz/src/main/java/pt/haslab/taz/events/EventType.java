package pt.haslab.taz.events;

/**
 * Created by nunomachado on 05/03/18.
 */
public enum EventType
{
    //thread events
    CREATE( "CREATE" ), //01
    START( "START" ),   //02
    END( "END" ),       //03
    JOIN( "JOIN" ),     //04
    LOG( "LOG" ),       //05

    //access events
    READ( "R" ),        //06
    WRITE( "W" ),       //07

    //communication events
    SND( "SND" ),               //08
    RCV( "RCV" ),               //09
    CLOSE( "CLOSE" ),           //10
    CONNECT( "CONNECT" ),       //11
    ACCEPT( "ACCEPT" ),         //12
    SHUTDOWN( "SHUTDOWN" ),     //13

    //message handlers
    HNDLBEG( "HANDLERBEGIN" ),  //14
    HNDLEND( "HANDLEREND" ),    //15

    // lock and unlock events
    LOCK( "LOCK" ),             //16
    UNLOCK( "UNLOCK" ),         //17

    //thread synchronization events
    WAIT( "WAIT" ),             //18
    NOTIFY( "NOTIFY" ),         //19
    NOTIFYALL( "NOTIFYALL" );   //20

    private final String desc;

    private EventType( String l )
    {
        this.desc = l;
    }

    @Override
    public String toString()
    {
        return this.desc;
    }

    /**
     * Translates a string representing the type of event
     * the corresponding EventType enum element.
     *
     * @param type
     * @return
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
}
