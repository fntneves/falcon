package pt.haslab.causalSolver.events;

/**
 * Created by nunomachado on 31/03/17.
 */
public class SocketEvent extends Event implements TimestampedEvent {
    String timestamp;
    String syscall;
    String syscall_exit;
    String fd;
    String src;
    String src_port;
    String dst;
    String dst_port;
    String socket;
    String message;
    int size;

    public SocketEvent(String thread, EventType type){
        super(thread, type);
        this.timestamp = null;
        this.syscall = null;
        this.syscall_exit = null;
        this.fd = null;
        this.src = null;
        this.src_port = null;
        this.socket = null;
        this.dst = null;
        this.dst_port = null;
        this.socket = null;
        this.message = null;
        this.size = 1;
        this.id = "";
    }

    public SocketEvent(String thread, String pid, EventType type){
        super(thread, pid, type);
        this.timestamp = null;
        this.syscall = null;
        this.syscall_exit = null;
        this.fd = null;
        this.src = null;
        this.src_port = null;
        this.socket = null;
        this.dst = null;
        this.dst_port = null;
        this.socket = null;
        this.message = null;
        this.size = 1;
        this.id = "";
    }

    public SocketEvent(String timestamp,
                       String thread,
                       EventType type,
                       String syscall,
                       String syscall_exit,
                       String fd,
                       String src,
                       String src_port,
                       String dst,
                       String dst_port,
                       String socket,
                       String message) {
        super(thread, type);

        this.timestamp = timestamp;
        this.syscall = syscall;
        this.syscall_exit = syscall_exit;
        this.size = 1;
        this.fd = fd;
        this.src = src;
        this.src_port = src_port;
        this.dst = dst;
        this.dst_port = dst_port;
        this.socket = socket;
        this.message = message;
        long hashId = this.toString().hashCode();
        this.id = String.valueOf((hashId < 0 ? hashId*-1 : hashId));

    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSyscall() {
        return syscall;
    }

    public void setSyscall(String syscall) {
        this.syscall = syscall;
    }

    public String getSyscall_exit() {
        return syscall_exit;
    }

    public void setSyscall_exit(String syscall_exit) {
        this.syscall_exit = syscall_exit;
    }

    public String getFd() {
        return fd;
    }

    public void setFd(String fd) {
        this.fd = fd;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getSrc_port() {
        return src_port;
    }

    public void setSrc_port(String src_port) {
        this.src_port = src_port;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public String getDst_port() {
        return dst_port;
    }

    public void setDst_port(String dst_port) {
        this.dst_port = dst_port;
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChannelId(){
        String endpoint = null;
        if (this.src != null && this.dst != null) {
            endpoint = this.src + ":" + this.src_port + "_" + this.dst + ":" + this.dst_port;
        }

        // Returns socketid|src:srcport_dst:dstport
        return this.socket + (endpoint == null ? "" : "|" + endpoint);
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        SocketEvent tmp = (SocketEvent)o;
        return (tmp.getSocket().equals(this.socket)
                && tmp.getThread().equals(this.thread)
                && tmp.getMessage().equals(this.message)
        );
    }

    @Override
    public String toString() {
        return type + "_" + thread + "_" + this.getChannelId().hashCode() + (message != null ? ("_" + message) : "");
    }
}
