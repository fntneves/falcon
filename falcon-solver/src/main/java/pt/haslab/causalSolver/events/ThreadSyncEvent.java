package pt.haslab.causalSolver.events;

/**
 * Created by nunomachado on 31/03/17.
 */
public class ThreadSyncEvent extends Event{
    String child;

    public ThreadSyncEvent(String thread, EventType type, String child) {
        super(thread, type);
        this.child = child;
    }

    public String getChild() {
        return child;
    }

    public void setChild(String child) {
        this.child = child;
    }

    @Override
    public String toString() {
        String res = type+"_"+thread+"_"+child;
        return res;
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        Event tmp = (Event)o;
        return (tmp.getThread() == this.thread
                && tmp.getType() == this.type
                && tmp.getThread() == this.child
        );
    }
}
