package pt.haslab.causalSolver.events;

/**
 * Created by nunomachado on 31/03/17.
 */
public class RWEvent extends Event {
    int loc;
    String var;
    long counter;

    public RWEvent(String thread, EventType type, int loc, String var, long counter) {
        super(thread, type);
        this.loc = loc;
        this.var = var;
        this.counter = counter;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public String getVariable() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    /**
     * Returns the node id of a RW child
     * (this info is indicated in the thread)
     * @return
     */
    public String getNode(){
        int start = thread.indexOf("@");
        String node = thread.substring(start+1);
        return node;
    }

    public boolean conflictsWith(RWEvent e){
        return ((type == EventType.WRITE || e.getType() == EventType.WRITE)
                && this.getNode().equals(e.getNode())
                //&& !thread.equals(e.getThread())
                && var.equals(e.getVariable())
                && (!thread.equals(e.getThread()) || counter!=(e.getCounter())));
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        RWEvent tmp = (RWEvent) o;
        return (tmp.getLoc() == this.loc
                && tmp.getVariable() == this.var
                && tmp.getCounter() == this.counter
        );
    }

    @Override
    public String toString() {
        String res = type+"_"+thread+"_"+var+"_"+counter+"@"+loc;
        return res;
    }
}
