import multiprocessing
import ctypes
from falcon.core.events.types import EventData
from falcon.core.events.handling.base_handler import BaseHandler

class BpfEventHandler(BaseHandler):
    def __init__(self, stream):
        self._stream = stream
        self._events = []

    def boot(self):
        pass

    def handle(self, cpu, data, size):
        event = ctypes.cast(data, ctypes.POINTER(EventData)).contents
        self._events.append((cpu, event, size))

        if len(self._events) > 10000:
            self._stream.send(self._events)
            del self._events[:]
            self._events = []

    def shutdown(self):
        self._stream.send(self._events)
