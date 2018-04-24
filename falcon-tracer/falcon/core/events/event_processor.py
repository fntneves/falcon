import multiprocessing
import ctypes
import socket
import signal
import json
from types.event import EventData
from falcon import util


class EventProcessor(multiprocessing.Process):
    def __init__(self, input_stream, handlers):
        self._input_stream = input_stream
        self._handlers = handlers
        self._hostname = None
        super(EventProcessor, self).__init__(name='event_processor')

    def run(self):
        signal.signal(signal.SIGINT, util.ignore_signal)
        self._hostname = socket.getfqdn()
        exit = False

        while not exit:
            # Prevent unexpected behavior caused by signals
            event = self._input_stream.get()

            if event == 'exit':
                # Start shutdown
                exit = True
            else:
                (cpu, event_data, size) = event
                event_data.host = self._hostname

                for handler in self._handlers:
                    handler.handle(cpu, event_data, size)

            self._input_stream.task_done()

        print 'Event processor {} is exiting...'.format(
            str(multiprocessing.current_process().pid))
