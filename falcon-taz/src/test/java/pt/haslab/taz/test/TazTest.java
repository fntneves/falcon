package pt.haslab.taz.test;

import org.json.JSONException;
import org.junit.Test;
import pt.haslab.taz.TraceProcessor;
import pt.haslab.taz.events.Event;
import pt.haslab.taz.events.MyPair;
import pt.haslab.taz.events.RWEvent;
import pt.haslab.taz.events.SyncEvent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by nunomachado on 08/03/18.
 */
public class TazTest {

    public static void main(String args[]){

        try {
            TraceProcessor processor = TraceProcessor.INSTANCE;
            File file = new File("/Users/nunomachado/Dropbox/PosDoc_UMinho/falcon/logs/zktrace_full.log");//processor.getClass().getClassLoader().getResource("testEventTrace.txt").getFile());
            System.out.println("Test file: "+file.getAbsolutePath());
            processor.loadEventTrace(file.getAbsolutePath());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void loadTraces() throws IOException, JSONException {
        TraceProcessor processor = TraceProcessor.INSTANCE;
        File file = new File(processor.getClass().getClassLoader().getResource("testEventTrace.txt").getFile());
        processor.loadEventTrace(file.getAbsolutePath());

        //expected results ---
        int expectedTotal = 41;
        int expectedT1_N1 = 8;
        int expectedT2_N1 = 13;
        int expectedT1_N2 = 13;
        int expectedT2_N2 = 7;

        int expectedLockPairs = 5;
        int expectedSndRcv = 5;
        int expectedFork = 2;
        int expectedJoin = 1;
        int expectedR = 2;
        int expectedW = 2;
        int expectedWait = 1;
        int expectedNotify = 1;
        int expectedConnAcpt = 1;
        int expectedCloseShut = 1;
        //------

        int countTotal = 0;
        for(List<Event> list : processor.eventsPerThread.values()){
            countTotal += list.size();
        }
        assertTrue("Total number of events loaded = "+countTotal+" (expected "+expectedTotal+")", countTotal == expectedTotal);

        assertTrue("#Events in thread T1@N1 = "+processor.eventsPerThread.get("T1@N1").size()+" (expected "+expectedT1_N1+")",
                processor.eventsPerThread.get("T1@N1").size() == expectedT1_N1);

        assertTrue("#Events in thread T2@N1 = "+processor.eventsPerThread.get("T2@N1").size()+" (expected "+expectedT2_N1+")",
                processor.eventsPerThread.get("T2@N1").size() == expectedT2_N1);

        assertTrue("#Events in thread T1@N2 = "+processor.eventsPerThread.get("T1@N2").size()+" (expected "+expectedT1_N2+")",
                processor.eventsPerThread.get("T1@N2").size() == expectedT1_N2);

        assertTrue("#Events in thread T2@N2 = "+processor.eventsPerThread.get("T2@N2").size()+" (expected "+expectedT2_N2+")",
                processor.eventsPerThread.get("T2@N2").size() == expectedT2_N2);

        int countLockPairs = 0;
        for (List<MyPair<SyncEvent, SyncEvent>> pairs : processor.lockEvents.values()) {
            for (MyPair<SyncEvent,SyncEvent> p : pairs) {
              if(p.getFirst() != null && p.getSecond() != null)
                  countLockPairs++;
            }
        }
        assertTrue("#Locking pairs = "+countLockPairs+" (expected "+expectedLockPairs+")",
                countLockPairs == expectedLockPairs);

        assertTrue("#SND/RCV pairs = "+processor.msgEvents.values().size()+" (expected "+expectedSndRcv+")",
                processor.msgEvents.values().size() == expectedSndRcv);

        assertTrue("#CONNECT/ACCEPT pairs = "+processor.connAcptEvents.values().size()+" (expected "+expectedConnAcpt+")",
                processor.connAcptEvents.values().size() == expectedConnAcpt);

        assertTrue("#CLOSE/SHUTDOWN pairs = "+processor.closeShutEvents.values().size()+" (expected "+expectedCloseShut+")",
                processor.closeShutEvents.values().size() == expectedCloseShut);

        int countR = 0;
        for (List<RWEvent> rlist : processor.readEvents.values()) {
            countR += rlist.size();
        }
        assertTrue("#Events read = "+countR+" (expected "+expectedR+")",
                countR == expectedR);

        int countW = 0;
        for (List<RWEvent> wlist : processor.writeEvents.values()) {
            countW += wlist.size();
        }
        assertTrue("#Events write = "+countW+" (expected "+expectedW+")",
                countW == expectedW);

        assertTrue("#Events fork = "+processor.forkEvents.values().size()+" (expected "+expectedFork+")",
                processor.forkEvents.values().size() == expectedFork);

        assertTrue("#Events join = "+processor.joinEvents.values().size()+" (expected "+expectedJoin+")",
                processor.joinEvents.values().size() == expectedJoin);

        int countWait = 0;
        for (List<SyncEvent> wlist : processor.waitEvents.values()) {
            countWait += wlist.size();
        }
        assertTrue("#Events wait = "+countWait+" (expected "+expectedWait+")",
                countWait == expectedWait);

        int countNotify = 0;
        for (List<SyncEvent> nlist : processor.notifyEvents.values()) {
            countNotify += nlist.size();
        }
        assertTrue("#Events notify = "+countNotify+" (expected "+expectedNotify+")",
                countNotify == expectedNotify);

        System.out.println("[OK] Test loadTraces()");
    }
}
