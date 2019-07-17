import sys
import ctypes
import time
import socket
import multiprocessing
import signal
import logging
import os
from falcon import util
from falcon.core.events.base_event import EventData

class BpfEventListener():
    def __init__(self, bpf, handler, traced_pid, on_ready_callback=None):
        self._bpf = bpf
        self._handler = handler
        self._traced_pid = traced_pid
        self._on_ready_callback = on_ready_callback

    def run(self, on_ready_callback=None):
        self._bpf.prepare()
        self._bpf.open_event_buffer('events', self.handle)
        self._bpf.attach_probes()

        exit = [False]
        def start_shutdown(signum, frame):
            logging.info('BPF event listener {} was interrupted...'.format(
                str(multiprocessing.current_process().pid)))
            exit[0] = True

        signal.signal(signal.SIGINT, start_shutdown)

        # Execute the given OnReady callback
        if self._on_ready_callback is not None:
            self._on_ready_callback()

        # self._bpf.bpf_instance().trace_print()

        # Poll the kprobe events queue
        while not exit[0]:
            self._bpf.bpf_instance().kprobe_poll()

        self._bpf.detach_probes()

        logging.info('BPF event listener {} is exiting...'.format(
            str(multiprocessing.current_process().pid)))

    def handle(self, cpu, data, size):
        event = ctypes.cast(data, ctypes.POINTER(EventData)).contents

        self._handler.handle(cpu, event, size)
