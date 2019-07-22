import logging
import uuid
from falcon.core.events.event_factory import EventFactory, EventType
from falcon.core.events.types import SocketSend, SocketReceive, SocketConnect
from falcon.core.events.handling.base_handler import BaseHandler
from falcon.core.events.handling.event_dispatcher import EventDispatcher

class FalconEventLogger(BaseHandler):
    def __init__(self, writer):
        self._holder = EventDispatcher()
        self._writer = writer
        self._writes = 0
        self._events_counter = 0
        super(FalconEventLogger, self).__init__()

    def boot(self):
        logging.info('Booting FalconEventLogger handler...')
        self._writer.open()

    def handle(self, cpu, data, size):
        event = EventFactory.create(data)

        if self._to_discard(event):
            return

        self._holder.put(cpu, event)

        self._events_counter = self._events_counter + 1

        for event in self._holder.retrieve_dispatch_candidates():
            self._writes = self._writes + 1
            self._writer.write(event.event)

    def shutdown(self):
        logging.info('Shutting down FalconEventLogger handler...')
        logging.info('Writing remaining %s events to file...' % len(self._holder.get_remaining()))

        for event in self._holder.get_remaining():
            self._writes = self._writes + 1
            self._writer.write(event.event)

        self._writer.close()
        logging.info('Logged %d of %d events in total.' % (self._writes, self._events_counter))

    def _to_discard(self, event):
        if (isinstance(event, SocketReceive) and event._sport in [9092, 53]) or (isinstance(event, SocketSend) and event._dport in [9092, 53]):
            return True

        if (isinstance(event, SocketConnect) and event._dport in [9092, 53]):
            return True

        return False
