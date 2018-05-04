import sys
import ctypes
import time
import socket
import multiprocessing
import signal
import logging
from falcon import util
from falcon.core.events.types.event import EventData

class BpfEventListener():
    def __init__(self, bpf, handler):
        self._bpf = bpf
        self._handler = handler

    def run(self):
        self._bpf.prepare()
        self._bpf.open_event_buffer('process_events', self.handle)
        self._bpf.open_event_buffer('socket_events', self.handle)

        # Give some time to open buffers.
        time.sleep(1)

        self._bpf.attach_probes()

        exit = [False]

        def start_shutdown(sigum, frame):
            logging.info('BPF event listener {} was interrupted...'.format(
                str(multiprocessing.current_process().pid)))
            exit[0] = True

        signal.signal(signal.SIGINT, start_shutdown)

        # Poll the kprobe events queue
        while not exit[0]:
            self._bpf.bpf_instance().kprobe_poll()

        self._bpf.detach_probes()

        logging.info('BPF event listener {} is exiting...'.format(
            str(multiprocessing.current_process().pid)))

    def handle(self, cpu, data, size):
        event = ctypes.cast(data, ctypes.POINTER(EventData)).contents

        self._handler.handle(cpu, event, size)

