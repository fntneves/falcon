package pt.haslab.causalSolver.events;

/**
 * Created by nunomachado on 30/03/17.
 */
public enum EventType {
    //thread events
    CREATE("CREATE"),
    START("START"),
    END("END"),
    JOIN("JOIN"),
    LOG("LOG"),

    //access events
    READ("R"),
    WRITE("W"),

    //communication events
    SND("SND"),
    RCV("RCV"),
    CLOSE("CLOSE"),
    SHUTDOWN("SHUTDOWN"),
    CONNECT("CONNECT"),
    ACCEPT("ACCEPT"),

    //socket handling partial-order events
    HNDLBEG("HANDLERBEGIN"),
    HNDLEND("HANDLEREND");



    private final String desc;

    private EventType(String l){
        this.desc = l;
    }

    @Override
    public String toString() {
        return this.desc;
    }


    /**
     * Translates a string representing the type of child
     * the corresponding EventType enum element.
     * @param type
     * @return
     */
    public static EventType getEventType(String type){

        if(type.equals("CREATE"))
            return EventType.CREATE;
        else if(type.equals("START"))
            return EventType.START;
        else if(type.equals("END"))
            return EventType.END;
        else if(type.equals("JOIN"))
            return EventType.JOIN;
        else if(type.equals("LOG"))
            return EventType.LOG;
        else if(type.equals("R"))
            return EventType.READ;
        else if(type.equals("W"))
            return EventType.WRITE;
        else if(type.equals("SND"))
            return EventType.SND;
        else if(type.equals("RCV"))
            return EventType.RCV;
        else if(type.equals("CONNECT"))
            return EventType.CONNECT;
        else if(type.equals("ACCEPT"))
            return EventType.ACCEPT;
        else if(type.equals("CLOSE"))
            return EventType.CLOSE;
        else if(type.equals("SHUTDOWN"))
            return EventType.SHUTDOWN;
        else if(type.equals("HANDLERBEGIN"))
            return EventType.HNDLBEG;
        else if(type.equals("HANDLEREND"))
            return EventType.HNDLEND;
        else
            return null;
    }
}
