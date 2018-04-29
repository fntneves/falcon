import multiprocessing
import threading
import ctypes
import time
from falcon.core.events.types import EventData
from falcon.core.events.handling.base_handler import BaseHandler

class BpfEventHandler(BaseHandler):
    def __init__(self, stream):
        self._stream = stream
        self._events = []
        self._lock = threading.RLock()
        self._timer = None
        self._exit = False

    def boot(self):
        pass

    def handle(self, cpu, data, size):
        event = ctypes.cast(data, ctypes.POINTER(EventData)).contents

        self._lock.acquire()
        self._events.append((cpu, event, size))
        self._lock.release()

    def start_flusher(self):
        self._timer = threading.Thread(target=self._flush, name='flusher')
        self._timer.start()

    def stop_flusher(self):
        self._exit = True
        self._timer.join()

    def _flush(self):
        while not self._exit:
            time.sleep(5.0)

            self._lock.acquire()
            old_events = None
            if len(self._events) > 0:
                old_events = self._events
                self._events = []
            self._lock.release()

            if old_events is not None:
                self._stream.send(old_events)
                del old_events[:]

    def shutdown(self):
        pass
