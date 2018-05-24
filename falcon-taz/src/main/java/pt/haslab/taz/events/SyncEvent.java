package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 05/03/18.
 * Represents synchronization events.
 * SyncEvent is associated with event types  LOCK, UNLOCK, NOTIFY, NOTIFYALL and WAIT.
 */
public class SyncEvent
                extends Event
{
    /* reference of the mutex object (lock or monitor) accessed */
    String var;

    public SyncEvent( String timestamp, EventType type, String thread, int eventNumber, String variable,
                      String lineOfCode )
    {
        super( timestamp, type, thread, eventNumber, lineOfCode );
        this.var = variable;
    }

    public SyncEvent( Event e )
    {
        super( e );
        this.var = "";
    }

    public String getVariable()
    {
        return this.var;
    }

    public void setVariable( String variable )
    {
        this.var = variable;
    }

    @Override
    public String toString()
    {
        String res = this.getType() + "_" + this.var + "_" + this.getThread() + "_" + this.getEventId() + "@" + this.getLineOfCode();
        return res;
    }

    /**
     * Returns a JSONObject representing the event.
     *
     * @return
     */
    public JSONObject toJSONObject()
                    throws JSONException
    {
        JSONObject json = super.toJSONObject();
        json.put( "variable", this.var );

        return json;
    }
}
