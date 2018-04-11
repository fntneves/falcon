package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 05/03/18.
 *
 * Represents thread creation points.
 * ThreadCreationEvent is associated with event types CREATE and JOIN.
 */
public class ThreadCreationEvent extends Event {
    String child;

    public ThreadCreationEvent(String timestamp, EventType type, String thread, int eventNumber, String child, String lineOfCode) {
        super(timestamp, type, thread, eventNumber, lineOfCode);
        this.child = child;
    }

    public ThreadCreationEvent(Event e) {
        super(e);
        child = "";
    }

    public String getChildThread() {
        return child;
    }

    public void setChildThread(String child) {
        this.child = child;
    }

    @Override
    public String toString() {
        String res = type+"_"+thread+"_"+child;
        return res;
    }

    /**
     * Returns a JSONObject representing the event.
     * @return
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = super.toJSONObject();
        json.put("child", child);

        return json;
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        ThreadCreationEvent tmp = (ThreadCreationEvent)o;
        return (tmp.getThread().equals(this.thread)
                && tmp.getType() == this.type
                && tmp.getChildThread().equals(this.child)
        );
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
