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

    let parentNode = undefined;

    if (logEntry.dependency != null) {
      /*const recvVectorTimestamp = dependencies[logEntry.dependency];
      vectorTimestamp = vectorTimestamp.update(recvVectorTimestamp);*/

      parentNode = dependencies[logEntry.dependency];
      const recvVectorTimestamp = parentNode.getFirstLogEvent().getVectorTimestamp();
      vectorTimestamp = vectorTimestamp.update(recvVectorTimestamp);
    }

    vectorTimestamp.incrementBy(logEntry.order - vectorTimestamp.getOwnTime());
/*
    while (vectorTimestamp.getOwnTime() < logEntry.order) {
      vectorTimestamp = vectorTimestamp.increment();
    }
*/
    // VectorTimestamp is immutable, so we have to assign the updated vector timestamp
    vectorTimestamps[logEntry.thread] = vectorTimestamp;

    const fields = fieldsGenerators[logEntry.type](logEntry);
    const logEvent = new LogEvent(logEntry.type, vectorTimestamp, i, fields);
    logEvent.pid = logEntry.pid;

    const modelNode = graph.addLogEvent(logEvent, parentNode);
    logEvents.push(logEvent);

    if (["CONNECT", "SND", "CREATE", "END"].includes(logEntry.type)) {
        dependencies[logEntry.id] = modelNode;
    }
    //logEvents.push(new LogEvent(logEntry.type, vectorTimestamp, i, fields));
  }
  return {"logEvents": logEvents, "graph": graph};
}

function initFieldsGenerators() {
  var fieldsGenerators = {};
  fieldsGenerators["START"] = startFieldsGenerator;
  fieldsGenerators["END"] = endFieldsGenerator;
  fieldsGenerators["CREATE"] = createFieldsGenerator;
  fieldsGenerators["JOIN"] = joinFieldsGenerator;
  fieldsGenerators["CONNECT"] = socketFieldsGenerator;
  fieldsGenerators["ACCEPT"] = socketFieldsGenerator;
  fieldsGenerators["CLOSE"] = socketFieldsGenerator;
  fieldsGenerators["SHUTDOWN"] = socketFieldsGenerator;
  fieldsGenerators["SND"] = streamFieldsGenerator;
  fieldsGenerators["RCV"] = streamFieldsGenerator;
  fieldsGenerators["HANDLERBEGIN"] = handlerFieldsGenerator;
  fieldsGenerators["HANDLEREND"] = handlerFieldsGenerator;
  fieldsGenerators["R"] = variableRelatedFieldsGenerator;
  fieldsGenerators["W"] = variableRelatedFieldsGenerator;
  fieldsGenerators["LOCK"] = variableRelatedFieldsGenerator;
  fieldsGenerators["UNLOCK"] = variableRelatedFieldsGenerator;
  fieldsGenerators["WAIT"] = variableRelatedFieldsGenerator;
  fieldsGenerators["NOTIFY"] = variableRelatedFieldsGenerator;
  fieldsGenerators["NOTIFYALL"] = variableRelatedFieldsGenerator;
  fieldsGenerators["LOG"] = logFieldsGenerator;

  return fieldsGenerators;
}

// START
function startFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;

  return fields;
}

// END
function endFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;

  return fields;
}

// CREATE
function createFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
  fields["child"] = logEntry.child;

  return fields;
}

// JOIN
function joinFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
  fields["child"] = logEntry.child;

  return fields;
}

// CONNECT / ACCEPT / CLOSE / SHUTDOWN
function socketFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
  fields["socket"] = logEntry.socket;
  fields["socket_type"] = logEntry.socket_type;

  return fields;
}

// SND (send) / RCV (receive)
function streamFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
  fields["socket"] = logEntry.socket;
  fields["src"] = logEntry.src;
  fields["src_port"] = logEntry.src_port;
  fields["dst"] = logEntry.dst;
  fields["dst_port"] = logEntry.dst_port;
  fields["socket_type"] = logEntry.socket_type;
  fields["message"] = logEntry.message;

  return fields;
}

// HANDLERBEGIN / HANDLEREND
function handlerFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;

  return fields;
}

// R (read) / W (write) / LOCK / UNLOCK / WAIT / NOTIFY / NOTIFYALL
function variableRelatedFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
  fields["variable"] = logEntry.variable;
  fields["loc"] = logEntry.loc;

  return fields;
}

// LOG
function logFieldsGenerator(logEntry) {
  var fields = {};
  fields["timestamp"] = logEntry.timestamp;
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
