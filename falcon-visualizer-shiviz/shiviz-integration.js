// TODO: Decide description and optional fields to pass to LogEvent constructor
function toShivizLogEvents(log) {
  var vectorClocks = {};
  var dependencies = {};
  var logEvents = []

  for (const i in log) {
    const logEntry = log[i];
    let vectorClock = vectorClocks[logEntry.thread];

    if (vectorClock == undefined) {
      let clock = {};
      clock[logEntry.thread] = 0;
      vectorClock = new VectorTimestamp(clock, logEntry.thread);
    }

    if (logEntry.dependency != null) {
      const recvVectorClock = dependencies[logEntry.dependency];
      vectorClock = vectorClock.update(recvVectorClock);
    }
    vectorClock = vectorClock.increment();
    /* VectorTimestamp is immutable */
    vectorClocks[logEntry.thread] = vectorClock;

    if (logEntry.type == "CONNECT" || logEntry.type == "SND") {
      /* VectorTimestamp is immutable - clone not needed */
      dependencies[logEntry.id] = vectorClock;
    }
    logEvents.push(new LogEvent(logEntry.type, vectorClock, i));
  }
  return logEvents;
}
