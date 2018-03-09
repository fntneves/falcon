package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 05/03/18.
 *
 * Represents a message handler delimiter.
 * This class is associated with the event types HANDLERBEGIN and HANDLEREND.
 */
public class HandlerEvent extends Event {
    /* line of code of the event, with format "className.methodName.lineOfCode" */
    String loc;

    public HandlerEvent(String timestamp, EventType type, String thread, int eventNumber, String lineOfCode) {
        super(timestamp, type, thread, eventNumber);
        this.loc = lineOfCode;
    }

    public HandlerEvent(Event e){
        super(e);
        this.loc = "";
    }

    public String getLineOfCode() {
        return loc;
    }

    public void setLineOfCode(String loc) {
        this.loc = loc;
    }

    /**
     * Returns a JSONObject representing the event.
     * @return
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = super.toJSONObject();
        json.put("loc", loc);

        return json;
    }

    @Override
    public String toString() {
        String res = type+"_"+thread+"_"+eventNumber+"@"+loc;
        return res;
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        HandlerEvent tmp = (HandlerEvent)o;
        return (tmp.getThread().equals(this.thread)
                && tmp.getType() == this.type
                && tmp.getEventNumber() == this.eventNumber
                && tmp.getLineOfCode().equals(this.loc)
        );
    }
}
