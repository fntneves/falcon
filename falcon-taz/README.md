# TAZ (evenT trAce organiZer) 
![alt text](https://upload.wikimedia.org/wikipedia/en/c/c4/Taz-Looney_Tunes.svg)

TAZ is the component of Falcon responsible for organizing the events traced during an execution into data structures that enable and ease posterior analyses. TAZ receives as input an event trace in JSON format and exposes an object `TraceProcessor` that allows accessing the different events according to their type. 

### Build

```bash
$ mvn package install
```

### Usage

To load an event trace into TAZ, just create an instance of `TraceProcessor` and invoke the method `loadEventTrace` with the path to the event trace file as parameter.
```java
TraceProcessor processor = TraceProcessor.INSTANCE;
processor.loadEventTrace("/path/to/event/trace");

//e.g. get the list of events of thread T1
processor.eventsPerThread.get("T1");
```

## Event Trace JSON API

TAZ is able to parse execution events that contain (a subset of) the following JSON fields: 
- `"type":string` indicates the event type. The current event types supported are: 
- _Thread events:_ **START, END, CREATE, JOIN**
- _Syncrhonization events:_ **LOCK, UNLOCK, WAIT, NOTIFY, NOTIFYALL**
- _Inter-process communication events:_ **CONNECT, ACCEPT, SHUTDOWN, CLOSE, SND, RCV**
- _Read-Write events:_ **R, W** 
- _Logging events:_ **LOG**
- `"timestamp":long` indicates the timestamp of the event. **[ALL]**
- `"thread":string` indicates the name of the thread that executed the event. The string is assumed to have the format **tid@pid**, where **tid** is the thread idenfier (e.g. thread name or id) and **pid** is the parent process identifier (e.g. process pid, machine IP, etc). **[ALL]**
- `"child":string` indicates the name of the thread that was created by the thread that executed the create event. **[CREATE, JOIN]**
- `"socket":string` is a string of format **min_ip:min_ip_port-max_ip:max_ip_port** representing the channel with which the socket is associated. This string is the concatenation of the IPs and corresponding ports in ascending order. **[CONNECT, ACCEPT, SHUTDOWN, CLOSE, SND, RCV]**
- `"src":string` indicates the IP of the source node that sent the message. **[SND, RCV]**
- `"src_port":int` indicates the port from which the source node sent the message. **[SND, RCV]**
- `"dst":string` indicates the IP of the destination node to which the message was sent. **[SND, RCV]**
- `"dst_port":int` indicates the port from which the destination node received the message. **[SND, RCV]**
- `"socket_type":string` indicates the type of channel used to transmit the message. Channel can be either **UDP** or **TCP**. **[CONNECT, ACCEPT, SHUTDOWN, CLOSE, SND, RCV]**
- `"message":string` is a unique identifier of the message. **[SND, RCV]**
- `"size"int` indicates the size (in bytes) of the message. **[SND, RCV]**
- `"variable":string` indicates the name/reference of the variable/object being accessed by a thread. The access can be, for instance, a read/write to a variable or to acquire/release a lock. **[R, W, LOCK, UNLOCK, WAIT, NOTIFY, NOTIFYALL]**
- `"loc":string` indicates the line of code of the event and has format **className.methodName.lineOfCode**. In practice, the line of code can be any value that uniquely identifies the program instruction. **[R, W, LOCK, UNLOCK, WAIT, NOTIFY, NOTIFYALL]**
- `"data"` is a JSONObject containing additional event details. Examples of fields include: `syscall_exit, syscall, socket_type, fd, exit_timestamp, enter_timestamp, message`, etc. **[ALL]**

Finally, TAZ supports the parsing of additional JSON fields, which are optional and typically set solely after applying the causality analysis:
- `"id":long` is a unique identifier of the event. **[ALL]**
- `"dependency":long` indicates the id of the event that *happens-before* this one. Dependency is `null` if the event has no causal depedencies. **[ALL]**
- `"order":long` indicates the logical clock of the event. **[ALL]**

#### JSON Fields per type of event
**START**
- `"timestamp":long`
- `"thread":string`
- `"type":string`

**END**
- `"timestamp":long`
- `"thread":string`
- `"type":string`
- `"data":JSONObject`
- `"event":string`

**CREATE** / **JOIN**
- `"timestamp":long`
- `"thread":string`
- `"type":string`
- `"child":string`

**CONNECT** / **ACCEPT** / **CLOSE** / **SHUTDOWN**
- `"timestamp":long`
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
- `"timestamp":long`
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

**R** *(read)* / **W** *(write)* / **LOCK** / **UNLOCK** / **WAIT** / **NOTIFY** / **NOTIFYALL** 
- `"thread":string`
- `"type":string`
- `"variable":string`
- `"loc":string`

**LOG**
- `"timestamp":long`
- `"thread":string`
- `"type":string`
- `"data":JSONObject`
- `"message":string`

---
## JAVA Objects
After parsing the JSON event trace, TAZ organizes the events into the following objects and data structures.

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
String loc;
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
String var;
}
```
**HandlerEvent** is used for message handler delimiters: HANDLERBEGIN and HANDLEREND. 
```java
class HandlerEvent extends Event {
super()
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


