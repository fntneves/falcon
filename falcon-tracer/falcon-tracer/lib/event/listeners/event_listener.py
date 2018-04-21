import sys
import ctypes
import time
import socket
from .. import EventData
from ... import util

class EventListener(): 
    def __init__(self, bpf, handlers): 
        self.bpf = bpf
        self._sleep_ms = 50
        self._exiting = False
        self._handlers = handlers
        self._hostname = socket.getfqdn()
        # Open the event buffer
        self.bpf["events"].open_perf_buffer(self.handle)

    def run(self): 
        while not self._exiting:
            try:
                if not self._exiting:
                    time.sleep(self._sleep_ms / 1000.0)
            except KeyboardInterrupt:
                self._exiting = True
                print >> sys.stderr, 'Exiting...'
            self.bpf.kprobe_poll()

    def handle(self, cpu, data, size):
        event = ctypes.cast(data, ctypes.POINTER(EventData)).contents

        # Add custom fields to each event
        event.host = self._hostname

        for handler in self._handlers:
            handler.handle(cpu, event)

