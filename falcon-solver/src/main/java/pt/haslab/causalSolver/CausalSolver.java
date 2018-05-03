package pt.haslab.causalSolver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pt.haslab.causalSolver.solver.Solver;
import pt.haslab.causalSolver.solver.Z3Solver;
import pt.haslab.causalSolver.stats.Stats;
import pt.haslab.taz.TraceProcessor;
import pt.haslab.taz.events.Event;
import pt.haslab.taz.events.EventType;
import pt.haslab.taz.events.MyPair;
import pt.haslab.taz.events.SocketEvent;
import pt.haslab.taz.events.SyncEvent;
import pt.haslab.taz.events.ThreadCreationEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by nunomachado on 30/03/17.
 */
@SuppressWarnings( "ALL" )
public class CausalSolver
{
    // Global event counter
    public static int dataEventId = 0; // Counter for RCV and SND messages

    //properties
    public static Properties props;

    //event trace processor
    public static TraceProcessor trace;

    //solver stuff
    public static Solver solver;

    public static HashMap<String, Event> allEvents; //map: string (event.toString) -> Event object

    //command line config
    public enum Parameters
    {
        EVENT_FILE( "event-file" ),
        SOLVER( "solver-bin" ),
        OUTPUT( "output-file" ),
        GOAL_TS( "use-timestamp" );

        private final String desc;

        private Parameters( String l )
        {
            this.desc = l;
        }

        @Override
        public String toString()
        {
            return this.desc;
        }
    }

    public static void main( String args[] )
    {
        allEvents = new HashMap<String, Event>();

        try
        {
            String propFile = "causalSolver.properties";
            props = new Properties();
            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream( propFile );
            if ( is != null )
            {
                props.load( is );

                parseParameters( args );

                //populate data structures
                String traceFile = props.getProperty( Parameters.EVENT_FILE.toString() );
                trace = TraceProcessor.INSTANCE;
                trace.loadEventTrace( traceFile );
                Stats.numEventsTrace = trace.getNumberOfEvents();

                //build constraint model with causality constraints
                initSolver();
                long modelStart = System.currentTimeMillis();
                buildConstraintModel();
                Stats.buildingModeltime = System.currentTimeMillis() - modelStart;

                //solve model
                long solvingStart = System.currentTimeMillis();
                boolean result = solver.solveModel();
                Stats.solvingTime = System.currentTimeMillis() - solvingStart;

                //parse solver's output and obtain causal order
                if ( result )
                {
                    parseSolverOutput();

                    //generate JSON file with events causally ordered
                    outputCausalOrderJSON();

                    Stats.printStats();
                }
                else
                    System.out.println( "unsat" );

                solver.close();
            }
        }
        catch ( FileNotFoundException e )
        {
            System.out.println( "[ERROR] Cannot find file!" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public static void initSolver()
                    throws IOException
    {
        String solverPath = props.getProperty( Parameters.SOLVER.toString() ); //set up solver path
        System.out.println( "[CausalSolver] Init solver: " + solverPath );
        solver = Z3Solver.getInstance();
        solver.init( solverPath );
    }

    public static void buildConstraintModel()
                    throws IOException
    {
        genProgramOrderConstraints();
        genCommunicationConstraints();
        genForkStartConstraints();
        genJoinExitConstraints();
        genLockingConstraints();
        genWaitNotifyConstraints();
        boolean useTimestamps = props.getProperty( Parameters.GOAL_TS.toString() ).equals( "true" );
        if ( useTimestamps )
        {
            genTimestampConstraints();
        }
        genCausalOrderFunction();
        solver.flush();
    }

    private static void genTimestampConstraints()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate timestamp constraints" );
        HashMap<String, List<SocketEvent>> channelEvents = new HashMap<String, List<SocketEvent>>();

        //filter events per socket channel
        for ( Event tse : trace.sortedByTimestamp )
        {
            if ( tse instanceof SocketEvent )
            {
                SocketEvent se = (SocketEvent) tse;
                String socketId = se.getSocket();
                if ( !channelEvents.containsKey( se.getSocket() ) )
                {
                    List<SocketEvent> tmp = new ArrayList<SocketEvent>();
                    channelEvents.put( socketId, tmp );
                }
                channelEvents.get( socketId ).add( se );
            }
        }

        String tagTS = "TS_";
        int counterTS = 0;
        solver.writeComment( "TIMESTAMP CONSTRAINTS" );

        for ( Map.Entry<String, List<SocketEvent>> entry : channelEvents.entrySet() )
        {
            List<SocketEvent> events = entry.getValue();
            String tsConstraint = "";
            for ( Event se : trace.sortedByTimestamp )
            {
                tsConstraint += ( se.toString() + " " );
            }
            solver.writeConstraint( solver.postNamedSoftAssert( solver.cLt( tsConstraint ), tagTS + counterTS++ ) );
        }
    }

    public static void genProgramOrderConstraints()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate program order constraints" );
        String tagPO = "PO_";
        int counterPO = 0;
        int max = 0;
        for ( List<Event> l : trace.eventsPerThread.values() )
        {
            max += l.size();
        }
        solver.writeConstraint( solver.declareIntVar( "MAX" ) );
        solver.writeConstraint( solver.postAssert( solver.cEq( "MAX", String.valueOf( max ) ) ) );

        //generate program order variables and constraints
        for ( List<Event> events : trace.eventsPerThread.values() )
        {
            if ( !events.isEmpty() )
            {
                solver.writeComment( "PROGRAM ORDER CONSTRAINTS - THREAD " + events.get( 0 ).getThread() );
                String threadOrder = "";
                for ( Event e : events )
                {
                    String var = solver.declareIntVar( e.toString(), "0", "MAX" );
                    solver.writeConstraint( var );
                    threadOrder += ( e.toString() + " " );

                    //store event in allEvents
                    allEvents.put( e.toString(), e );
                }
                if ( events.size() > 1 )
                {
                    solver.writeConstraint( solver.postNamedAssert( solver.cLt( threadOrder ), tagPO + counterPO++ ) );
                    solver.writeConstraint( solver.postAssert( solver.cDistinct( threadOrder ) ) );
                }
            }
        }
    }

    public static void genCommunicationConstraints()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate communication constraints" );
        String tagSND_RCV = "SR_";
        int counterSND_RCV = 0;
        solver.writeComment( "COMMUNICATION CONSTRAINTS - SEND / RECEIVE" );
        for ( MyPair<SocketEvent, SocketEvent> pair : trace.msgEvents.values() )
        {
            if ( pair.getFirst() != null && pair.getSecond() != null )
            {
                pair.getSecond().setDependency( String.valueOf( pair.getFirst().hashCode() ) ); //set dependency
                String cnst = solver.cLt( pair.getFirst().toString(), pair.getSecond().toString() );
                solver.writeConstraint( solver.postNamedAssert( cnst, tagSND_RCV + counterSND_RCV++ ) );
            }
        }

        solver.writeComment( "COMMUNICATION CONSTRAINTS - CONNECT / ACCEPT" );
        String tagCON_ACC = "CA_";
        int counterCON_ACC = 0;
        for ( MyPair<SocketEvent, SocketEvent> pair : trace.connAcptEvents.values() )
        {
            if ( pair.getFirst() != null && pair.getSecond() != null )
            {
                pair.getSecond().setDependency( String.valueOf( pair.getFirst().hashCode() ) ); //set dependency
                String cnst = solver.cLt( pair.getFirst().toString(), pair.getSecond().toString() );
                solver.writeConstraint( solver.postNamedAssert( cnst, tagCON_ACC + counterCON_ACC++ ) );
            }
        }

        solver.writeComment( "COMMUNICATION CONSTRAINTS - CLOSE / SHUTDOWN" );
        String tagCLS_SHT = "CS_";
        int counterCLS_SHT = 0;
        for ( MyPair<SocketEvent, SocketEvent> pair : trace.closeShutEvents.values() )
        {
            if ( pair.getFirst() != null && pair.getSecond() != null )
            {
                pair.getSecond().setDependency( String.valueOf( pair.getFirst().hashCode() ) ); //set dependency
                String cnst = solver.cLt( pair.getFirst().toString(), pair.getSecond().toString() );
                solver.writeConstraint( solver.postNamedAssert( cnst, tagCLS_SHT + counterCLS_SHT++ ) );
            }
        }
    }

    public static void genLockingConstraints()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate locking constraints" );
        solver.writeComment( "LOCKING CONSTRAINTS" );
        for ( String var : trace.lockEvents.keySet() )
        {
            // for two lock/unlock pairs on the same locking object,
            // one pair must be executed either before or after the other
            ListIterator<MyPair<SyncEvent, SyncEvent>> pairIterator_i = trace.lockEvents.get( var ).listIterator( 0 );
            ListIterator<MyPair<SyncEvent, SyncEvent>> pairIterator_j;

            while ( pairIterator_i.hasNext() )
            {
                MyPair<SyncEvent, SyncEvent> pair_i = pairIterator_i.next();
                //advance iterator to have two different pairs
                pairIterator_j = trace.lockEvents.get( var ).listIterator( pairIterator_i.nextIndex() );

                while ( pairIterator_j.hasNext() )
                {
                    MyPair<SyncEvent, SyncEvent> pair_j = pairIterator_j.next();

                    //there is no need to add constraints for locking pairs of the same thread
                    //as they are already encoded in the program order constraints
                    if ( pair_i.getFirst().getThread().equals( pair_j.getFirst().getThread() ) )
                        continue;

                    // Ui < Lj || Uj < Li
                    String cnstUi_Lj = solver.cLt( pair_i.getSecond().toString(), pair_j.getFirst().toString() );
                    String cnstUj_Li = solver.cLt( pair_j.getSecond().toString(), pair_i.getFirst().toString() );
                    String cnst = solver.cOr( cnstUi_Lj, cnstUj_Li );
                    solver.writeConstraint( solver.postNamedAssert( cnst, "LC" ) );
                }
            }
        }
    }

    public static void genForkStartConstraints()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate fork-start constraints" );
        String tagFRK_STR = "FS_";
        int counterFRK_STR = 0;
        solver.writeComment( "FORK-START CONSTRAINTS" );
        for ( List<ThreadCreationEvent> l : trace.forkEvents.values() )
        {
            for ( ThreadCreationEvent forkevent : l )
            {
                String startEvent = "START_" + forkevent.getChildThread();
                String cnst = solver.cLt( forkevent.toString(), startEvent );
                solver.writeConstraint( solver.postNamedAssert( cnst, tagFRK_STR + counterFRK_STR++ ) );
                //set dependency
                allEvents.get( startEvent ).setDependency( String.valueOf( forkevent.hashCode() ) );
            }
        }
    }

    public static void genJoinExitConstraints()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate join-exit constraints" );
        solver.writeComment( "JOIN-END CONSTRAINTS" );
        String tagJOIN_END = "JE_";
        int counterJOIN_END = 0;
        for ( List<ThreadCreationEvent> l : trace.joinEvents.values() )
        {
            for ( ThreadCreationEvent joinEvent : l )
            {
                int endEventPos = trace.eventsPerThread.get( joinEvent.getChildThread() ).size();
                Event endEvent = trace.eventsPerThread.get( joinEvent.getChildThread() ).get(
                                endEventPos - 1 );//"END_"+joinEvent.getChildThread();
                String cnst = solver.cLt( endEvent.toString(), joinEvent.toString() );
                solver.writeConstraint( solver.postNamedAssert( cnst, tagJOIN_END + counterJOIN_END++ ) );
                //set dependency
                allEvents.get( joinEvent.toString() ).setDependency( String.valueOf( endEvent.hashCode() ) );
            }
        }
    }

    public static void genWaitNotifyConstraints()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate wait-notify constraints" );
        solver.writeComment( "WAIT-NOTIFY CONSTRAINTS" );
        HashMap<SyncEvent, Set<String>> binaryVars =
                        new HashMap<SyncEvent, Set<String>>(); //map: notify event -> list of all binary vars corresponding to that notify

        //for a given condition, each notify can be mapped to any wait
        //but a wait can only have a single notify
        for ( String condition : trace.waitEvents.keySet() )
        {
            for ( SyncEvent wait : trace.waitEvents.get( condition ) )
            {
                StringBuilder globalOr = new StringBuilder();

                for ( SyncEvent notify : trace.notifyEvents.get( condition ) )
                {
                    //binary var used to indicate whether the signal operation is mapped to a wait operation or not
                    String binVar = "B_" + condition + "-W_" + wait.getThread() + "_" + wait.getEventNumber() + "-N_"
                                    + notify.getThread() + "_" + notify.getEventNumber();

                    if ( !binaryVars.containsKey( notify ) )
                    {
                        binaryVars.put( notify, new HashSet<String>() );
                    }
                    binaryVars.get( notify ).add( binVar );

                    //const: Oa_sg < Oa_wt && b^{a_sg}_{a_wt} = 1
                    globalOr.append( solver.cAnd( solver.cLt( notify.toString(), wait.toString() ),
                                                  solver.cEq( binVar, "1" ) ) );
                    solver.writeConstraint( solver.declareIntVar( binVar, 0, 1 ) );
                }
                solver.writeConstraint( solver.postNamedAssert( solver.cOr( globalOr.toString() ), "WN" ) );
            }
        }

        //add constraints stating that a given notify can only be mapped to a single wait operation
        for ( SyncEvent notify : binaryVars.keySet() )
        {
            //for notifyAll, we don't constrain the number of waits that can be matched with this notify
            if ( notify.getType() == EventType.NOTIFYALL )
            {
                //const: Sum_{x \in WT} b^{a_sg}_{x} >= 0
                solver.writeConstraint( solver.postNamedAssert(
                                solver.cGeq( solver.cSummation( binaryVars.get( notify ) ), "0" ), "WN" ) );
            }
            else
            {
                //const: Sum_{x \in WT} b^{a_sg}_{x} <= 1
                solver.writeConstraint( solver.postNamedAssert(
                                solver.cLeq( solver.cSummation( binaryVars.get( notify ) ), "1" ), "WN" ) );
            }
        }
    }

    /**
     * Objective function consists in minimizing the order (logical timestamp) of each event
     * in such a way that preserves the happens-before constraint
     *
     * @throws IOException
     */
    public static void genCausalOrderFunction()
                    throws IOException
    {
        System.out.println( "[CausalSolver] Generate causality objective function" );
        solver.writeComment( "CAUSALITY OBJECTIVE FUNCTION" );
        solver.writeConstraint( solver.cMinimize( solver.cSummation( allEvents.keySet() ) ) );
    }

    /**
     * Parse the input parameters and set the program's configurations accordingly.
     *
     * @param args array containing the configuration inputs.
     */
    public static void parseParameters( String[] args )
    {
        String option = "--";

        for ( int i = 0; i < args.length; i++ )
        {
            String flag = args[i];
            String value = ( i + 1 < args.length ) ? args[++i] : "";
            if ( flag.equals( option + Parameters.EVENT_FILE ) )
            {
                props.setProperty( Parameters.EVENT_FILE.toString(), value );
            }
            else if ( flag.equals( option + Parameters.SOLVER ) )
            {
                props.setProperty( Parameters.SOLVER.toString(), value );
            }
            else if ( flag.equals( option + Parameters.OUTPUT ) )
            {
                props.setProperty( Parameters.OUTPUT.toString(), value );
            }
            else if ( flag.equals( option + Parameters.GOAL_TS ) )
            {
                props.setProperty( Parameters.GOAL_TS.toString(), value );
            }
            else
            { //error - unkown input
                System.err.print( "Wrong input: " + flag );
                System.err.println( "\nOptions:" );
                System.err.println( "--event-file <path-to-event-file>\tPath to the event trace in JSON format." );
                System.err.println(
                                "--output-file <path-to-output-file>\tSave the global, causally-ordered trace produced by Falcon." );
                System.err.println( "--solver-bin <path-to-solver-bin>\tPath to the Z3 binary." );
                System.err.println(
                                "--use-timestamp <true/false>\t\tSolve the model according to the original event timestamps (=true) or to minimize the logical clocks (=false)." );

                System.exit( 1 );
            }
        }
    }

    /**
     * Augment each event with the corresponding logical clock output by Z3.
     */
    public static void parseSolverOutput()
    {
        String output = solver.readOutputLine();
        while ( !output.equals( "" ) && !output.equals( ")" ) )
        {
            //System.out.println(output);

            //it's an event - parse event reference and logical causal order
            if ( output.contains( "(define-fun" ) )
            {
                String[] content = output.split( " " );
                String var = content[3];

                if ( var.startsWith( EventType.CREATE.toString() )
                                || var.startsWith( EventType.START.toString() )
                                || var.startsWith( EventType.END.toString() )
                                || var.startsWith( EventType.JOIN.toString() )
                                || var.startsWith( EventType.LOG.toString() )
                                || var.startsWith( EventType.READ.toString() + "_" )
                                || var.startsWith( EventType.WRITE.toString() + "_" )
                                || var.startsWith( EventType.SND.toString() )
                                || var.startsWith( EventType.RCV.toString() )
                                || var.startsWith( EventType.CONNECT.toString() )
                                || var.startsWith( EventType.ACCEPT.toString() )
                                || var.startsWith( EventType.CLOSE.toString() )
                                || var.startsWith( EventType.SHUTDOWN.toString() )
                                || var.startsWith( EventType.HNDLBEG.toString() )
                                || var.startsWith( EventType.HNDLEND.toString() )
                                || var.startsWith( EventType.LOCK.toString() )
                                || var.startsWith( EventType.UNLOCK.toString() )
                                || var.startsWith( EventType.WAIT.toString() )
                                || var.startsWith( EventType.NOTIFYALL.toString() )
                                || var.startsWith( EventType.NOTIFY.toString() )
                                )
                {
                    output = solver.readOutputLine().trim();
                    int endPos = output.indexOf( ")" );
                    int order = Integer.valueOf( output.substring( 0, endPos ) );

                    if ( allEvents.containsKey( var ) )
                        allEvents.get( var ).setScheduleOrder( order );
                }
            }
            else if ( output.contains( "error" ) )
                System.out.println( output );

            output = solver.readOutputLine();
        }
    }

    /**
     * Generate global ordered trace in JSON format
     */
    public static void outputCausalOrderJSON()
    {
        TreeSet<Event> orderedEvents = new TreeSet<Event>( allEvents.values() );
        try
        {
            FileWriter outfile = new FileWriter( new File( props.getProperty( Parameters.OUTPUT.toString() ) ) );
            JSONArray jsonEvents = new JSONArray();
            for ( Event e : orderedEvents )
            {
                JSONObject json = e.toJSONObject();
                System.out.println( json.toString() );
                jsonEvents.put( json );
            }
            outfile.write( jsonEvents.toString() );
            outfile.flush();
            outfile.close();
        }
        catch ( JSONException exc )
        {
            exc.printStackTrace();
        }
        catch ( IOException exc )
        {
            exc.printStackTrace();
        }
    }
}
