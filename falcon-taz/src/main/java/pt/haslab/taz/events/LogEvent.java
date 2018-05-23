package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 06/05/18.
 * Represents LOG events.
 */
public class LogEvent extends Event
{
    /* message logged by the event */
    String message;

    public LogEvent( String timestamp, EventType type, String thread, int eventNumber, String lineOfCode,
                     String message )
    {
        super( timestamp, type, thread, eventNumber, lineOfCode );
        this.message = message;
    }

    public LogEvent( Event e, String message )
    {
        super( e );
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
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
        json.put( "message", message);

        return json;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o == this )
            return true;

        if ( o == null || getClass() != o.getClass() )
            return false;

        LogEvent tmp = (LogEvent) o;
        return ( tmp.getLineOfCode().equals( this.lineOfCode )
                        && tmp.getMessage().equals( this.message )
                        && tmp.getEventId() == this.eventId
        );
    }

    @Override
    public String toString()
    {
        //since there can be multiple LOG events per thread
        //we need to uniquely identify each one
        String res = type + "_" + eventId + "_" + thread;

        return res;
    }
}
