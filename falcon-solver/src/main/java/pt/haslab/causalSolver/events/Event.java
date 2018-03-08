package pt.haslab.causalSolver.events;

import java.util.Comparator;

/**
 * Created by nunomachado on 30/03/17.
 */
public class Event implements Comparable {
    String thread;
    String pid;
    EventType type;
    int order;
    String id;
    String dependency;
    Object data;


    public Event(){}

    public Event(String thread, EventType type) {
        this.thread = thread;
        this.pid = thread;
        this.type = type;
        this.order = -1;
        long hashId = (type+"_"+thread).hashCode();
        this.id = String.valueOf((hashId < 0 ? hashId*-1 : hashId));
        this.dependency = null;
        this.data = null;
    }

    public Event(String thread, String pid, EventType type) {
        this.thread = thread;
        this.pid = pid;
        this.type = type;
        this.order = -1;
        long hashId = (type+"_"+thread).hashCode();
        this.id = String.valueOf((hashId < 0 ? hashId*-1 : hashId));
        this.dependency = null;
        this.data = null;
    }

    public String getPid() {
        return pid;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Object getData() {
        return this.data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String newId) {
        this.id = newId;
    }

    public void setId() {
        if(id.equals("")) {
            long hashId = this.toString().hashCode();
            this.id = String.valueOf((hashId < 0 ? hashId * -1 : hashId));
        }
    }

    public String getDependency() {
        return dependency;
    }

    public void setDependency(String dependency) {
        this.dependency = dependency;
    }


    @Override
    public String toString() {
        String res = type+"_"+thread;
        return res;
    }


    public int compareTo(Object o1) {
        Event e = (Event) o1;

        if(this.order < e.getOrder())
            return -1;
        else if(this.order == e.getOrder() && this.equals(e))
            return 0;
        else
            return 1;
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        Event tmp = (Event)o;
        return (tmp.getThread() == this.thread
                && tmp.getType() == this.type
                && tmp.getOrder() == this.order
        );
    }
}
