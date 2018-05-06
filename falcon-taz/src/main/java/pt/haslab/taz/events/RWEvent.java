package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 05/03/18.
 * Represents READ and WRITE events.
 */
public class RWEvent
                extends Event
{
    /* name of the variable accessed */
    String var;

    public RWEvent( String timestamp, EventType type, String thread, int eventNumber, String variable,
                    String lineOfCode )
    {
        super( timestamp, type, thread, eventNumber, lineOfCode );
        this.var = variable;
    }

    public RWEvent( Event e )
    {
        super( e );
        this.var = "";
    }

    public String getVariable()
    {
        return var;
    }

    public void setVariable( String variable )
    {
        this.var = variable;
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
        json.put( "variable", var );
        //json.put("loc", loc);

        return json;
    }

    /**
     * Indicates whether this RWEvent conflicts with another RWEvent.
     * Two RWEvents conflict if:
     * i) occur at the same node;
     * ii) access the same variable;
     * iii) at least one of them is a write.
     *
     * @param e
     * @return boolean indicating whether the two events conflict
     */
    public boolean conflictsWith( RWEvent e )
    {
        return ( ( type == EventType.WRITE || e.getType() == EventType.WRITE )
                        && this.getNodeId().equals( e.getNodeId() )
                        //&& !thread.equals(e.getThread())
                        && var.equals( e.getVariable() )
                        && ( !thread.equals( e.getThread() ) || eventNumber != e.getEventNumber() ) );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o == this )
            return true;

        if ( o == null || getClass() != o.getClass() )
            return false;

        RWEvent tmp = (RWEvent) o;
        return ( tmp.getLineOfCode().equals( this.loc )
                        && tmp.getVariable().equals( this.var )
                        && tmp.getEventNumber() == this.eventNumber
        );
    }

    @Override
    public String toString()
    {
        String res = type + "_" + var + "_" + thread + "_" + eventNumber + "@" + loc;
        return res;
    }
}
