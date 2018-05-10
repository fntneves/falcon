package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 05/03/18.
 * Class Event represents a generic event in Taz. All the other event types inherit from this class.
 */
public class Event
                implements Comparable
{
    //--- REQUIRED PARAMETERS ---
    /* event timestamp as given by the trace */
    String timestamp;

    /* type of event (possible types are in EventType class) */
    EventType type;

    /* thread that executed the event */
    String thread;

    /* line of code of the event, with format "className.methodName.lineOfCode" */
    String loc;

    /* indicates that the event is the n-th event in the trace file */
    long eventId;

    //--- OPTIONAL PARAMETERS ---
    /* name of the event that causally precedes this event (useful when drawing space-time diagrams) */
    String dependency;

    /* JSON object with additional event details */
    JSONObject data;

    /* Execution order given by the constraint solver (according to a given criteria).
     *  This variable should only be set after the constraint solving process has taken place. */
    long scheduleOrder;

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
        this.loc = lineOfCode;
        //initially, set scheduleOrder equal to eventId
        //override scheduleOrder after having causal order
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
        this.loc = e.getLineOfCode();
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
        return loc;
    }

    public void setLineOfCode( String loc )
    {
        this.loc = loc;
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
        return scheduleOrder;
    }

    public void setScheduleOrder( int scheduleOrder )
    {
        this.scheduleOrder = scheduleOrder;
    }

    /**
     * Returns the identifier of the node at which the event was executed
     * (this info is obtained by parsing the thread field)
     *
     * @return node identifier
     */
    public String getNodeId()
    {
        int start = thread.indexOf( "@" );
        String node = thread.substring( start + 1 );
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
        String res = type + "_" + thread;
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
        JSONObject json = new JSONObject();
        json.put( "type", type.toString() );
        json.put( "thread", thread );
        json.put( "loc", loc );
        json.put( "order", scheduleOrder );
        json.put( "id", eventId );
        json.put( "timestamp", timestamp );
        json.put( "dependency", dependency == null ? JSONObject.NULL : dependency );
        json.putOpt( "data", data );

        return json;
    }

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
                        && tmp.getLineOfCode().equals( this.loc )
        );
    }
}
