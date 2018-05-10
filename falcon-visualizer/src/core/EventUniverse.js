import EventFactory from './event/EventFactory';

const eventsConstructor = Symbol('constructEvents');
const clusterThreadsByPid = Symbol('clusterThreadsByPid');
const filterUnrelevantThreads = Symbol('filterUnrelevantThreads');

export default class EventUniverse {

  constructor(data) {
    this.events = {};
    this.orderedEvents = [];
    this.orderedThreads = [];
    this[eventsConstructor](data);
  }

  get count() {
    return Object.keys(this.events).length;
  }

  get maxClock() {
    return this.orderedEvents[this.orderedEvents.length - 1].clock;
  }

  at(clock) {
    return this.subset(clock, clock + 1);
  }

  subset(startClock, endClock) {
    return this.orderedEvents.filter(e => e.clock >= startClock && e.clock < endClock);
  }

  event(id) {
    return this.events[id] || null;
  }

  [eventsConstructor](data) {
    data.forEach((record) => {
      const event = EventFactory.build(record);
      const threadName = event.getThreadIdentifier();
      this.events[event.id] = event;
      this.orderedEvents.push(event);

      if (this.getThreadOrderedIndex(threadName) < 0) {
        this.orderedThreads.push({
          pid: event.pid,
          thread: event.thread,
        });
      }
    });
    this[clusterThreadsByPid]();
    // this[filterUnrelevantThreads]();
  }

  [clusterThreadsByPid]() {
    this.orderedThreads = this.orderedThreads.sort((a, b) => {
      // PIDs are the same, so we need to compare threads.
      if (a.pid === b.pid) {
        if (a.thread < b.thread) {
          return -1;
        }
        return a.thread === b.thread ? 0 : 1;
      }

      // If PIDs are different, then compare them.
      if (a.pid < b.pid) {
        return -1;
      }
      return a.pid === b.pid ? 0 : 1;
    });
  }

  [filterUnrelevantThreads]() {
    const filteredOrderedThreads = [];
    this.orderedThreads.forEach((thread) => {
      const threadEvents = this.orderedEvents.filter(orderedEvent =>
        thread.pid === orderedEvent.pid && thread.thread === orderedEvent.thread);

      if (threadEvents.findIndex(e => !['START', 'END'].includes(e.type)) < 0) {
        // No relevant events were found, so prevent thread and events to be drew.
        this.orderedEvents = this.orderedEvents.filter(e => !threadEvents.includes(e));
        console.log(`Discarding thread ${thread.pid}-${thread.thread}`);
      } else {
        // Save the thread.
        filteredOrderedThreads.push(thread);
      }
    });
    this.orderedThreads = filteredOrderedThreads;
  }

  getThreadOrderedIndex(name) {
    const [pid, thread] = name.split('||');
    console.log(`name: ${name}, pid: ${pid}, thread: ${thread}`);

    return this.orderedThreads.findIndex(t => t.pid === pid && t.thread === thread);
  }
}

