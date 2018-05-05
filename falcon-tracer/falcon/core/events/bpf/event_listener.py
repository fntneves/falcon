import sys
import ctypes
import time
import socket
import multiprocessing
import signal
from falcon import util

class BpfEventListener(multiprocessing.Process):
    def __init__(self, bpf, handler):
        self._bpf = bpf
        self._handler = handler

        super(BpfEventListener, self).__init__(name='bpf_listener')

    def run(self):
        self._bpf.prepare()
        self._bpf.open_event_buffer('process_events', self.handle)
        self._bpf.open_event_buffer('socket_events', self.handle)

        # Give some time to open buffers.
        time.sleep(1)

        self._handler.start_flusher()
        self._bpf.attach_probes()

        exit = [False]

        def start_shutdown(signum, frame):
            print 'BPF event listener {} is interrupted...'.format(str(multiprocessing.current_process().pid))
            exit[0] = True

        signal.signal(signal.SIGINT, start_shutdown)

        # Poll the kprobe events queue
        while not exit[0]:
            self._bpf.bpf_instance().kprobe_poll()

        self._bpf.detach_probes()
        self._handler.stop_flusher()

        print 'BPF event listener {} is exiting...'.format(
            str(multiprocessing.current_process().pid))

    def handle(self, cpu, data, size):
        self._handler.handle(cpu, data, size)
