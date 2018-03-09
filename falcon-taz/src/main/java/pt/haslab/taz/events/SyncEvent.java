package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 05/03/18.
 *
 * Represents synchronization events.
 * SyncEvent is associated with event types  LOCK, UNLOCK, NOTIFY, NOTIFYALL and WAIT.
 */
public class SyncEvent extends Event {
    /* reference of the mutex object (lock or monitor) accessed */
    String var;

    /* line of code of the event, with format "className.methodName.lineOfCode" */
    String loc;

    public SyncEvent(String timestamp, EventType type, String thread, int eventNumber, String variable, String lineOfCode) {
        super(timestamp, type, thread, eventNumber);
        this.var = variable;
        this.loc = lineOfCode;
    }

    public SyncEvent(Event e){
        super(e);
        this.var = "";
        this.loc = "";
    }

    public String getLoc() {
        return loc;
    }

    public void setLineOfCode(String loc) {
        this.loc = loc;
    }

    public String getVariable() {
        return var;
    }

    public void setVariable(String variable) {
        this.var = variable;
    }

    @Override
    public String toString() {
        String res = type+"_"+var+"_"+thread+"_"+eventNumber+"@"+loc;
        return res;
    }

    /**
     * Returns a JSONObject representing the event.
     * @return
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = super.toJSONObject();
        json.put("variable", var);
        json.put("loc", loc);

        return json;
    }
}
