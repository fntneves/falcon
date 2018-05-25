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
        return this.var;
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
        json.put( "variable", this.var );
        //json.put("lineOfCode", lineOfCode);

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
        return ( ( this.getType() == EventType.WRITE || e.getType() == EventType.WRITE )
                        && this.getNodeId().equals( e.getNodeId() )
                        //&& !thread.equals(e.getThread())
                        && this.var.equals( e.getVariable() )
                        && ( !this.getThread().equals( e.getThread() ) || this.getEventId() != e.getEventId() ) );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o == this )
            return true;

        if ( o == null || getClass() != o.getClass() )
            return false;

        RWEvent tmp = (RWEvent) o;
        return ( tmp.getLineOfCode().equals( this.getLineOfCode() )
                        && tmp.getVariable().equals( this.var )
                        && tmp.getEventId() == this.getEventId()
        );
    }

    @Override
    public String toString()
    {
        String res = this.getType() + "_" + this.var + "_" + this.getThread() + "_" + this.getEventId() + "@" + this.getLineOfCode();
        return res;
    }
}
