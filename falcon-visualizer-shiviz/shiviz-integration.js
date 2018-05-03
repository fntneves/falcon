function toShivizLogEvents(logObject) {
  var vectorTimestamps = {};
  var dependencies = {};
  var logEvents = [];
  var graph = new ModelGraph();

  var fieldsGenerators = initFieldsGenerators();

  for (const i in logObject) {
    const logEntry = logObject[i];
    let vectorTimestamp = vectorTimestamps[logEntry.thread];

    if (vectorTimestamp == undefined) {
      let clock = {};
      clock[logEntry.thread] = 0;
      vectorTimestamp = new VectorTimestamp(clock, logEntry.thread);
    }

    var parentNode = undefined;

    if (logEntry.dependency != null) {
      /*const recvVectorTimestamp = dependencies[logEntry.dependency];
      vectorTimestamp = vectorTimestamp.update(recvVectorTimestamp);*/

      parentNode = dependencies[logEntry.dependency];
      const recvVectorTimestamp = parentNode.getFirstLogEvent().getVectorTimestamp();
      vectorTimestamp = vectorTimestamp.update(recvVectorTimestamp);
    }

    while (vectorTimestamp.getOwnTime() < logEntry.order) {
      vectorTimestamp = vectorTimestamp.increment();
    }

    /* VectorTimestamp is immutable */
    vectorTimestamps[logEntry.thread] = vectorTimestamp;

    const fields = fieldsGenerators[logEntry.type](logEntry);
    const logEvent = new LogEvent(logEntry.type, vectorTimestamp, i, fields);
    logEvent.pid = logEntry.pid;

    const modelNode = graph.addLogEvent(logEvent, parentNode);
    logEvents.push(logEvent);

    if (logEntry.type == "CONNECT" || logEntry.type == "SND") {
      /* VectorTimestamp is immutable - clone not needed */
      dependencies[logEntry.id] = /*vectorTimestamp;*/ modelNode;
    }

    //logEvents.push(new LogEvent(logEntry.type, vectorTimestamp, i, fields));
  }
  return {"logEvents": logEvents, "graph": graph};
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
/* ----------

function mapThreadsToPids(logObject) {
  var threadsToPid = {}

  for (const i in logObject) {
    const logEntry = logObject[i];

    threadsToPid[logEntry.thread] = logEntry.pid;
  }

  return threadsToPid;
}*/
