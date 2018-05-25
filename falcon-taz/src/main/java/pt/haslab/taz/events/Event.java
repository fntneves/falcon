package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class Event represents a generic event in Taz. All the other event types extend this class.
 *
 * Created by nunomachado on 05/03/18.
 */
public class Event
                implements Comparable
{
    //--- REQUIRED PARAMETERS ---
    /* event timestamp as given by the trace */
    private String timestamp;

    /* type of event (possible types are in EventType class) */
    private EventType type;

    /* thread that executed the event */
    private String thread;

    /* line of code of the event, with format "className.methodName.lineOfCode" */
    private String lineOfCode;

    /* indicates that the event is the n-th event in the trace file */
    private long eventId;

    //--- OPTIONAL PARAMETERS ---
    /* name of the event that causally precedes this event (useful when drawing space-time diagrams) */
    private String dependency;

    /* JSON object with additional event details */
    private JSONObject data;

    /*
     * Execution order should be given by the falcon-solver. Therefore, this variable should only be set if the event
     * trace received as input was the result of the constraint solving procedure.
     */
    private long scheduleOrder;

    public Event()
    {
    }

    public Event( String timestamp, EventType type, String thread, long eventId, String lineOfCode )
    {
        this.timestamp = timestamp;
        this.type = type;
        this.thread = thread;
        this.dependency = null;
        this.eventId = eventId;
        this.data = null;
        this.lineOfCode = lineOfCode;
        /*
         * Initially, set scheduleOrder to be equal to the eventId.
         * Override scheduleOrder after computing the global causal order.
         */
        this.scheduleOrder = eventId;
    }

    public Event( Event e )
    {
        this.timestamp = e.getTimestamp();
        this.type = e.getType();
        this.thread = e.getThread();
        this.dependency = e.getDependency();
        this.eventId = e.getEventId();
        this.data = e.getData();
        this.scheduleOrder = e.getScheduleOrder();
        this.lineOfCode = e.getLineOfCode();
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( String timestamp )
    {
        this.timestamp = timestamp;
    }

    public EventType getType()
    {
        return type;
    }

    public void setType( EventType type )
    {
        this.type = type;
    }

    public String getThread()
    {
        return thread;
    }

    public void setThread( String thread )
    {
        this.thread = thread;
    }

    public String getLineOfCode()
    {
        return lineOfCode;
    }

    public void setLineOfCode( String loc )
    {
        this.lineOfCode = loc;
    }

    public String getDependency()
    {
        return dependency;
    }

    public void setDependency( String dependency )
    {
        this.dependency = dependency;
    }

    public void setDependency( Event dependency )
    {
        this.dependency = String.valueOf( dependency.getEventId() );
    }

    public long getEventId()
    {
        return eventId;
    }

    public void setEventId( int eventId )
    {
        this.eventId = eventId;
    }

    public JSONObject getData()
    {
        return data;
    }

    public Object getDataField( String jsonField )
                    throws JSONException
    {
        if ( this.data != null && this.data.has( jsonField ) )
            return this.data.get( jsonField );
        else
            return null;
    }

    public void setData( JSONObject data )
    {
        this.data = data;
    }

    public long getScheduleOrder()
    {
        return this.scheduleOrder;
    }

    public void setScheduleOrder( int scheduleOrder )
    {
        this.scheduleOrder = scheduleOrder;
    }

    /**
     * Returns the hostname of the node at which the event was executed
     * (this info is obtained by parsing the thread field).
     *
     * @return the node identifier which corresponds to the hostname.
     */
    public String getNodeId()
    {
        int start = this.thread.indexOf( "@" );
        String node = this.thread.substring( start + 1 );
        return node;
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        String res = this.type + "_" + this.thread;
        return res;
    }

    /**
     * Encodes the event into a JSONObject.
     *
     * @return  a JSONObject representing the event.
     */
    public JSONObject toJSONObject()
                    throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put( "type", this.type.toString() );
        json.put( "thread", this.thread );
        json.put( "loc", this.lineOfCode );
        json.put( "order", this.scheduleOrder );
        json.put( "id", this.eventId );
        json.put( "timestamp", this.timestamp );
        json.put( "dependency", this.dependency == null ? JSONObject.NULL : this.dependency );
        json.putOpt( "data", this.data );

        return json;
    }

    /**
     * Event comparator based on the logical clock.
     *
     * @param o1  another event.
     * @return    an integer indicating whether the logical clock of this event is lower, equal, or higher than that
     *            of another event.
     */
    public int compareTo( Object o1 )
    {
        Event e = (Event) o1;

        if ( this.scheduleOrder < e.getScheduleOrder() )
            return -1;
        else if ( this.scheduleOrder == e.getScheduleOrder() && this.equals( e ) )
            return 0;
        else
            return 1;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o == this )
            return true;

        if ( o == null || getClass() != o.getClass() )
            return false;

        Event tmp = (Event) o;
        return ( tmp.getThread() == this.thread
                        && tmp.getType() == this.type
                        && tmp.getScheduleOrder() == this.scheduleOrder
                        && tmp.getLineOfCode().equals( this.lineOfCode )
        );
    }
}
