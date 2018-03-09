package pt.haslab.taz.events;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nunomachado on 05/03/18.
 *
 * Represents events that are occur during message exchanges between two nodes.
 * SocketEvent is associated with the event types SND, RCV, CLOSE, SHUTDOWN, CONNECT, and ACCEPT.
 */
public class SocketEvent extends Event {

    public enum SocketType { TCP, UDP };

    /* channel with which the socket is associated. Format: "min_ip:min_ip_port-max_ip:max_ip_port" */
    String socket;

    /* IP of the node that sent the message */
    String src;

    /* port from which the source node sent the message */
    int src_port;

    /* IP of the destination node to which the message was sent */
    String dst;

    /* port from which the destination node received the message */
    int dst_port;

    /* type of channel used to transmit the message (see enum SocketType) */
    SocketType socket_type;

    /* message size (in bytes) */
    int size;

    /* message unique identifier */
    String msgId;

    public SocketEvent(String timestamp, EventType type, String thread, int eventNumber, String loc,
                       String socketId,
                       String src,
                       int src_port,
                       String dst,
                       int dst_port,
                       String socket_type,
                       int size,
                       String messageId) {
        super(timestamp, type, thread, eventNumber, loc);
        this.socket = socketId;
        this.src = src;
        this.src_port = src_port;
        this.dst = dst;
        this.dst_port = dst_port;
        this.msgId = messageId;
        this.size = size;
        this.setSocketType(socket_type);
    }

    public SocketEvent(Event e){
        super(e);
        this.socket = "";
        this.src = "";
        this.src_port = 0;
        this.dst = "";
        this.dst_port = 0;
        this.msgId = "";
        this.size = 0;
        this.socket_type = null;
    }

    public String getSocket() {
        return socket;
    }

    /**
     * Gives a string indicating the direction of the socket (src:src_port-dst:dst_port).
     * This differs from getSocket(), which returns the channel identification, regardless of the direction.
     * @return
     */
    public String getDirectedSocket(){
        String channel = src+":"+src_port+"-"+dst+":"+dst_port;
        return channel;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public int getSrcPort() {
        return src_port;
    }

    public void setSrcPort(int src_port) {
        this.src_port = src_port;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public int getDstPort() {
        return dst_port;
    }

    public void setDstPort(int dst_port) {
        this.dst_port = dst_port;
    }

    public SocketType getSocketType() {
        return socket_type;
    }

    public void setSocketType(SocketType socket_type) {
        this.socket_type = socket_type;
    }

    public void setSocketType(String socket_type) {
        if(socket_type.equals("TCP"))
            this.socket_type = SocketType.TCP;
        else
            this.socket_type = SocketType.UDP;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getMessageId() {
        return msgId;
    }

    public void setMessageId(String msgId) {
        this.msgId = msgId;
    }



    /**
     * Indicates whether this SocketEvent conflicts with another SocketEvent.
     * Two socket events conflict if:
     *   i) they are distinct RCV events;
     *  ii) have the same port;
     * iii) occur at the same node.
     * @param e
     * @return boolean indicating whether the two events conflict
     */
    public boolean conflictsWith(SocketEvent e){
        if((type == EventType.SND || e.getType() == EventType.SND))
            return false;

        return (this.dst.equals(e.getDst())
                && this.getDstPort() == e.getDstPort()
                && !this.equals(e));
    }

    @Override
    public boolean equals(Object o){
        if(o == this)
            return true;

        if (o == null || getClass() != o.getClass()) return false;

        SocketEvent tmp = (SocketEvent)o;
        return (tmp.getDst().equals(this.dst)
                && tmp.getMessageId() == this.msgId
                && tmp.getSocket().equals(this.socket)
                && tmp.getThread().equals(this.thread)
                && tmp.getEventNumber() == this.eventNumber
        );
    }

    @Override
    public String toString() {
        String res = type + "_" + socket.hashCode() + ( (msgId != null && !msgId.equals("")) ? ("_" + msgId) : "") + "_" + thread + "_"+eventNumber;
        return res;
    }

    /**
     * Returns a JSONObject representing the event.
     * @return
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = super.toJSONObject();
        json.put("socket", socket);
        json.put("src", src);
        json.put("src_port", src_port);
        json.put("dst", dst);
        json.put("dst_port", dst_port);
        json.put("socket_type", socket_type);
        json.put("size", size);
        json.put("message", msgId);

        return json;
    }
}
