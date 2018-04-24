import multiprocessing
from event_parser import EventParser
from logging_worker import LoggingWorker
from falcon.core.events.handling.base_handler import BaseHandler

class EventLogger(BaseHandler):
    def __init__(self):
        self._booted = False
        self._output_stream = None
        self._worker = None

    def boot(self):
        self._output_stream = multiprocessing.JoinableQueue()
        self._worker = LoggingWorker(self._output_stream)
        self._worker.daemon = True
        self._worker.start()
        self._booted = True

    def handle(self, cpu, data, size):
        assert self._booted is True

        event = EventParser.parse(cpu, data, size)
        if event is None:
            return

        if isinstance(event, list):
            [self._output_stream.put(e) for e in event]
        else:
            self._output_stream.put(event)

    def shutdown(self):
        if self._booted:
            assert self._output_stream is not None
            assert self._worker is not None
        else:
            return

        self._output_stream.join()
        self._output_stream.put('exit')
        self._worker.join()
