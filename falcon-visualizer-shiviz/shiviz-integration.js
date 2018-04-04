
function vectorclocks_generator(log) {

  var vectorclocks = {};
  var dependencies = {};

  for (var i in log) {
    var logevent = log[i];

    var vectorclock = vectorclocks[logevent.thread];

    if (vectorclock == undefined) {

      var clock = {};
      clock[logevent.thread] = 0;
      vectorclock = new VectorTimestamp(clock, logevent.thread);
    }

    if (logevent.dependency != null) {
      var recv_vectorclock = dependencies[logevent.dependency];
      vectorclock = vectorclock.update(recv_vectorclock);
    }
    vectorclock = vectorclock.increment();
    /* VectorTimestamp is immutable */
    vectorclocks[logevent.thread] = vectorclock;

    logevent["vectorclock"] = vectorclock.getClock();

    if (logevent.type == "CONNECT" || logevent.type == "SND") {
      /* VectorTimestamp is immutable - clone not needed */
      dependencies[logevent.id] = vectorclock;
    }
  }

  return log;
}
