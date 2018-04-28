import multiprocessing
import ctypes
import socket
import signal
import json
from types.event import EventData
from falcon import util


class EventProcessor(multiprocessing.Process):
    def __init__(self, stream, handlers):
        self._stream = stream
        self._handlers = handlers
        self._hostname = None
        super(EventProcessor, self).__init__(name='event_processor')

    def run(self):
        signal.signal(signal.SIGINT, util.ignore_signal)
        self._hostname = socket.getfqdn()
        exit = False

        while not exit:
            try:
                data = self._stream.recv()

                if data == 'exit':
                    # Start shutdown
                    exit = True
                else:
                    for event in data:
                        (cpu, event_data, size) = event
                        event_data.host = self._hostname

                        for handler in self._handlers:
                            handler.handle(cpu, event_data, size)
            except EOFError:
                exit = True
                print 'Cannot read. Stream is closed...'

        print 'Event processor {} is exiting...'.format(
            str(multiprocessing.current_process().pid))
