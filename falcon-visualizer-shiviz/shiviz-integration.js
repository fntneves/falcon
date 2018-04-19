function toShivizLogEvents(logObject) {
  var vectorClocks = {};
  var dependencies = {};
  var logEvents = [];

  var fieldsGenerators = initFieldsGenerators();

  for (const i in logObject) {
    const logEntry = logObject[i];
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

    const fields = fieldsGenerators[logEntry.type](logEntry);
    logEvents.push(new LogEvent(logEntry.type, vectorClock, i, fields));
  }
  return logEvents;
}

function initFieldsGenerators() {
  var fieldsGenerators = {};
  fieldsGenerators["START"] = startFieldsGenerator;
  fieldsGenerators["END"] = endFieldsGenerator;
  fieldsGenerators["CONNECT"] = socketFieldsGenerator;
  fieldsGenerators["ACCEPT"] = socketFieldsGenerator;
  fieldsGenerators["CLOSE"] = socketFieldsGenerator;
  fieldsGenerators["SND"] = streamFieldsGenerator;
  fieldsGenerators["RCV"] = streamFieldsGenerator;
  fieldsGenerators["LOG"] = logFieldsGenerator;

  return fieldsGenerators;
}

// START
function startFieldsGenerator(logEntry) {
  var fields = {};

  return fields;
}

// END
function endFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;

  return fields;
}

// CONNECT / ACCEPT / CLOSE
function socketFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
  fields["socket"] = logEntry.socket;

  return fields;
}

// SND / RCV
function streamFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
  fields["socket"] = logEntry.socket;
  fields["src"] = logEntry.src;
  fields["src_port"] = logEntry.src_port;
  fields["dst"] = logEntry.dst;
  fields["dst_port"] = logEntry.dst_port;
  fields["message"] = logEntry.message;

  return fields;
}

// LOG
function logFieldsGenerator(logEntry) {
  var fields = {};
  fields["message"] = logEntry.data.message;

  return fields;
}
// ----------

function mapThreadsToPids(logObject) {
  var threadsToPid = {}

  for (const i in logObject) {
    const logEntry = logObject[i];

    threadsToPid[logEntry.thread] = logEntry.pid;
  }

  return threadsToPid;
}
