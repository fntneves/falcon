import logging
import threading
import copy
import time
from falcon.core.events.event_factory import EventFactory, EventType
from falcon.core.events.types import SocketSend, SocketReceive, SocketConnect
from falcon.core.events.handling.base_handler import BaseHandler
from falcon.core.events.handling.event_dispatcher import EventDispatcher
from sortedcontainers import SortedSet

class SynchronizedEventQueue(object):
    def __init__(self, key):
        self.key = key
        self.queue = SortedSet(key=self.key)
        self.lock = threading.RLock()

    def add(self, item):
        with self.lock:
            self.queue.add(item)

    def __len__(self):
        with self.lock:
            return len(self.queue)

    def drain_all(self):
        with self.lock:
            queue_copy = self.queue
            self.queue = SortedSet(key=self.key)

        return queue_copy

class FalconEventLogger(BaseHandler):

    def __init__(self, writer):
        self._events = SynchronizedEventQueue(lambda event: event._ktime)
        self._flusher = threading.Thread(name='flusher', target=self._run_periodic_flush)
        self._writer = writer
        self._writes = 0
        self._events_counter = 0
        self._events_written = 0
        self._events_discarded = 0
        self._last_flushed_ktime = 0
        self._flush_task_running = False
        self._flush_lock = threading.RLock()
        super(FalconEventLogger, self).__init__()

    def boot(self):
        logging.info('Booting FalconEventLogger handler...')
        self._writer.open()

    def handle(self, cpu, data, size):
        event = EventFactory.create(data)

        if self._to_discard(event):
            return

        # Acquire lock to check last flushed ktime.
        with self._flush_lock:
            if event._ktime < self._last_flushed_ktime:
                logging.info("Discarding out of order event...")
                self._events_discarded = self._events_discarded + 1

        self._events.add(event)
        self._events_counter = self._events_counter + 1

        if not self._flush_task_running:
            self._flush_task_running = True
            self._flusher.start()

    def shutdown(self):
        logging.info('Shutting down FalconEventLogger handler...')
        logging.info('Waiting flusher to terminate...')
        self._flush_task_running = False
        self._flusher.join()

        logging.info('Flushing pending %s events...' % len(self._events))
        self._flush()

        self._writer.close()
        logging.info('Processed %d of %d events in total (%s discarded).' % (self._events_written, self._events_counter, self._events_discarded))

    def _to_discard(self, event):
        if (isinstance(event, SocketReceive) and event._sport in [9092, 53]) or (isinstance(event, SocketSend) and event._dport in [9092, 53]):
            return True

        if (isinstance(event, SocketConnect) and event._dport in [9092, 53]):
            return True

        return False

    def _flush(self):
        logging.debug('Flushing events...')
        with self._flush_lock:
            events = self._events.drain_all()
            if len(events) > 0:
                self._last_flushed_ktime = events[-1]._ktime

        for event in events:
            self._writer.write(event)
            self._events_written = self._events_written + 1
        logging.debug('Flushed %d events' % len(events))

    def _run_periodic_flush(self):
        while self._flush_task_running:
            self._flush()
            time.sleep(2)



