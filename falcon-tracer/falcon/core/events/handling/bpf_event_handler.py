import multiprocessing
import ctypes
from falcon.core.events.types import EventData
from falcon.core.events.handling.base_handler import BaseHandler

class BpfEventHandler(BaseHandler):
    def __init__(self, output_stream):
        self._output_stream = output_stream

    def boot(self):
        pass

    def handle(self, cpu, data, size):
        event = ctypes.cast(data, ctypes.POINTER(EventData)).contents

        self._output_stream.put((cpu, event, size))

    def shutdown(self):
        pass
