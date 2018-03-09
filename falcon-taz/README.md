# TAZ (evenT trAce organiZer) 
![alt text](https://upload.wikimedia.org/wikipedia/en/c/c4/Taz-Looney_Tunes.svg)

TAZ is a trace processor component that parses a JSON file containing events of an application's execution and organizes them into data structures according to their type. 

### Build

```bash
$ mvn package install
```

### Usage

To load an event trace into TAZ, just create an instance of `TraceProcessor` and invoke method `loadEventTrace` with the pass to the event trace as parameter.
```java
TraceProcessor processor = TraceProcessor.INSTANCE;
processor.loadEventTrace("/path/to/event/trace");

//e.g. access to list of events of thread T1
processor.eventsPerThread.get("T1");
```


The JSON API used by TAZ and the Java data structures that it produces are detailed as follows.

## JSON API
**START**
- `"timestamp":double`
- `"thread":string`
- `"type":string`

**END**
- `"timestamp":double`
- `"thread":string`
- `"type":string`
- `"data":JSONObject`
  - `"event":string`

**CREATE** / **JOIN**
- `"timestamp":double`
- `"thread":string`
- `"type":string`
- `"child":string`

**CONNECT** / **ACCEPT** / **CLOSE** / **SHUTDOWN**
- `"timestamp":double`
- `"thread":string`
- `"type":string`
- `"socket":string`
- `"socket_type":string`
- `"data":JSONObject`
  - `"syscall_exit"`
  - `"syscall"`
  - `"fd"`
  - `"exit_timestamp"`
  - `"enter_timestamp"`

**SND** *(send)* / **RCV** *(receive)*
- `"timestamp":double`
- `"thread":string`
- `"type":string`
- `"socket":string`
- `"src":string`
- `"src_port":int`
- `"dst":string`
- `"dst_port":int`
- `"socket_type":string`
- `"message":string`
- `"size":int`
- `"data":JSONObject`
  - `"syscall_exit"`
  - `"syscall"`
  - `"fd"`
  - `"exit_timestamp"`
  - `"enter_timestamp"`

**HANDLERBEGIN** / **HANDLEREND**
- `"thread":string`
- `"type":string`
- `"counter":int`
- `"loc":string`

**R** *(read)* / **W** *(write)* / **LOCK** / **UNLOCK** / **WAIT** / **NOTIFY** / **NOTIFYALL** 
- `"thread":string`
- `"type":string`
- `"variable":string`
- `"counter":int`
- `"loc":string`

**LOG**
- `"timestamp":double`
- `"thread":string`
- `"type":string`
- `"data":JSONObject`
  - `"message":string`

## Entry Description
- `"timestamp"` is a double value indicating the timestamp of the event.
- `"thread"` is a string indicating the name of the thread that executed the event. The string has format **tid@pid**, where **tid** is the thread id and **pid** is the process/node id.
- `"type"` is a string indicating the type of event. Valid types: **START, END, CREATE, JOIN, CONNECT, ACCEPT, CLOSE, SHUTDOWN, SND, RCV, R, W, LOCK, UNLOCK, WAIT, NOTIFY, NOTIFYALL**.
- `"child"` is a string indicating the name of the thread that was created by the thread that executed the create event.
- `"socket"` is a string of format **min_ip:min_ip_port-max_ip:max_ip_port** representing the channel with which the socket is associated. This string is the concatenation of the ips and corresponding ports in ascending order.
- `"src"` is a string indicating the IP of the source node that sent the message.
- `"src_port"` is an integer indicating the port from which the source node sent the message.
- `"dst"` is a string indicating the IP of the destination node to which the message was sent.
- `"dst_port"` is an integer indicating the port from which the destination node received the message.
- `"socket_type"` is a string indicating the type of channel used to transmit the message. Channel can be either **UDP** or **TCP**.
- `"message"` is a string corresponding to a unique identifier of the message.
- `"size"` is an integer indicating the size (in bytes) of the message.
- `"variable"` is a string indicating the name (reference) of the variable (object) being accessed (i.e. read/written) by a thread.
- `"counter"` is an integer indicating the n-th access to a variable performed by the same thread during the execution. 
- `"loc"` is a string indicating the line of code of the event, with format **className.methodName.lineOfCode**.
- `"data"` is an array of additional event details, which can comprise the following fields: `syscall_exit, syscall, socket_type, fd, exit_timestamp, enter_timestamp, message`.

---
## JAVA Objects

```java
enum EventType {
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

    //message handlers
    HNDLBEG("HANDLERBEGIN"),
    HNDLEND("HANDLEREND"),

    // lock and unlock events
    LOCK("LOCK"),
    UNLOCK("UNLOCK"),

    //thread synchronization events
    WAIT("WAIT"),
    NOTIFY("NOTIFY"),
    NOTIFYALL("NOTIFYALL");
}
```
**Event** is the general class from which all the other event sub-types inherit from. It is also used for LOG events.
```java
class Event {
    String timestamp;
    String thread;
    EventType type;
    String dependency; //indicates the event that causally precedes this event
    int eventNumber; 
    Object data;
    int scheduleOrder; //order given by the solver according to the desired criteria
}
```
**ThreadCreationEvent** is used for CREATE and JOIN events. 
```java
class ThreadCreationEvent extends Event {
    String child;
}
```
**SyncEvent** is used for LOCK, UNLOCK, NOTIFY, NOTIFYALL and WAIT events. 
```java
class SyncEvent extends Event {
    String loc;
    String var;
}
```
**SocketEvent** is used for SND, RCV, CLOSE, SHUTDOWN, CONNECT, and ACCEPT events. 
```java
class SocketEvent extends Event {
    enum SocketType { TCP, UDP };
    
    String socket;
    String src;
    int src_port;
    String dst;
    int dst_port;
    SocketType socket_type;
    int size;
    String message;
}
```
**RWEvent** is used for READ and WRITE events. 
```java
class RWEvent extends Event {
    String loc;
    String var;
}
```
**HandlerEvent** is used for message handler delimiters: HANDLERBEGIN and HANDLEREND. 
```java
class HandlerEvent extends Event {
    String loc;
}
```

### Data Structures 

```java
     /* Map: message id -> pair of events (snd,rcv) */
    public Map<String, MyPair<SocketEvent, SocketEvent>> msgEvents;

    /* Map: socket id -> pair of events (connect,accept) */
    public Map<String, MyPair<SocketEvent, SocketEvent>> connAcptEvents;

    /* Map: socket id -> pair of events (close,shutdown) */
    public Map<String, MyPair<SocketEvent, SocketEvent>> closeShutEvents;

    /* Map: thread -> list of all events in that thread's execution */
    public Map<String, List<Event>> eventsPerThread;

    /* Map: thread -> list of thread's fork events */
    public Map<String, List<ThreadCreationEvent>> forkEvents;

    /* Map: thread -> list of thread's join events */
    public Map<String, List<ThreadCreationEvent>> joinEvents;

    /* Map: string (event.toString) -> Event object */
    public HashMap<String, Event> eventNameToObject;

    /* Map: mutex variable -> list of pairs of locks/unlocks */
    public Map<String, List<MyPair<SyncEvent, SyncEvent>>> lockEvents;

    /* Map: variable -> list of reads to that variable by all threads */
    public Map<String, List<RWEvent>> readEvents;

    /* Map: variable -> list of writes to that variable by all threads */
    public Map<String, List<RWEvent>> writeEvents;

    /* Map: condition variable -> list of thread's wait events */
    public Map<String, List<SyncEvent>> waitEvents;

    /* Map: condition variable -> list of thread's notify events */
    public Map<String, List<SyncEvent>> notifyEvents;

    /* list with socket events ordered by timestamp */
    public TreeSet<Event> sortedByTimestamp;
```
 
 
