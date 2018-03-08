package pt.haslab.causalSolver.events;

/**
 * Created by nunomachado on 30/03/17.
 */
public class LogEvent extends Event implements TimestampedEvent {
    String timestamp = null;

    public LogEvent(){ super(); }

    public LogEvent(String thread, EventType type, String timestamp) {
        super(thread, type);
        this.timestamp = timestamp;
    }

    public LogEvent(String thread, String pid, EventType type, String timestamp) {
        super(thread, pid, type);
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        String res = type+"_"+thread + '_' + timestamp;
        return res;
    }

    @Override
    public String getTimestamp() {
        return this.timestamp;
    }
}
