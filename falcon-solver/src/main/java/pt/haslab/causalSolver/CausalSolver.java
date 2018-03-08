package pt.haslab.causalSolver;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import pt.haslab.causalSolver.events.*;
import pt.haslab.causalSolver.solver.Solver;
import pt.haslab.causalSolver.solver.Z3Solver;
import pt.haslab.causalSolver.stats.Stats;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.*;

/**
 * Created by nunomachado on 30/03/17.
 */
@SuppressWarnings("ALL")
public class CausalSolver {
    // Global event counter
    public static int dataEventId = 0; // Counter for RCV and SND messages

    //properties
    public static Properties props;

    //data structures
    public static Map<String, MyPair<Deque<SocketEvent>, Deque<SocketEvent>>> partialEventsSndRcv;     //Map: channel id -> pair of event lists ([snd],[rcv])
    public static Set<MyPair<SocketEvent, SocketEvent>> eventsSndRcv;     //Set: pair of events (snd,rcv)
    public static Map<String, MyPair<SocketEvent, SocketEvent>> eventsConnAcpt;   //Map: socket id -> pair of events (snd,rcv)
    public static Map<String, MyPair<SocketEvent, SocketEvent>> eventsCloseShut;  //Map: socket id -> pair of events (close,shutdown)
    public static Map<String, List<Event>> eventsPerThread;          //Map: thread -> list of all events in that thread's execution
    public static Map<String, List<ThreadSyncEvent>> eventsFork;        //Map: thread -> list of thread's fork events
    public static Map<String, List<ThreadSyncEvent>> eventsJoin;        //Map: thread -> list of thread's join events
    public static TreeSet<TimestampedEvent> sortedByTimestamp;           //list with socket events ordered by timestamp

    //solver stuff
    public static Solver solver;
    public static HashMap<String, Event> allEvents; //map: string (event.toString) -> Event object

    public static void main(String args[]) {
        partialEventsSndRcv = new HashMap<String, MyPair<Deque<SocketEvent>, Deque<SocketEvent>>>();
        eventsSndRcv = new HashSet<MyPair<SocketEvent, SocketEvent>>();
        eventsCloseShut = new HashMap<String, MyPair<SocketEvent, SocketEvent>>();
        eventsConnAcpt = new HashMap<String, MyPair<SocketEvent, SocketEvent>>();
        eventsPerThread = new HashMap<String, List<Event>>();
        eventsFork = new HashMap<String, List<ThreadSyncEvent>>();
        eventsJoin = new HashMap<String, List<ThreadSyncEvent>>();
        allEvents = new HashMap<String, Event>();
        sortedByTimestamp = new TreeSet<TimestampedEvent>(new TimestampComparator());

        try {
            String propFile = "causalSolver.properties";
            props = new Properties();
            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(propFile);
            if (is != null) {
                props.load(is);

                //populate data structures
                loadEvents();

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
                if (result) {
                    parseSolverOutput();

                    //generate JSON file with events causally ordered
                    outputCausalOrderJSON();

                    Stats.printStats();
                } else System.out.println("unsat");

                solver.close();
            }
        }catch (FileNotFoundException e){
            System.out.println("[ERROR] Cannot find file!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadEvents() throws IOException, JSONException {
        String events = props.getProperty("event-file");
        System.out.println("[CausalSolver] load events from "+events);
        BufferedReader br = new BufferedReader(new FileReader(events));
        String line = br.readLine();
        while (line != null) {
            JSONObject object = new JSONObject(line);
            parseJSONEvent(object);
            line = br.readLine();

        }
        printDataStructures();
    }


    private static void parseJSONEvent(JSONObject event) throws JSONException {
        EventType type = EventType.getEventType(event.getString("type"));
        String thread = event.getString("thread");
        String pid = event.optString("pid", thread);

        if(type == null)
            throw new JSONException("Unknown event type: " + event.getString("type"));

        Stats.numEventsTrace++;

        //initialize thread map data structures
        if (!eventsPerThread.containsKey(thread)) {
            eventsPerThread.put(thread, new LinkedList<Event>());
            eventsFork.put(thread, new LinkedList<ThreadSyncEvent>());
            eventsJoin.put(thread, new LinkedList<ThreadSyncEvent>());
        }

        //populate data structures
        Event e = null;
        switch (type) {
            case CONNECT:
            case ACCEPT:
            case CLOSE:
            case SHUTDOWN:
            case RCV:
            case SND:
                e = new SocketEvent(thread, pid, type);
                SocketEvent se = (SocketEvent) e;
                if(event.has("timestamp")){
                    double time = event.getDouble("timestamp");
                    BigDecimal bd = new BigDecimal(String.valueOf(time)).multiply(new BigDecimal(1000000)).stripTrailingZeros();
                    String timestamp = bd.toPlainString();
                    se.setTimestamp(timestamp);
                }

                se.setSrc(event.optString("src", null));
                se.setSrc_port(event.optString("src_port", null));
                se.setDst(event.optString("dst", null));
                se.setDst_port(event.optString("dst_port", null));
                se.setSocket(event.getString("socket"));
                se.setSize(event.optInt("size", 1));
                se.setMessage(String.valueOf(dataEventId++));
                se.setId(); //recompute id
                String key = se.getChannelId();

                //handle SND and RCV
                if (type == EventType.SND)
                    handleSndSocketEvent(se);

                if (type == EventType.RCV)
                    handleRcvSocketEvent(se);

                //handle CONNECT and ACCEPT
                else if (type == EventType.CONNECT || type == EventType.ACCEPT) {
                    if(eventsConnAcpt.containsKey(key)){
                        //put the ACCEPT event if pair.second == null or CONNECT otherwise
                        if(eventsConnAcpt.get(key).getSecond() == null){
                            eventsConnAcpt.get(key).setSecond(se);
                        }
                        else{
                            eventsConnAcpt.get(key).setFirst(se);
                        }
                    }
                    else{
                        //create a new pair (CONNECT,ACCEPT)
                        MyPair<SocketEvent, SocketEvent> pair = new MyPair<SocketEvent, SocketEvent>(null, null);
                        if(type == EventType.CONNECT){
                            pair.setFirst(se);
                        }
                        else {
                            pair.setSecond(se);
                        }
                        eventsConnAcpt.put(key, pair);
                    }
                }
                //handle CLOSE and SHUTDOWN
                else if (type == EventType.CLOSE || type == EventType.SHUTDOWN) {
                    if(eventsCloseShut.containsKey(key)){
                        //put the SHUTDOWN event if pair.second == null and thread is different or CLOSE otherwise
                        if(eventsCloseShut.get(key).getSecond() == null && type == EventType.SHUTDOWN){
                            eventsCloseShut.get(key).setSecond(se);
                        }
                        else{
                            //put close event if there's none yet or overwrite the existing one if it's from the same thread as the shutdown
                            SocketEvent closeEvent = eventsCloseShut.get(key).getFirst();
                            SocketEvent shutevent = eventsCloseShut.get(key).getSecond();
                            if(closeEvent == null
                                    || (shutevent != null && !shutevent.getThread().equals(se.getThread()))){
                                eventsCloseShut.get(key).setFirst(se);
                            }
                        }
                    }
                    else{
                        //create a new pair (CLOSE,SHUTDOWN)
                        MyPair<SocketEvent, SocketEvent> pair = new MyPair<SocketEvent, SocketEvent>(null, null);
                        if(type == EventType.CLOSE){
                            pair.setFirst(se);
                        }
                        else {
                            pair.setSecond(se);
                        }
                        eventsCloseShut.put(key, pair);
                    }
                }
                if(se.getTimestamp() != null && !se.getTimestamp().equals(""))
                    sortedByTimestamp.add(se);
                eventsPerThread.get(thread).add(se);
                break;
            case START:
            case END:
                e = new Event(thread, pid, type);
                eventsPerThread.get(thread).add(e);
                break;
            case LOG:
                BigDecimal bd = new BigDecimal(String.valueOf(event.getString("timestamp"))).multiply(new BigDecimal(1000000)).stripTrailingZeros();
                e = new LogEvent(thread, pid, type, bd.toPlainString());
                eventsPerThread.get(thread).add(e);
                sortedByTimestamp.add((TimestampedEvent) e);
                break;
            case CREATE:
            case JOIN:
                String child = event.getString("child");
                e = new ThreadSyncEvent(thread, type, child);
                e.setId(String.valueOf(e.toString().hashCode()));
                eventsPerThread.get(thread).add(e);
                if (type == EventType.CREATE)
                    eventsFork.get(thread).add((ThreadSyncEvent) e);
                else
                    eventsJoin.get(thread).add((ThreadSyncEvent) e);
                break;
            case WRITE:
            case READ:
            case HNDLBEG:
            case HNDLEND:
                break;
            default:
                throw new JSONException("Unknown event type: " + type);
        }

        if(event.has("data"))
            e.setData(event.optJSONObject("data"));
    }

    private static void handleSndSocketEvent(SocketEvent snd) {
        MyPair<Deque<SocketEvent>, Deque<SocketEvent>> eventPairs = getOrCreatePartialEventsPairs(snd.getChannelId());
        Deque<SocketEvent> sndEvents = eventPairs.getFirst();
        Deque<SocketEvent> rcvEvents = eventPairs.getSecond();


        // When either there's nothing to match this SND message with or there are already
        // SND messages to be paired with RCV messages, enqueue it immediately.
        if (rcvEvents.size() == 0 || sndEvents.size() > 0) {
            sndEvents.add(snd);
            return;
        }

        // Pair this SND with the already enqueued RCV messages and if even after
        // this match there are bytes remaining, enqueue it in SND list.
        while(snd.getSize() > 0 && rcvEvents.size() > 0) {
            SocketEvent rcv = rcvEvents.peek();

            if (snd.getSize() > rcv.getSize()) {
                // 1. Dequeue RCV message from partial receives
                rcv = rcvEvents.pop();

                // 2. Decrement read bytes from SND message
                snd.setSize(snd.getSize() - rcv.getSize());

                // 3. Associate RCV message with SND message
                eventsSndRcv.add(new MyPair<SocketEvent, SocketEvent>(snd, rcv));
            } else {
                // 1. Decrement sent bytes from RCV message
                rcv.setSize(rcv.getSize() - snd.getSize());
                snd.setSize(0);

                // 2. Associate RCV message with SND message
                eventsSndRcv.add(new MyPair<SocketEvent, SocketEvent>(snd, rcv));

                // 3. Remove enqueued RCV if there are no more bytes remaining
                if (rcv.getSize() < 1) rcvEvents.pop();
            }
        }

        // Enqueue SND if there are bytes remaining
        if (snd.getSize() > 0) sndEvents.add(snd);
    }

    private static void handleRcvSocketEvent(SocketEvent rcv) {
        MyPair<Deque<SocketEvent>, Deque<SocketEvent>> eventPairs = getOrCreatePartialEventsPairs(rcv.getChannelId());
        Deque<SocketEvent> sndEvents = eventPairs.getFirst();
        Deque<SocketEvent> rcvEvents = eventPairs.getSecond();

        // When either there's nothing to match this RCV message with or there are already
        // RCV messages to be paired with RCV messages, enqueue it immediately.
        if (sndEvents.size() == 0 || rcvEvents.size() > 0) {
            rcvEvents.add(rcv);
            return;
        }

        // Pair this RCV with the already enqueued SND messages and if even after
        // this match there are bytes remaining, enqueue it in RCV list.
        while(rcv.getSize() > 0 && sndEvents.size() > 0) {
            SocketEvent snd = sndEvents.peek();

            if (rcv.getSize() > snd.getSize()) {
                // 1. Dequeue SND message from partial receives
                snd = sndEvents.pop();

                // 2. Decrement read bytes from RCV message
                rcv.setSize(rcv.getSize() - snd.getSize());

                // 3. Associate RCV message with SND message
                eventsSndRcv.add(new MyPair<SocketEvent, SocketEvent>(snd, rcv));
            } else {
                // 1. Decrement sent bytes from SND message
                snd.setSize(snd.getSize() - rcv.getSize());
                rcv.setSize(0);

                // 2. Associate RCV message with SND message
                eventsSndRcv.add(new MyPair<SocketEvent, SocketEvent>(snd, rcv));

                // 3. Remove enqueued SND if there are no more bytes remaining
                if (snd.getSize() < 1) sndEvents.pop();
            }
        }

        // Enqueue SND if there are bytes remaining
        if (rcv.getSize() > 0) rcvEvents.add(rcv);
    }

    private static MyPair<Deque<SocketEvent>, Deque<SocketEvent>> getOrCreatePartialEventsPairs(String channelId) {
        if (! partialEventsSndRcv.containsKey(channelId)) {
            Deque<SocketEvent> sndEvents = new ArrayDeque<SocketEvent>();
            Deque<SocketEvent> rcvEvents= new ArrayDeque<SocketEvent>();
            MyPair<Deque<SocketEvent>, Deque<SocketEvent>> pendingEventsPair = new MyPair<Deque<SocketEvent>, Deque<SocketEvent>>(sndEvents, rcvEvents);
            partialEventsSndRcv.put(channelId, pendingEventsPair);
            return pendingEventsPair;
        }

        return partialEventsSndRcv.get(channelId);
    }

    private static void printDataStructures() {

        System.out.println("--- THREAD EVENTS ---");
        for (String t : eventsPerThread.keySet()) {
            System.out.println("#" + t);
            for (Event e : eventsPerThread.get(t)) {
                System.out.println(" " + e.toString());
            }
        }

        System.out.println("\n--- SEND/RECEIVE EVENTS ---");
        for (MyPair<SocketEvent, SocketEvent> se : eventsSndRcv) {
            System.out.println(se.getFirst() + " -> " + se.getSecond());
        }

        System.out.println("\n--- CONNECT/ACCEPT EVENTS ---");
        for (MyPair<SocketEvent, SocketEvent> se : eventsConnAcpt.values()) {
            System.out.println(se.getFirst() + " -> " + se.getSecond());
        }

        System.out.println("\n--- CLOSE/SHUTDOWN EVENTS ---");
        for (MyPair<SocketEvent, SocketEvent> se : eventsCloseShut.values()) {
            System.out.println(se.getFirst() + " -> " + se.getSecond());
        }

        System.out.println("\n--- FORK EVENTS ---");
        for (List<ThreadSyncEvent> fset : eventsFork.values()) {
            for (Event f : fset) {
                System.out.println(f);
            }
        }
        System.out.println("\n--- JOIN EVENTS ---");
        for (List<ThreadSyncEvent> jset : eventsJoin.values()) {
            for (Event j : jset) {
                System.out.println(j);
            }
        }

        if(!sortedByTimestamp.isEmpty()){
            System.out.println("\n--- TIMESTAMP EVENTS ---");
            for (TimestampedEvent se : sortedByTimestamp) {
                System.out.println(se);
            }
            System.out.println("");
        }
    }

    public static void initSolver() throws IOException {
        String solverPath = props.getProperty("solver-bin"); //set up solver path
        System.out.println("[CausalSolver] Init solver: "+solverPath);
        solver = Z3Solver.getInstance();
        solver.init(solverPath);
    }

    public static void buildConstraintModel() throws IOException{
        genProgramOrderConstraints();
        genCommunicationConstraints();
        genForkStartConstraints();
        genJoinExitConstraints();
        boolean useTimestamps = props.getProperty("use-timestamp").equals("true");
        if(useTimestamps){
            genTimestampConstraints();
        }
        genCausalOrderFunction();
        solver.flush();
    }

    private static void genTimestampConstraints() throws IOException {
        System.out.println("[CausalSolver] Generate timestamp constraints");
        HashMap<String, List<SocketEvent>> channelEvents = new HashMap<String, List<SocketEvent>>();

        //filter events per socket channel
        for(TimestampedEvent tse : sortedByTimestamp){
            if(tse instanceof SocketEvent) {
                SocketEvent se = (SocketEvent) tse;
                String socketId = se.getSocket();
                if (!channelEvents.containsKey(se.getSocket())) {
                    List<SocketEvent> tmp = new ArrayList<SocketEvent>();
                    channelEvents.put(socketId, tmp);
                }
                channelEvents.get(socketId).add(se);
            }
        }

        String tagTS = "TS_";
        int counterTS = 0;
        solver.writeComment("TIMESTAMP CONSTRAINTS");

        for(Map.Entry<String,List<SocketEvent>> entry : channelEvents.entrySet())
        {
            List<SocketEvent> events = entry.getValue();
            String tsConstraint = "";
            for(TimestampedEvent se : sortedByTimestamp){
                tsConstraint += (se.toString() + " ");
            }
            solver.writeConstraint(solver.postNamedSoftAssert(solver.cLt(tsConstraint), tagTS + counterTS++));
        }
    }

    public static void genProgramOrderConstraints() throws IOException {
        System.out.println("[CausalSolver] Generate program order constraints");
        String tagPO = "PO_";
        int counterPO = 0;
        int max = 0;
        for (List<Event> l : eventsPerThread.values()) {
            max += l.size();
        }
        solver.writeConstraint(solver.declareIntVar("MAX"));
        solver.writeConstraint(solver.postAssert(solver.cEq("MAX", String.valueOf(max))));

        //generate program order variables and constraints
        for (List<Event> events : eventsPerThread.values()) {
            if (!events.isEmpty()) {
                solver.writeComment("PROGRAM ORDER CONSTRAINTS - THREAD "+events.get(0).getThread());
                String threadOrder = "";
                for (Event e : events) {
                    String var = solver.declareIntVar(e.toString(), "0", "MAX");
                    solver.writeConstraint(var);
                    threadOrder += (e.toString()+" ");

                    //store event in allEvents
                    allEvents.put(e.toString(), e);
                }
                if(events.size() > 1) {
                    solver.writeConstraint(solver.postNamedAssert(solver.cLt(threadOrder),tagPO+counterPO++));
                    solver.writeConstraint(solver.postAssert(solver.cDistinct(threadOrder)));
                }
            }
        }
    }

    public static void genCommunicationConstraints() throws IOException {
        System.out.println("[CausalSolver] Generate communication constraints");
        String tagSND_RCV = "SR_";
        int counterSND_RCV = 0;
        solver.writeComment("COMMUNICATION CONSTRAINTS - SEND / RECEIVE");
        for (MyPair<SocketEvent, SocketEvent> pair : eventsSndRcv) {
            if(pair.getFirst()!= null && pair.getSecond()!=null) {
                pair.getSecond().setDependency(pair.getFirst().getId()); //set dependency
                String cnst = solver.cLt(pair.getFirst().toString(), pair.getSecond().toString());
                solver.writeConstraint(solver.postNamedAssert(cnst,tagSND_RCV+counterSND_RCV++));
            }
        }

        solver.writeComment("COMMUNICATION CONSTRAINTS - CONNECT / ACCEPT");
        String tagCON_ACC = "CA_";
        int counterCON_ACC = 0;
        for (MyPair<SocketEvent, SocketEvent> pair : eventsConnAcpt.values()) {
            if(pair.getFirst()!= null && pair.getSecond()!=null) {
                pair.getSecond().setDependency(pair.getFirst().getId()); //set dependency
                String cnst = solver.cLt(pair.getFirst().toString(), pair.getSecond().toString());
                solver.writeConstraint(solver.postNamedAssert(cnst,tagCON_ACC+counterCON_ACC++));
            }
        }

        solver.writeComment("COMMUNICATION CONSTRAINTS - CLOSE / SHUTDOWN");
        String tagCLS_SHT = "CS_";
        int counterCLS_SHT = 0;
        for (MyPair<SocketEvent, SocketEvent> pair : eventsCloseShut.values()) {
            if(pair.getFirst()!= null && pair.getSecond()!=null) {
                pair.getSecond().setDependency(pair.getFirst().getId()); //set dependency
                String cnst = solver.cLt(pair.getFirst().toString(), pair.getSecond().toString());
                solver.writeConstraint(solver.postNamedAssert(cnst,tagCLS_SHT+counterCLS_SHT++));
            }
        }
    }

    public static void genForkStartConstraints() throws IOException {
        System.out.println("[CausalSolver] Generate fork-start constraints");
        String tagFRK_STR = "FS_";
        int counterFRK_STR = 0;
        solver.writeComment("FORK-START CONSTRAINTS");
        for(List<ThreadSyncEvent> l : eventsFork.values()){
            for(ThreadSyncEvent e : l){
                String startEvent = "START_"+e.getChild();
                String cnst = solver.cLt(e.toString(), startEvent);
                solver.writeConstraint(solver.postNamedAssert(cnst,tagFRK_STR+counterFRK_STR++));
                //set dependency
                allEvents.get(startEvent).setDependency(e.getId());
            }
        }
    }

    public static void genJoinExitConstraints() throws IOException {
        System.out.println("[CausalSolver] Generate join-exit constraints");
        solver.writeComment("JOIN-END CONSTRAINTS");
        String tagJOIN_END = "JE_";
        int counterJOIN_END = 0;
        for(List<ThreadSyncEvent> l : eventsJoin.values()){
            for(ThreadSyncEvent e : l){
                String endEvent = "END_"+e.getChild();
                String cnst = solver.cLt(endEvent, e.toString());
                solver.writeConstraint(solver.postNamedAssert(cnst,tagJOIN_END+counterJOIN_END++));
                //set dependency
                allEvents.get(endEvent).setDependency(e.getId());
            }
        }
    }

    /**
     * Objective function consists in minimizing the order (logical timestamp) of each event
     * in such a way that preserves the happens-before constraint
     * @throws IOException
     */
    public static void genCausalOrderFunction() throws IOException {
        System.out.println("[CausalSolver] Generate causality objective function");
        solver.writeComment("CAUSALITY OBJECTIVE FUNCTION");
        solver.writeConstraint(solver.cMinimize(solver.cSummation(allEvents.keySet())));
    }

    public static void parseSolverOutput(){
        String output = solver.readOutputLine();
        while(!output.equals("") && !output.equals(")")){
            //System.out.println(output);

            //it's an event - parse event reference and logical causal order
            if(output.contains("(define-fun")){
                String[] content = output.split(" ");
                String var = content[3];

                if(var.startsWith(EventType.CREATE.toString())
                        || var.startsWith(EventType.START.toString())
                        || var.startsWith(EventType.END.toString())
                        || var.startsWith(EventType.JOIN.toString())
                        || var.startsWith(EventType.LOG.toString())
                        || var.startsWith(EventType.READ.toString()+"_")
                        || var.startsWith(EventType.WRITE.toString()+"_")
                        || var.startsWith(EventType.SND.toString())
                        || var.startsWith(EventType.RCV.toString())
                        || var.startsWith(EventType.CONNECT.toString())
                        || var.startsWith(EventType.ACCEPT.toString())
                        || var.startsWith(EventType.CLOSE.toString())
                        || var.startsWith(EventType.SHUTDOWN.toString())
                        || var.startsWith(EventType.HNDLBEG.toString())
                        || var.startsWith(EventType.HNDLEND.toString())){
                    output = solver.readOutputLine().trim();
                    int endPos = output.indexOf(")");
                    int order = Integer.valueOf(output.substring(0, endPos));

                    if (allEvents.containsKey(var))
                        allEvents.get(var).setOrder(order);
                }
            }
            else if(output.contains("error"))
                System.out.println(output);

            output = solver.readOutputLine();
        }
    }

    public static void outputCausalOrderJSON(){
        TreeSet<Event> orderedEvents = new TreeSet<Event>(allEvents.values());
        try {
            FileWriter outfile = new FileWriter(new File(props.getProperty("output-file")));
            JSONArray jsonEvents = new JSONArray();
            for (Event e : orderedEvents) {
                JSONObject json = new JSONObject();
                json.put("type", e.getType().toString());
                json.put("thread", e.getThread());
                json.put("pid", e.getPid());
                json.put("order", e.getOrder());
                json.put("id",e.getId());
                json.put("dependency", e.getDependency() == null ? JSONObject.NULL : e.getDependency());
                json.putOpt("data", e.getData());

                if (e instanceof SocketEvent) {
                    SocketEvent se = (SocketEvent) e;
                    json.putOpt("timestamp", se.getTimestamp());
                    json.putOpt("src", se.getSrc());
                    json.putOpt("src_port", se.getSrc_port());
                    json.putOpt("dst", se.getDst());
                    json.putOpt("dst_port", se.getDst_port());
                    json.putOpt("src", se.getSrc());
                    json.put("socket", se.getSocket());
                    json.putOpt("message", se.getMessage());
                }
                System.out.println(json.toString());
                jsonEvents.put(json);
            }
            outfile.write(jsonEvents.toString());
            outfile.flush();
            outfile.close();
        }
        catch (JSONException exc){
            exc.printStackTrace();
        }
        catch (IOException exc){
            exc.printStackTrace();
        }
    }
}
