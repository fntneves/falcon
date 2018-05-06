package pt.haslab.taz;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.taz.events.Event;
import pt.haslab.taz.events.EventType;
import pt.haslab.taz.events.HandlerEvent;
import pt.haslab.taz.events.LogEvent;
import pt.haslab.taz.events.MyPair;
import pt.haslab.taz.events.RWEvent;
import pt.haslab.taz.events.SocketEvent;
import pt.haslab.taz.events.SyncEvent;
import pt.haslab.taz.events.ThreadCreationEvent;
import pt.haslab.taz.events.TimestampComparator;
import pt.haslab.taz.utils.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created by nunomachado on 05/03/18.
 */
public enum TraceProcessor
{
    /* TraceProcessor is a singleton class, implemented using the single-element enum type approach */
    INSTANCE;

    private static Logger logger = LoggerFactory.getLogger( TraceProcessor.class );

    /* counts the number of events in the trace */
    private int eventNumber = 0;

    /* Map: message id -> pair of events (snd,rcv) */
    public Map<String, MyPair<SocketEvent, SocketEvent>> msgEvents;

    /* Map: rcv event-> list of events of the message handler
     * (list starts with HANDLERBEGIN and ends with HANDLEREND) */
    public Map<SocketEvent, List<Event>> handlerEvents;

    /* set indicating whether a given thread's trace has message handlers */
    private HashSet hasHandlers;

    /* Map: socket id -> pair of events (connect,accept) */
    public Map<String, MyPair<SocketEvent, SocketEvent>> connAcptEvents;

    /* Map: socket id -> pair of events (close,shutdown) */
    public Map<String, MyPair<SocketEvent, SocketEvent>> closeShutEvents;

    /* Map: thread -> list of all events in that thread's execution */
    public Map<String, List<Event>> eventsPerThread;

    /* Map: thread -> list of thread's fork events */
    public Map<String, List<ThreadCreationEvent>> forkEvents;

    /* Map: thread -> list of thread's join events */
    public Map<String, List<ThreadCreationEvent>> joinEvents;

    /* Map: string (event.toString) -> Event object */
    public HashMap<String, Event> eventNameToObject;

    /* Map: mutex variable -> list of pairs of locks/unlocks */
    public Map<String, List<MyPair<SyncEvent, SyncEvent>>> lockEvents;

    /* Map: variable -> list of reads to that variable by all threads */
    public Map<String, List<RWEvent>> readEvents;

    /* Map: variable -> list of writes to that variable by all threads */
    public Map<String, List<RWEvent>> writeEvents;

    /* Map: condition variable -> list of thread's wait events */
    public Map<String, List<SyncEvent>> waitEvents;

    /* Map: condition variable -> list of thread's notify events */
    public Map<String, List<SyncEvent>> notifyEvents;

    /* list with socket events ordered by timestamp */
    public TreeSet<Event> sortedByTimestamp;

    //local variables (only used during parsing, but not necessary afterwards)
    /* Map: socket channel -> pair of event lists ([snd],[rcv]) */
    private Map<String, MyPair<Deque<SocketEvent>, Deque<SocketEvent>>> pendingEventsSndRcv;

    TraceProcessor()
    {
        msgEvents = new HashMap<String, MyPair<SocketEvent, SocketEvent>>();
        lockEvents = new HashMap<String, List<MyPair<SyncEvent, SyncEvent>>>();
        eventsPerThread = new HashMap<String, List<Event>>();
        readEvents = new HashMap<String, List<RWEvent>>();
        writeEvents = new HashMap<String, List<RWEvent>>();
        forkEvents = new HashMap<String, List<ThreadCreationEvent>>();
        joinEvents = new HashMap<String, List<ThreadCreationEvent>>();
        waitEvents = new HashMap<String, List<SyncEvent>>();
        notifyEvents = new HashMap<String, List<SyncEvent>>();
        connAcptEvents = new HashMap<String, MyPair<SocketEvent, SocketEvent>>();
        closeShutEvents = new HashMap<String, MyPair<SocketEvent, SocketEvent>>();
        eventNameToObject = new HashMap<String, Event>();
        sortedByTimestamp = new TreeSet<Event>( new TimestampComparator() );
        pendingEventsSndRcv = new HashMap<String, MyPair<Deque<SocketEvent>, Deque<SocketEvent>>>();
        handlerEvents = new HashMap<SocketEvent, List<Event>>();
        hasHandlers = new HashSet();
    }

    public int getNumberOfEvents()
    {
        return eventNumber;
    }

    public void loadEventTrace( String pathToFile )
                    throws JSONException, IOException
    {

        logger.info( "Loading events from " + pathToFile );

        try
        {
            //Parse event trace as JSON Array
            JSONTokener tokener = new JSONTokener( new FileReader( pathToFile ) );
            JSONArray jsonevents = new JSONArray( tokener );

            for ( int i = 0; i < jsonevents.length(); i++ )
            {
                JSONObject object = jsonevents.getJSONObject( i );
                parseJSONEvent( object );
            }
        }
        catch ( JSONException e )
        {
            String msg = e.getMessage();

            if ( msg.startsWith( "A JSONArray text must start" ) )
            {
                //Parse events as JSON Objects
                logger.error( "Load as JSONArray failed. Try loading as file of JSONObjects" );
                BufferedReader br = new BufferedReader( new FileReader( pathToFile ) );
                String line = br.readLine();
                while ( line != null )
                {
                    try
                    {
                        JSONObject object = new JSONObject( line );
                        parseJSONEvent( object );
                    }
                    catch ( JSONException objError )
                    {
                        logger.error( "Not a JSON object! Ignoring event: " + line );
                    }
                    finally
                    {
                        line = br.readLine();
                    }
                }
            }
            else
            {
                throw e;
            }
        }
        finally
        {
            if ( !hasHandlers.isEmpty() )
            {
                parseMessageHandlers();
            }
        }

        if ( logger.isDebugEnabled() )
        {
            printDataStructures();
        }
        logger.info( "Trace successfully loaded!" );

    }

    public SocketEvent sndFromMessageId( String messageId )
    {
        MyPair<SocketEvent, SocketEvent> pair = msgEvents.get( messageId );
        if ( pair != null )
        {
            SocketEvent send = pair.getFirst();
            if ( send != null && send.getMessageId().equals( messageId ) )
            {
                return send;
            }
        }
        return null;
    }

    public SyncEvent getCorrespondingUnlock( SyncEvent lockEvent )
    {
        String thread = lockEvent.getThread();
        List<MyPair<SyncEvent, SyncEvent>> pairs = lockEvents.get( lockEvent.getVariable() );
        for ( MyPair<SyncEvent, SyncEvent> se : pairs )
        {
            if ( se.getFirst().equals( lockEvent ) )
            {
                return se.getSecond();
            }
        }
        return null;
    }

    public ThreadCreationEvent getCorrespondingJoin( ThreadCreationEvent tce )
    {
        List<ThreadCreationEvent> joins = joinEvents.get( tce.getThread() );
        String childThread = tce.getChildThread();
        if ( joins == null )
            return null;
        for ( ThreadCreationEvent join : joins )
        {
            if ( join != null && childThread.equals( join.getChildThread() ) )
            {
                return join;
            }
        }
        return null;
    }

    private void parseJSONEvent( JSONObject event )
                    throws JSONException
    {
        //required fields
        EventType type = EventType.getEventType( event.getString( "type" ) );

        if ( type == null )
            throw new JSONException( "Unknown event type: " + event.getString( "type" ) );

        String thread = event.getString( "thread" );
        String loc = event.optString( "loc" );
        loc = ( loc == null ) ? "" : loc;
        // consider timestamp to be a long for the moment
        long time = event.getLong( "timestamp" );
        String timestamp = String.valueOf( time );

        eventNumber++;
        Event e = new Event( timestamp, type, thread, eventNumber, loc );

        //optional fields
        e.setDependency( event.optString( "dependency", null ) );
        if ( event.has( "data" ) )
            e.setData( event.optJSONObject( "data" ) );

        //initialize thread map data structures
        if ( !eventsPerThread.containsKey( thread ) )
        {
            eventsPerThread.put( thread, new LinkedList<Event>() );
        }

        //populate data structures
        switch ( type )
        {
            case LOG:
                String msg = event.getString( "message" );
                LogEvent le = new LogEvent( e, msg );
                eventsPerThread.get( thread ).add( le );
                break;
            case CONNECT:
            case ACCEPT:
            case CLOSE:
            case SHUTDOWN:
            case RCV:
            case SND:
                SocketEvent se = new SocketEvent( e );
                String socket = event.getString( "socket" );
                se.setSocket( socket );
                se.setSocketType( event.getString( "socket_type" ) );
                if ( type == EventType.RCV || type == EventType.SND )
                {
                    se.setSrc( event.getString( "src" ) );
                    se.setSrcPort( event.getInt( "src_port" ) );
                    se.setDst( event.getString( "dst" ) );
                    se.setDstPort( event.getInt( "dst_port" ) );
                    se.setSize( event.getInt( "size" ) );
                    se.setMessageId( event.optString( "message", null ) );
                }

                //handle SND and RCV
                if ( type == EventType.SND || type == EventType.RCV )
                {
                    //handle UDP cases by matching message id
                    if ( se.getSocketType() == SocketEvent.SocketType.UDP )
                    {
                        //update existing entry or create one if necessary
                        if ( msgEvents.containsKey( se.getMessageId() ) )
                        {
                            if ( type == EventType.SND )
                            {
                                msgEvents.get( se.getMessageId() ).setFirst( se );
                            }
                            else
                            {
                                msgEvents.get( se.getMessageId() ).setSecond( se );
                            }
                        }
                        else
                        {
                            MyPair<SocketEvent, SocketEvent> pair;
                            if ( type == EventType.SND )
                            {
                                pair = new MyPair<SocketEvent, SocketEvent>( se, null );
                            }
                            else
                            {
                                pair = new MyPair<SocketEvent, SocketEvent>( null, se );
                            }
                            msgEvents.put( se.getMessageId(), pair );
                        }
                    }

                    //handle TCP cases that may not have message id and/or partitioned messages
                    else if ( se.getSocketType() == SocketEvent.SocketType.TCP )
                    {
                        if ( type == EventType.SND )
                            handleSndSocketEvent( se );

                        if ( type == EventType.RCV )
                            handleRcvSocketEvent( se );
                    }
                }
                //handle CONNECT and ACCEPT
                else if ( type == EventType.CONNECT || type == EventType.ACCEPT )
                {
                    if ( connAcptEvents.containsKey( socket ) )
                    {
                        //put the ACCEPT event if pair.second == null or CONNECT otherwise
                        if ( connAcptEvents.get( socket ).getSecond() == null )
                        {
                            connAcptEvents.get( socket ).setSecond( se );
                        }
                        else
                        {
                            connAcptEvents.get( socket ).setFirst( se );
                        }
                    }
                    else
                    {
                        //create a new pair (CONNECT,ACCEPT)
                        MyPair<SocketEvent, SocketEvent> pair = new MyPair<SocketEvent, SocketEvent>( null, null );
                        if ( type == EventType.CONNECT )
                        {
                            pair.setFirst( se );
                        }
                        else
                        {
                            pair.setSecond( se );
                        }
                        connAcptEvents.put( socket, pair );
                    }
                }

                //handle CLOSE and SHUTDOWN
                else if ( type == EventType.CLOSE || type == EventType.SHUTDOWN )
                {
                    if ( closeShutEvents.containsKey( socket ) )
                    {
                        //put the SHUTDOWN event if pair.second == null and thread is different or CLOSE otherwise
                        if ( closeShutEvents.get( socket ).getSecond() == null && type == EventType.SHUTDOWN )
                        {
                            closeShutEvents.get( socket ).setSecond( se );
                        }
                        else
                        {
                            //put close event if there's none yet or overwrite the existing one if it's from the same thread as the shutdown
                            SocketEvent closeEvent = closeShutEvents.get( socket ).getFirst();
                            SocketEvent shutevent = closeShutEvents.get( socket ).getSecond();
                            if ( closeEvent == null
                                            || ( shutevent != null && !shutevent.getThread().equals(
                                            se.getThread() ) ) )
                            {
                                closeShutEvents.get( socket ).setFirst( se );
                            }
                        }
                    }
                    else
                    {
                        //create a new pair (CLOSE,SHUTDOWN)
                        MyPair<SocketEvent, SocketEvent> pair = new MyPair<SocketEvent, SocketEvent>( null, null );
                        if ( type == EventType.CLOSE )
                        {
                            pair.setFirst( se );
                        }
                        else
                        {
                            pair.setSecond( se );
                        }
                        closeShutEvents.put( socket, pair );
                    }
                }

                if ( se.getTimestamp() != null && !se.getTimestamp().equals( "" ) )
                    sortedByTimestamp.add( se );

                eventsPerThread.get( thread ).add( se );
                break;
            case START:
            case END:
                eventsPerThread.get( thread ).add( e );
                break;
            case CREATE:
            case JOIN:
                String child = event.getString( "child" );
                ThreadCreationEvent tce = new ThreadCreationEvent( e );
                tce.setChildThread( child );
                if ( type == EventType.CREATE )
                {
                    if ( !forkEvents.containsKey( thread ) )
                    {
                        forkEvents.put( thread, new LinkedList<ThreadCreationEvent>() );
                    }
                    forkEvents.get( thread ).add( tce );
                }
                else
                {
                    if ( !joinEvents.containsKey( thread ) )
                    {
                        joinEvents.put( thread, new LinkedList<ThreadCreationEvent>() );
                    }
                    joinEvents.get( thread ).add( tce );
                }
                eventsPerThread.get( thread ).add( tce );
                break;
            case WRITE:
            case READ:
                String var = event.getString( "variable" );
                RWEvent rwe = new RWEvent( e );
                rwe.setVariable( var );

                if ( type == EventType.READ )
                {
                    if ( !readEvents.containsKey( var ) )
                    {
                        readEvents.put( var, new LinkedList<RWEvent>() );
                    }
                    readEvents.get( var ).add( rwe );
                }
                else
                {
                    if ( !writeEvents.containsKey( var ) )
                    {
                        writeEvents.put( var, new LinkedList<RWEvent>() );
                    }
                    writeEvents.get( var ).add( rwe );
                }
                eventsPerThread.get( thread ).add( rwe );
                break;
            case HNDLBEG:
            case HNDLEND:
                HandlerEvent he = new HandlerEvent( e );
                eventsPerThread.get( thread ).add( he );
                if ( type == EventType.HNDLBEG )
                    hasHandlers.add( thread );
                break;
            case LOCK:
            case UNLOCK:
                var = event.getString( "variable" );
                SyncEvent syne = new SyncEvent( e );
                syne.setVariable( var );

                if ( type == EventType.LOCK )
                {
                    List<MyPair<SyncEvent, SyncEvent>> pairList = lockEvents.get( var );
                    MyPair<SyncEvent, SyncEvent> pair =
                                    ( pairList != null ) ? pairList.get( pairList.size() - 1 ) : null;
                    if ( pair == null || pair.getSecond() != null )
                    {
                        // Only adds the lock event if the previous lock event has a corresponding unlock
                        // in order to handle Reentrant Locks
                        Utils.insertInMapToLists( lockEvents, var, new MyPair<SyncEvent, SyncEvent>( syne, null ) );
                        eventsPerThread.get( thread ).add( syne );
                    }
                }
                else if ( type == EventType.UNLOCK )
                {
                    // second component is the unlock event associated with the lock
                    List<MyPair<SyncEvent, SyncEvent>> pairList = lockEvents.get( var );
                    MyPair<SyncEvent, SyncEvent> pair =
                                    ( pairList != null ) ? pairList.get( pairList.size() - 1 ) : null;
                    if ( pair == null )
                    {
                        Utils.insertInMapToLists( lockEvents, var, new MyPair<SyncEvent, SyncEvent>( null, syne ) );
                    }
                    else
                    {
                        pair.setSecond( syne );
                    }
                    eventsPerThread.get( thread ).add( syne );
                }
                break;
            case NOTIFY:
            case NOTIFYALL:
            case WAIT:
                var = event.getString( "variable" );
                syne = new SyncEvent( e );
                syne.setVariable( var );
                if ( type == EventType.WAIT )
                {
                    if ( !waitEvents.containsKey( var ) )
                    {
                        waitEvents.put( var, new LinkedList<SyncEvent>() );
                    }
                    waitEvents.get( var ).add( syne );
                }
                else if ( type == EventType.NOTIFY || type == EventType.NOTIFYALL )
                {
                    if ( !notifyEvents.containsKey( var ) )
                    {
                        notifyEvents.put( var, new LinkedList<SyncEvent>() );
                    }
                    notifyEvents.get( var ).add( syne );
                }
                eventsPerThread.get( thread ).add( syne );
                break;
            default:
                throw new JSONException( "Unknown event type: " + type );
        }
    }

    /**
     * Re-iterates through the trace generating the list of events corresponding to each RCV's message handler
     */
    private void parseMessageHandlers()
    {

        for ( String thread : eventsPerThread.keySet() )
        {

            //only check threads that actually have message handlers
            if ( !hasHandlers.contains( thread ) || eventsPerThread.get( thread ).size() <= 1 )
                continue;

            //handle nested message handlers by parsing with two iterators
            int slowIt = 0;
            int fastIt = 0;
            List<Event> threadEvents = eventsPerThread.get( thread );

            for ( slowIt = 0; slowIt < threadEvents.size(); slowIt++ )
            {
                Event e = threadEvents.get( slowIt );

                if ( e.getType() == EventType.RCV && threadEvents.get( slowIt + 1 ).getType() == EventType.HNDLBEG )
                {
                    List<Event> handlerList = new ArrayList<Event>();
                    fastIt = slowIt + 1;
                    Event handlerEvent = threadEvents.get( fastIt );
                    int nestedCounter = 0;
                    while ( handlerEvent.getType() != EventType.HNDLEND
                                    && nestedCounter >= 0
                                    && fastIt < threadEvents.size() )
                    {
                        handlerList.add( handlerEvent );
                        if ( handlerEvent.getType() == EventType.HNDLBEG )
                        {
                            nestedCounter++;
                        }
                        else if ( handlerEvent.getType() == EventType.HNDLEND )
                        {
                            nestedCounter--; //last HANDLEREND will set nestedCounter = -1 and end loop
                        }
                        fastIt++;
                        if ( fastIt < threadEvents.size() )
                            handlerEvent = threadEvents.get( fastIt );
                    }
                    handlerList.add( handlerEvent ); //add HANDLEREND event
                    handlerEvents.put( (SocketEvent) e, handlerList );
                }
            }
        }
    }

    private MyPair<Deque<SocketEvent>, Deque<SocketEvent>> getOrCreatePartialEventsPairs( String socket )
    {
        if ( !pendingEventsSndRcv.containsKey( socket ) )
        {
            Deque<SocketEvent> sndEvents = new ArrayDeque<SocketEvent>();
            Deque<SocketEvent> rcvEvents = new ArrayDeque<SocketEvent>();
            MyPair<Deque<SocketEvent>, Deque<SocketEvent>> pendingEventsPair =
                            new MyPair<Deque<SocketEvent>, Deque<SocketEvent>>( sndEvents, rcvEvents );
            pendingEventsSndRcv.put( socket, pendingEventsPair );
            return pendingEventsPair;
        }

        return pendingEventsSndRcv.get( socket );
    }

    private void handleSndSocketEvent( SocketEvent snd )
    {
        MyPair<Deque<SocketEvent>, Deque<SocketEvent>> eventPairs =
                        getOrCreatePartialEventsPairs( snd.getDirectedSocket() );
        Deque<SocketEvent> sndEvents = eventPairs.getFirst();
        Deque<SocketEvent> rcvEvents = eventPairs.getSecond();

        // When either there's nothing to match this SND event with or there are already
        // SND events to be paired with RCV events, enqueue it immediately.
        if ( rcvEvents.size() == 0 || sndEvents.size() > 0 )
        {
            sndEvents.add( snd );
            return;
        }

        // Pair this SND with the already enqueued RCV events and if even after
        // this match there are bytes remaining, enqueue it in SND list.
        while ( snd.getSize() > 0 && rcvEvents.size() > 0 )
        {
            SocketEvent rcv = rcvEvents.peek();

            if ( snd.getSize() > rcv.getSize() )
            {
                // 1. Dequeue RCV message from partial receives
                rcv = rcvEvents.pop();

                // 2. Decrement read bytes from SND message
                String msgid = computePartitionedMessageId( snd, rcv );
                snd.setSize( snd.getSize() - rcv.getSize() );

                // 3. Associate RCV message with SND message
                msgEvents.put( msgid, new MyPair<SocketEvent, SocketEvent>( snd, rcv ) );
            }
            else
            {
                // 1. Decrement sent bytes from RCV message
                rcv.setSize( rcv.getSize() - snd.getSize() );
                snd.setSize( 0 );

                // 2. Associate RCV message with SND message
                String msgid = computePartitionedMessageId( rcv, snd );
                msgEvents.put( msgid, new MyPair<SocketEvent, SocketEvent>( snd, rcv ) );

                // 3. Remove enqueued RCV if there are no more bytes remaining
                if ( rcv.getSize() < 1 )
                    rcvEvents.pop();
            }
        }

        // Enqueue SND if there are bytes remaining
        if ( snd.getSize() > 0 )
            sndEvents.add( snd );
    }

    private void handleRcvSocketEvent( SocketEvent rcv )
    {
        MyPair<Deque<SocketEvent>, Deque<SocketEvent>> eventPairs =
                        getOrCreatePartialEventsPairs( rcv.getDirectedSocket() );
        Deque<SocketEvent> sndEvents = eventPairs.getFirst();
        Deque<SocketEvent> rcvEvents = eventPairs.getSecond();

        // When either there's nothing to match this RCV message with or there are already
        // RCV messages to be paired with RCV messages, enqueue it immediately.
        if ( sndEvents.size() == 0 || rcvEvents.size() > 0 )
        {
            rcvEvents.add( rcv );
            return;
        }

        // Pair this RCV with the already enqueued SND messages and if even after
        // this match there are bytes remaining, enqueue it in RCV list.
        while ( rcv.getSize() > 0 && sndEvents.size() > 0 )
        {
            SocketEvent snd = sndEvents.peek();

            if ( rcv.getSize() > snd.getSize() )
            {
                // 1. Dequeue SND message from partial receives
                snd = sndEvents.pop();

                // 2. Decrement read bytes from RCV message
                rcv.setSize( rcv.getSize() - snd.getSize() );

                // 3. Associate RCV message with SND message
                String msgid = computePartitionedMessageId( rcv, snd );
                msgEvents.put( msgid, new MyPair<SocketEvent, SocketEvent>( snd, rcv ) );
            }
            else
            {
                // 1. Decrement sent bytes from SND message
                snd.setSize( snd.getSize() - rcv.getSize() );
                rcv.setSize( 0 );

                // 2. Associate RCV message with SND message
                String msgid = computePartitionedMessageId( snd, rcv );
                msgEvents.put( msgid, new MyPair<SocketEvent, SocketEvent>( snd, rcv ) );

                // 3. Remove enqueued SND if there are no more bytes remaining
                if ( snd.getSize() < 1 )
                    sndEvents.pop();
            }
        }

        // Enqueue SND if there are bytes remaining
        if ( rcv.getSize() > 0 )
            rcvEvents.add( rcv );
    }

    /**
     * Augments a socket event with a message id with a suffix ".X" indicating that the event is partitioned
     *
     * @param full - socket event with full message size
     * @param part - socket event with partial message size
     * @return
     */
    private String computePartitionedMessageId( SocketEvent full, SocketEvent part )
    {
        // the message id will be augmented with a suffix ".X" indicating that the event is partitioned
        String msgid = "";
        if ( full.getMessageId() != null )
        {
            msgid = full.getMessageId();
        }
        else
        {
            msgid = String.valueOf( full.getEventNumber() );
            full.setMessageId( msgid );
        }
        msgid = msgid + "." + full.getSize();
        part.setMessageId( msgid );

        return msgid;
    }

    public void printDataStructures()
    {

        StringBuilder debugMsg = new StringBuilder();
        debugMsg.append( "THREAD EVENTS\n" );
        for ( String t : eventsPerThread.keySet() )
        {
            debugMsg.append( "#" + t + "\n" );
            for ( Event e : eventsPerThread.get( t ) )
            {
                debugMsg.append( " " + e.toString() + "\n" );
            }
            debugMsg.append( "\n" );
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "SEND/RECEIVE EVENTS\n" );
        for ( MyPair<SocketEvent, SocketEvent> se : msgEvents.values() )
        {
            debugMsg.append( se.getFirst() + " -> " + se.getSecond() + "\n" );
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "MESSAGE HANDLER EVENTS\n" );
        for ( SocketEvent rcv : handlerEvents.keySet() )
        {
            debugMsg.append( "#" + rcv.toString() + "\n" );
            for ( Event e : handlerEvents.get( rcv ) )
            {
                debugMsg.append( " " + e.toString() + "\n" );
            }
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "CONNECT/ACCEPT EVENTS\n" );
        for ( MyPair<SocketEvent, SocketEvent> se : connAcptEvents.values() )
        {
            debugMsg.append( se.getFirst() + " -> " + se.getSecond() + "\n" );
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "CLOSE/SHUTDOWN EVENTS\n" );
        for ( MyPair<SocketEvent, SocketEvent> se : closeShutEvents.values() )
        {
            debugMsg.append( se.getFirst() + " -> " + se.getSecond() + "\n" );
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "FORK EVENTS\n" );
        for ( List<ThreadCreationEvent> fset : forkEvents.values() )
        {
            for ( ThreadCreationEvent f : fset )
            {
                debugMsg.append( f.toString() + "\n" );
            }
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "JOIN EVENTS\n" );
        for ( List<ThreadCreationEvent> jset : joinEvents.values() )
        {
            for ( ThreadCreationEvent j : jset )
            {
                debugMsg.append( j.toString() + "\n" );
            }
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "READ/WRITE EVENTS\n" );
        for ( List<RWEvent> rlist : readEvents.values() )
        {
            for ( RWEvent r : rlist )
            {
                debugMsg.append( r.toString() + "\n" );
            }
        }
        for ( List<RWEvent> wlist : writeEvents.values() )
        {
            for ( RWEvent w : wlist )
            {
                debugMsg.append( w.toString() + "\n" );
            }
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "LOCKING EVENTS\n" );
        for ( List<MyPair<SyncEvent, SyncEvent>> pairs : lockEvents.values() )
        {
            for ( MyPair<SyncEvent, SyncEvent> p : pairs )
            {
                debugMsg.append( p.toString() + "\n" );
            }
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "WAIT EVENTS\n" );
        for ( List<SyncEvent> wlist : waitEvents.values() )
        {
            for ( SyncEvent w : wlist )
            {
                debugMsg.append( w.toString() + "\n" );
            }
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "NOTIFY EVENTS\n" );
        for ( List<SyncEvent> nlist : notifyEvents.values() )
        {
            for ( SyncEvent n : nlist )
            {
                debugMsg.append( n.toString() + "\n" );
            }
        }
        logger.debug( debugMsg.toString() );

        if ( !sortedByTimestamp.isEmpty() )
        {
            debugMsg = new StringBuilder();
            debugMsg.append( "TIMESTAMP EVENTS\n" );
            for ( Event e : sortedByTimestamp )
            {
                debugMsg.append( e.toString() + "\n" );
            }
            logger.debug( debugMsg.toString() );
        }
    }
}
