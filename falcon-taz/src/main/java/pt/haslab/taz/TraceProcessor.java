package pt.haslab.taz;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.taz.causality.SocketCausalPair;
import pt.haslab.taz.events.Event;
import pt.haslab.taz.events.EventType;
import pt.haslab.taz.events.HandlerEvent;
import pt.haslab.taz.events.LogEvent;
import pt.haslab.taz.causality.CausalPair;
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
    public Map<String, SocketCausalPair> msgEvents;

    /* Map: rcv event-> list of events of the message handler
     * (list starts with HANDLERBEGIN and ends with HANDLEREND) */
    public Map<SocketEvent, List<Event>> handlerEvents;

    /* set indicating whether a given thread's trace has message handlers */
    private HashSet hasHandlers;

    /* Map: socket id -> pair of events (connect,accept) */
    public Map<String, CausalPair<SocketEvent, SocketEvent>> connAcptEvents;

    /* Map: socket id -> pair of events (close,shutdown) */
    public Map<String, CausalPair<SocketEvent, SocketEvent>> closeShutEvents;

    /* Map: thread -> list of all events in that thread's execution */
    public Map<String, List<Event>> eventsPerThread;

    /* Map: thread -> list of thread's fork events */
    public Map<String, List<ThreadCreationEvent>> forkEvents;

    /* Map: thread -> list of thread's join events */
    public Map<String, List<ThreadCreationEvent>> joinEvents;

    /* Map: string (event.toString) -> Event object */
    public HashMap<String, Event> eventNameToObject;

    /* Map: mutex variable -> list of pairs of locks/unlocks */
    public Map<String, List<CausalPair<SyncEvent, SyncEvent>>> lockEvents;

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
    private Map<String, CausalPair<Deque<SocketEvent>, Deque<SocketEvent>>> pendingEventsSndRcv;

    TraceProcessor()
    {
        msgEvents = new HashMap<String, SocketCausalPair>();
        lockEvents = new HashMap<String, List<CausalPair<SyncEvent, SyncEvent>>>();
        eventsPerThread = new HashMap<String, List<Event>>();
        readEvents = new HashMap<String, List<RWEvent>>();
        writeEvents = new HashMap<String, List<RWEvent>>();
        forkEvents = new HashMap<String, List<ThreadCreationEvent>>();
        joinEvents = new HashMap<String, List<ThreadCreationEvent>>();
        waitEvents = new HashMap<String, List<SyncEvent>>();
        notifyEvents = new HashMap<String, List<SyncEvent>>();
        connAcptEvents = new HashMap<String, CausalPair<SocketEvent, SocketEvent>>();
        closeShutEvents = new HashMap<String, CausalPair<SocketEvent, SocketEvent>>();
        eventNameToObject = new HashMap<String, Event>();
        sortedByTimestamp = new TreeSet<Event>( new TimestampComparator() );
        pendingEventsSndRcv = new HashMap<String, CausalPair<Deque<SocketEvent>, Deque<SocketEvent>>>();
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
                        logger.error( "Invalid JSON: " + line );
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
        SocketCausalPair pair = msgEvents.get( messageId );
        if ( pair == null )
            return null;

        return pair.getSnd();
    }

    public SyncEvent getCorrespondingUnlock( SyncEvent lockEvent )
    {
        String thread = lockEvent.getThread();
        List<CausalPair<SyncEvent, SyncEvent>> pairs = lockEvents.get( lockEvent.getVariable() );
        for ( CausalPair<SyncEvent, SyncEvent> se : pairs )
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
        // --- REQUIRED FIELDS ---
        EventType type = EventType.getEventType( event.getString( "type" ) );

        if ( type == null )
            throw new JSONException( "Unknown event type: " + event.getString( "type" ) );

        String thread = event.getString( "thread" );
        String loc = event.optString( "loc" );
        loc = ( loc == null ) ? "" : loc;
        // consider timestamp to be a long for the moment
        long time = event.getLong( "timestamp" );
        String timestamp = String.valueOf( time );

        // --- OPTIONAL FIELDS ---
        //use event id from trace if it's present in the JSON object
        //otherwise, use global event counter as id
        long eventId = event.has( "id" ) ? event.getLong( "id" ) : eventNumber++;
        Event e = new Event( timestamp, type, thread, eventId, loc );

        String dependency = event.optString( "dependency" );
        if(dependency.length() == 0 || dependency.equals( "null" ))
            dependency = null;
        e.setDependency( dependency );
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
                LogEvent logEvent = new LogEvent( e, msg );
                eventsPerThread.get( thread ).add( logEvent );
                break;
            case CONNECT:
            case ACCEPT:
            case CLOSE:
            case SHUTDOWN:
            case RCV:
            case SND:
                SocketEvent socketEvent = new SocketEvent( e );
                String socket = event.getString( "socket" );
                socketEvent.setSocket( socket );
                socketEvent.setSocketType( event.getString( "socket_type" ) );
                socketEvent.setSrc( event.getString( "src" ) );
                socketEvent.setSrcPort( event.getInt( "src_port" ) );
                socketEvent.setDst( event.getString( "dst" ) );
                socketEvent.setDstPort( event.getInt( "dst_port" ) );

                //handle SND and RCV
                if ( type == EventType.SND || type == EventType.RCV )
                {
                    socketEvent.setSize( event.getInt( "size" ) );
                    socketEvent.setMessageId( event.optString( "message", null ) );

                    //handle UDP cases by matching message id
                    if ( socketEvent.getSocketType() == SocketEvent.SocketType.UDP )
                    {
                        //update existing entry or create one if necessary
                        if ( msgEvents.containsKey( socketEvent.getMessageId() ) )
                        {
                            if ( type == EventType.SND )
                            {
                                msgEvents.get( socketEvent.getMessageId() ).addSnd( socketEvent );
                            }
                            else
                            {
                                msgEvents.get( socketEvent.getMessageId() ).addRcv( socketEvent );
                            }
                        }
                        else
                        {
                            SocketCausalPair pair;
                            if ( type == EventType.SND )
                            {
                                pair = new SocketCausalPair();
                                pair.addSnd( socketEvent );
                            }
                            else
                            {
                                pair = new SocketCausalPair();
                                pair.addRcv( socketEvent );
                            }
                            msgEvents.put( socketEvent.getMessageId(), pair );
                        }
                    }

                    //handle TCP cases that may not have message id and/or partitioned messages
                    else if ( socketEvent.getSocketType() == SocketEvent.SocketType.TCP )
                    {
                        if ( type == EventType.SND )
                        {
                            handleSndSocketEvent( socketEvent );
                        }

                        if ( type == EventType.RCV )
                        {
                            handleRcvSocketEvent( socketEvent );
                        }
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
                            connAcptEvents.get( socket ).setSecond( socketEvent );
                        }
                        else
                        {
                            connAcptEvents.get( socket ).setFirst( socketEvent );
                        }
                    }
                    else
                    {
                        //create a new pair (CONNECT,ACCEPT)
                        CausalPair<SocketEvent, SocketEvent> pair = new CausalPair<SocketEvent, SocketEvent>( null, null );
                        if ( type == EventType.CONNECT )
                        {
                            pair.setFirst( socketEvent );
                        }
                        else
                        {
                            pair.setSecond( socketEvent );
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
                            closeShutEvents.get( socket ).setSecond( socketEvent );
                        }
                        else
                        {
                            //put close event if there's none yet or overwrite the existing one if it's from the same thread as the shutdown
                            SocketEvent closeEvent = closeShutEvents.get( socket ).getFirst();
                            SocketEvent shutevent = closeShutEvents.get( socket ).getSecond();
                            if ( closeEvent == null
                                            || ( shutevent != null && !shutevent.getThread().equals(
                                            socketEvent.getThread() ) ) )
                            {
                                closeShutEvents.get( socket ).setFirst( socketEvent );
                            }
                        }
                    }
                    else
                    {
                        //create a new pair (CLOSE,SHUTDOWN)
                        CausalPair<SocketEvent, SocketEvent> pair = new CausalPair<SocketEvent, SocketEvent>( null, null );
                        if ( type == EventType.CLOSE )
                        {
                            pair.setFirst( socketEvent );
                        }
                        else
                        {
                            pair.setSecond( socketEvent );
                        }
                        closeShutEvents.put( socket, pair );
                    }
                }

                if ( socketEvent.getTimestamp() != null && !socketEvent.getTimestamp().equals( "" ) )
                    sortedByTimestamp.add( socketEvent );

                eventsPerThread.get( thread ).add( socketEvent );
                break;
            case START:
            case END:
                eventsPerThread.get( thread ).add( e );
                break;
            case CREATE:
            case JOIN:
                String child = event.getString( "child" );
                ThreadCreationEvent creationEvent = new ThreadCreationEvent( e );
                creationEvent.setChildThread( child );
                if ( type == EventType.CREATE )
                {
                    if ( !forkEvents.containsKey( thread ) )
                    {
                        forkEvents.put( thread, new LinkedList<ThreadCreationEvent>() );
                    }
                    forkEvents.get( thread ).add( creationEvent );
                }
                else
                {
                    if ( !joinEvents.containsKey( thread ) )
                    {
                        joinEvents.put( thread, new LinkedList<ThreadCreationEvent>() );
                    }
                    joinEvents.get( thread ).add( creationEvent );
                }
                eventsPerThread.get( thread ).add( creationEvent );
                break;
            case WRITE:
            case READ:
                String var = event.getString( "variable" );
                RWEvent rwEvent = new RWEvent( e );
                rwEvent.setVariable( var );

                if ( type == EventType.READ )
                {
                    if ( !readEvents.containsKey( var ) )
                    {
                        readEvents.put( var, new LinkedList<RWEvent>() );
                    }
                    readEvents.get( var ).add( rwEvent );
                }
                else
                {
                    if ( !writeEvents.containsKey( var ) )
                    {
                        writeEvents.put( var, new LinkedList<RWEvent>() );
                    }
                    writeEvents.get( var ).add( rwEvent );
                }
                eventsPerThread.get( thread ).add( rwEvent );
                break;
            case HNDLBEG:
            case HNDLEND:
                HandlerEvent handlerEvent = new HandlerEvent( e );
                eventsPerThread.get( thread ).add( handlerEvent );
                if ( type == EventType.HNDLBEG )
                    hasHandlers.add( thread );
                break;
            case LOCK:
            case UNLOCK:
                var = event.getString( "variable" );
                SyncEvent syncEvent = new SyncEvent( e );
                syncEvent.setVariable( var );

                if ( type == EventType.LOCK )
                {
                    List<CausalPair<SyncEvent, SyncEvent>> pairList = lockEvents.get( var );
                    CausalPair<SyncEvent, SyncEvent> pair =
                                    ( pairList != null ) ? pairList.get( pairList.size() - 1 ) : null;
                    if ( pair == null || pair.getSecond() != null )
                    {
                        // Only adds the lock event if the previous lock event has a corresponding unlock
                        // in order to handle Reentrant Locks
                        Utils.insertInMapToLists( lockEvents, var, new CausalPair<SyncEvent, SyncEvent>( syncEvent, null ) );
                        eventsPerThread.get( thread ).add( syncEvent );
                    }
                }
                else if ( type == EventType.UNLOCK )
                {
                    // second component is the unlock event associated with the lock
                    List<CausalPair<SyncEvent, SyncEvent>> pairList = lockEvents.get( var );
                    CausalPair<SyncEvent, SyncEvent> pair =
                                    ( pairList != null ) ? pairList.get( pairList.size() - 1 ) : null;
                    if ( pair == null )
                    {
                        Utils.insertInMapToLists( lockEvents, var, new CausalPair<SyncEvent, SyncEvent>( null, syncEvent ) );
                    }
                    else
                    {
                        pair.setSecond( syncEvent );
                    }
                    eventsPerThread.get( thread ).add( syncEvent );
                }
                break;
            case NOTIFY:
            case NOTIFYALL:
            case WAIT:
                var = event.getString( "variable" );
                syncEvent = new SyncEvent( e );
                syncEvent.setVariable( var );
                if ( type == EventType.WAIT )
                {
                    if ( !waitEvents.containsKey( var ) )
                    {
                        waitEvents.put( var, new LinkedList<SyncEvent>() );
                    }
                    waitEvents.get( var ).add( syncEvent );
                }
                else if ( type == EventType.NOTIFY || type == EventType.NOTIFYALL )
                {
                    if ( !notifyEvents.containsKey( var ) )
                    {
                        notifyEvents.put( var, new LinkedList<SyncEvent>() );
                    }
                    notifyEvents.get( var ).add( syncEvent );
                }
                eventsPerThread.get( thread ).add( syncEvent );
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

    private CausalPair<Deque<SocketEvent>, Deque<SocketEvent>> getOrCreatePartialEventsPairs( String socket )
    {
        if ( !pendingEventsSndRcv.containsKey( socket ) )
        {
            Deque<SocketEvent> sndEvents = new ArrayDeque<SocketEvent>();
            Deque<SocketEvent> rcvEvents = new ArrayDeque<SocketEvent>();
            CausalPair<Deque<SocketEvent>, Deque<SocketEvent>> pendingEventsPair =
                            new CausalPair<Deque<SocketEvent>, Deque<SocketEvent>>( sndEvents, rcvEvents );

            pendingEventsSndRcv.put( socket, pendingEventsPair );

            return pendingEventsPair;
        }

        return pendingEventsSndRcv.get( socket );
    }

    private void handleSndSocketEvent( SocketEvent snd )
    {
        CausalPair<Deque<SocketEvent>, Deque<SocketEvent>> eventPairs =
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

        SocketEvent rcv = rcvEvents.peek();
        String msgId = computeMessageId(snd, rcv);

        // Create new causal pair if necessary
        if(!msgEvents.containsKey( msgId ))
        {
            SocketCausalPair socketPair = new SocketCausalPair(  );
            msgEvents.put( msgId, socketPair );
        }

        // Flag that indicates whether the socket pair ([SND], [RCV]) is complete,
        // meaning that all bytes sent were received
        boolean hasRCVtoMatch = true;

        // Pair this SND with the pending enqueued RCV events and, if there are bytes remaining
        // at the end, add SND event to the pending queue
        while ( rcvEvents.size() > 0 && hasRCVtoMatch)
        {
            rcv = rcvEvents.peek();

            if( rcv.getMessageId() == null )
            {
                msgId = computeMessageId( snd, rcv );
            }

            SocketCausalPair socketPair = msgEvents.get( msgId );

            if ( snd.getSize() > rcv.getSize() )
            {
                // Move partitioned RCV from the pending queue to the causal pair
                rcvEvents.pop();
                socketPair.addRcv( rcv );

                // Subtract read bytes from SND event
                snd.setSize( snd.getSize() - rcv.getSize() );
            }
            else if ( rcv.getSize() == snd.getSize() )
            {
                // Move partitioned RCV from the pending queue to the causal pair
                rcvEvents.pop();
                socketPair.addRcv( rcv );

                // Move SND from the pending queue to the causal pair
                if( !sndEvents.isEmpty() && sndEvents.peek().equals( snd ))
                    sndEvents.pop();

                socketPair.addSnd( snd );

                //rebalance bytes sent/received
                socketPair.recomputeSize();

                hasRCVtoMatch = false;
            }
            else
            {
                // Subtract sent bytes from RCV event
                rcv.setSize( rcv.getSize() - snd.getSize() );

                // Add partitioned SND to the causal pair
                socketPair.addSnd( snd );

                // Account for case in which there was already a pending RCV in the queue
                // that is larger than the incoming SND
                hasRCVtoMatch = false;
            }
        }

        // add SND to the pending queue if not already present
        if( hasRCVtoMatch && ( !sndEvents.isEmpty() && !sndEvents.peek().equals( snd ) ) )
            sndEvents.add( snd );
    }

    private void handleRcvSocketEvent( SocketEvent rcv )
    {
        CausalPair<Deque<SocketEvent>, Deque<SocketEvent>> eventPairs =
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

        SocketEvent snd = sndEvents.peek();
        String msgId = computeMessageId(snd, rcv);

        // Create new causal pair if necessary
        if(!msgEvents.containsKey( msgId ))
        {
            SocketCausalPair socketPair = new SocketCausalPair(  );
            msgEvents.put( msgId, socketPair );
        }

        // Flag that indicates whether the socket pair ([SND], [RCV]) is complete,
        // meaning that all bytes sent were received
        boolean hasSNDtoMatch = true;

        // Pair this RCV with the pending enqueued SND events and, if there are bytes remaining
        // at the end, add RCV event to the pending queue
        while ( sndEvents.size() > 0 && hasSNDtoMatch )
        {
            snd = sndEvents.peek();
            if( snd.getMessageId() == null )
            {
                msgId = computeMessageId( snd, rcv );
            }

            SocketCausalPair socketPair = msgEvents.get( msgId );

            if ( rcv.getSize() > snd.getSize() )
            {
                // Move partitioned SND from the pending queue to the causal pair
                sndEvents.pop();
                socketPair.addSnd( snd );

                // Subtract read bytes from RCV event
                rcv.setSize( rcv.getSize() - snd.getSize() );
            }
            else if ( rcv.getSize() == snd.getSize() )
            {
                // Move (partitioned) SND from the pending queue to the causal pair
                sndEvents.pop();
                socketPair.addSnd( snd );

                // Move RCV from the pending queue to the causal pair
                if( !rcvEvents.isEmpty() && rcvEvents.peek().equals( rcv ))
                    rcvEvents.pop();

                socketPair.addRcv( rcv );

                //rebalance bytes sent/received
                socketPair.recomputeSize();

                hasSNDtoMatch = false;
            }
            else
            {
                // Subtract sent bytes from SND event
                snd.setSize( snd.getSize() - rcv.getSize() );

                // Add partitioned RCV to the causal pair
                socketPair.addRcv( rcv );

                // Account for case in which there was already a pending SND in the queue
                // that is larger than the incoming RCV
                hasSNDtoMatch = false;
            }
        }

        // add RCV to the pending queue if not already present
        if( hasSNDtoMatch && ( !rcvEvents.isEmpty() && !rcvEvents.peek().equals( rcv ) ) )
            rcvEvents.add( rcv );
    }

    /**
     *  Compute message id for a pair of SND and RCV events (potentially partitioned).
     *  The message id is computed as follows:
     *    i) msgId = SND's event id if SND's bytes >= RCV's bytes
     *   ii) msgId = RCV's event id if RCV's bytes > SND's bytes
     * @param snd
     * @param rcv
     * @return message Id for the pair
     */
    public String computeMessageId(SocketEvent snd, SocketEvent rcv)
    {
        String msgId = String.valueOf( snd.getEventId() );

        // case i) msgId = SND's event because SND's bytes >= RCV's bytes
        if (  snd.getSize() >= rcv.getSize() )
        {
            // Account for cases in which RCV's size == SND's size because RCV's bytes were already decremented:
            //  i) rcv.msgId == null -> use SND's event id as message Id
            // ii) rcv.msgId != null -> RCV's bytes were decremented, thus use RCV's event id as message Id
            if( rcv.getMessageId() == null )
            {
                rcv.setMessageId( msgId );
            }
            else
            {
                msgId = rcv.getMessageId();
            }

            snd.setMessageId( msgId );
        }
        // case ii) msgId = RCV's event id because RCV's bytes > SND's bytes
        else
        {
            msgId = String.valueOf( rcv.getEventId() );
            snd.setMessageId( msgId );
            rcv.setMessageId( msgId );
        }

        return msgId;
    }

    /**
     * Combines partitioned SNDs or RCVs into single coarse-grained events
     */
    public void aggregateAllPartitionedMessages()
    {
        for( SocketCausalPair pair : msgEvents.values() )
        {
            pair.aggregatePartitionedMessages();
        }
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
        for ( SocketCausalPair se : msgEvents.values() )
        {
            debugMsg.append( se.toString() + "\n" );
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
        for ( CausalPair<SocketEvent, SocketEvent> se : connAcptEvents.values() )
        {
            debugMsg.append( se.getFirst() + " -> " + se.getSecond() + "\n" );
        }
        logger.debug( debugMsg.toString() );

        debugMsg = new StringBuilder();
        debugMsg.append( "CLOSE/SHUTDOWN EVENTS\n" );
        for ( CausalPair<SocketEvent, SocketEvent> se : closeShutEvents.values() )
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
        for ( List<CausalPair<SyncEvent, SyncEvent>> pairs : lockEvents.values() )
        {
            for ( CausalPair<SyncEvent, SyncEvent> p : pairs )
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
