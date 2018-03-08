package pt.haslab.causalSolver.events;

/**
 * Created by nunomachado on 07/04/17.
 */
public class HandlerEvent extends Event{
    String method;
    long counter;

    public HandlerEvent(String thread, EventType type, String method, long counter) {
        super(thread,type);
        this.method = method;
        this.counter = counter;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    @Override
    public String toString() {
        String res = type+"_"+thread+"_"+method+"_"+counter;
        return res;
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        HandlerEvent tmp = (HandlerEvent)o;
        return (tmp.getThread() == this.thread
                && tmp.getType() == this.type
                && tmp.getCounter() == this.counter
                && tmp.getMethod() == this.method
        );
    }
}
