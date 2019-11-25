import logging
import threading
import copy
import uuid
from falcon.core.events.event_factory import EventFactory, EventType
from falcon.core.events.types import SocketSend, SocketReceive, SocketConnect
from falcon.core.events.handling.base_handler import BaseHandler
from falcon.core.events.handling.event_dispatcher import EventDispatcher
from timeloop import Timeloop
from datetime import timedelta
from sortedcontainers import SortedSet

tl = Timeloop()

class FalconEventLogger(BaseHandler):
    def __init__(self, writer):
        self._flush_running = False
        self._flush_lock = threading.RLock()
        ktime = lambda event: event._ktime
        self._events = SortedSet(key=ktime)
        self._last_flushed_ktime = 0
        self._writer = writer
        self._writes = 0
        self._events_counter = 0
        self._events_written = 0
        self._events_discarded = 0
        super(FalconEventLogger, self).__init__()

    def boot(self):
        logging.info('Booting FalconEventLogger handler...')
        self._writer.open()

    def handle(self, cpu, data, size):
        event = EventFactory.create(data)

        if self._to_discard(event):
            return

        if event._ktime < self._last_flushed_ktime:
            logging.ingo("Discarding out of order event...")
            self._events_discarded = self._events_discarded + 1

        with self._flush_lock:
            self._events.add(event)

        self._events_counter = self._events_counter + 1

        if not self._flush_running:
            tl.start()
            self._flush_running = True

    def shutdown(self):
        logging.info('Shutting down FalconEventLogger handler...')
        logging.info('Waiting for flusher to terminate...')
        tl.stop()

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

    @tl.job(interval=timedelta(seconds=3))
    def _flush(self):
        with self._flush_lock:
            events = copy.copy(self._events)
            if len(events) < 0:
                self._last_flushed_ktime = events[-1]._ktime

        for event in events:
            self._writer.write(event)
            self._events_written = self._events_written + 1

